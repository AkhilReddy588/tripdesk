package com.akhil.tripdesk;

import com.akhil.tripdesk.entity.Role;
import com.akhil.tripdesk.entity.User;
import com.akhil.tripdesk.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BookingFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void endToEndHotelBookingFlow() throws Exception {
        userRepository.save(User.builder().name("Admin").email("admin@it.com")
                .password(passwordEncoder.encode("admin123")).role(Role.ADMIN).build());
        String adminToken = login("admin@it.com", "admin123");

        // admin creates a hotel and a room
        JsonNode hotel = json(mvc.perform(post("/api/hotels").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Inn\",\"city\":\"Hyderabad\",\"rating\":4.2}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long hotelId = hotel.get("id").asLong();

        JsonNode room = json(mvc.perform(post("/api/hotels/" + hotelId + "/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roomNumber\":\"101\",\"type\":\"DELUXE\",\"pricePerNight\":3000}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long roomId = room.get("id").asLong();

        // a normal user registers
        JsonNode reg = json(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Akhil\",\"email\":\"akhil@it.com\",\"password\":\"secret12\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        String userToken = reg.get("token").asText();

        // a normal user cannot create hotels
        mvc.perform(post("/api/hotels").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"city\":\"Y\"}"))
                .andExpect(status().isForbidden());

        // public search works without a token
        mvc.perform(get("/api/hotels").param("city", "hyderabad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        LocalDate in = LocalDate.now().plusDays(10);
        String body = "{\"roomId\":" + roomId + ",\"checkIn\":\"" + in + "\",\"checkOut\":\"" + in.plusDays(2) + "\"}";

        // booking without a token is rejected
        mvc.perform(post("/api/bookings/hotel").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());

        // first booking succeeds
        mvc.perform(post("/api/bookings/hotel").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPrice").value(6000))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // overlapping booking is blocked
        String overlap = "{\"roomId\":" + roomId + ",\"checkIn\":\"" + in.plusDays(1) + "\",\"checkOut\":\""
                + in.plusDays(3) + "\"}";
        mvc.perform(post("/api/bookings/hotel").header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON).content(overlap))
                .andExpect(status().isConflict());

        // room no longer shows as available for those dates
        mvc.perform(get("/api/hotels/" + hotelId + "/rooms")
                        .param("checkIn", in.toString()).param("checkOut", in.plusDays(2).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mvc.perform(get("/api/bookings/me").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void validationErrorsAreReturnedPerField() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    private String login(String email, String password) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return json(res).get("token").asText();
    }

    private JsonNode json(String s) throws Exception {
        return mapper.readTree(s);
    }
}
