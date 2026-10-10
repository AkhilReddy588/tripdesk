package com.akhil.tripdesk.config;

import com.akhil.tripdesk.entity.*;
import com.akhil.tripdesk.repository.FlightRepository;
import com.akhil.tripdesk.repository.HotelRepository;
import com.akhil.tripdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Seeds an admin account and sample hotels/flights so the API can be tried right away. */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final HotelRepository hotelRepository;
    private final FlightRepository flightRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) return;

        userRepository.save(User.builder().name("Admin").email("admin@tripdesk.com")
                .password(passwordEncoder.encode("admin123")).role(Role.ADMIN).build());

        Hotel taj = Hotel.builder().name("Taj Krishna").city("Hyderabad")
                .address("Road No. 1, Banjara Hills").rating(4.7).build();
        taj.getRooms().add(room(taj, "101", "DELUXE", "8500"));
        taj.getRooms().add(room(taj, "102", "DELUXE", "8500"));
        taj.getRooms().add(room(taj, "201", "SUITE", "15000"));

        Hotel leela = Hotel.builder().name("The Leela Palace").city("Bengaluru")
                .address("Old Airport Road").rating(4.8).build();
        leela.getRooms().add(room(leela, "301", "PREMIER", "12000"));

        hotelRepository.save(taj);
        hotelRepository.save(leela);

        LocalDate d = LocalDate.now().plusDays(7);
        flightRepository.save(Flight.builder().flightNumber("6E-5321").airline("IndiGo")
                .source("HYD").destination("BLR")
                .departureTime(d.atTime(LocalTime.of(7, 30))).arrivalTime(d.atTime(LocalTime.of(8, 45)))
                .price(new BigDecimal("4200")).totalSeats(180).availableSeats(180).build());
        flightRepository.save(Flight.builder().flightNumber("AI-542").airline("Air India")
                .source("HYD").destination("DEL")
                .departureTime(d.atTime(LocalTime.of(18, 0))).arrivalTime(d.atTime(LocalTime.of(20, 15)))
                .price(new BigDecimal("6800")).totalSeats(150).availableSeats(150).build());
    }

    private Room room(Hotel hotel, String number, String type, String price) {
        return Room.builder().hotel(hotel).roomNumber(number).type(type)
                .pricePerNight(new BigDecimal(price)).build();
    }
}
