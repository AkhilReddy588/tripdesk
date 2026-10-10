package com.akhil.tripdesk.service;

import com.akhil.tripdesk.dto.BookingDtos.BookingResponse;
import com.akhil.tripdesk.dto.BookingDtos.FlightBookingRequest;
import com.akhil.tripdesk.dto.BookingDtos.HotelBookingRequest;
import com.akhil.tripdesk.entity.*;
import com.akhil.tripdesk.exception.BadRequestException;
import com.akhil.tripdesk.exception.BookingConflictException;
import com.akhil.tripdesk.exception.ResourceNotFoundException;
import com.akhil.tripdesk.repository.BookingRepository;
import com.akhil.tripdesk.repository.FlightRepository;
import com.akhil.tripdesk.repository.RoomRepository;
import com.akhil.tripdesk.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock BookingRepository bookingRepository;
    @Mock RoomRepository roomRepository;
    @Mock FlightRepository flightRepository;
    @Mock UserRepository userRepository;

    @InjectMocks BookingService bookingService;

    private User user;
    private Room room;
    private Flight flight;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Akhil").email("akhil@test.com").role(Role.USER).password("x").build();
        Hotel hotel = Hotel.builder().id(10L).name("Lakeview").city("Hyderabad").build();
        room = Room.builder().id(100L).hotel(hotel).roomNumber("101").type("DELUXE")
                .pricePerNight(new BigDecimal("3500")).build();
        flight = Flight.builder().id(200L).flightNumber("TD101").price(new BigDecimal("5000"))
                .totalSeats(10).availableSeats(3).build();
    }

    @Test
    void bookRoom_calculatesPriceForNights() {
        LocalDate in = LocalDate.now().plusDays(1);
        when(userRepository.findByEmail("akhil@test.com")).thenReturn(Optional.of(user));
        when(roomRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlappingBooking(100L, in, in.plusDays(3))).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse res = bookingService.bookRoom("akhil@test.com",
                new HotelBookingRequest(100L, in, in.plusDays(3)));

        assertThat(res.totalPrice()).isEqualByComparingTo("10500");
        assertThat(res.status()).isEqualTo("CONFIRMED");
        assertThat(res.type()).isEqualTo("HOTEL");
    }

    @Test
    void bookRoom_rejectsOverlappingDates() {
        LocalDate in = LocalDate.now().plusDays(1);
        when(userRepository.findByEmail("akhil@test.com")).thenReturn(Optional.of(user));
        when(roomRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(room));
        when(bookingRepository.existsOverlappingBooking(100L, in, in.plusDays(2))).thenReturn(true);

        assertThatThrownBy(() -> bookingService.bookRoom("akhil@test.com",
                new HotelBookingRequest(100L, in, in.plusDays(2))))
                .isInstanceOf(BookingConflictException.class);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void bookRoom_rejectsCheckOutBeforeCheckIn() {
        LocalDate in = LocalDate.now().plusDays(5);
        assertThatThrownBy(() -> bookingService.bookRoom("akhil@test.com",
                new HotelBookingRequest(100L, in, in.minusDays(1))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void bookRoom_unknownRoom_throwsNotFound() {
        LocalDate in = LocalDate.now().plusDays(1);
        when(userRepository.findByEmail("akhil@test.com")).thenReturn(Optional.of(user));
        when(roomRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookingService.bookRoom("akhil@test.com",
                new HotelBookingRequest(999L, in, in.plusDays(1))))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void bookFlight_decrementsSeats() {
        when(userRepository.findByEmail("akhil@test.com")).thenReturn(Optional.of(user));
        when(flightRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(flight));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse res = bookingService.bookFlight("akhil@test.com", new FlightBookingRequest(200L, 2));

        assertThat(flight.getAvailableSeats()).isEqualTo(1);
        assertThat(res.totalPrice()).isEqualByComparingTo("10000");
    }

    @Test
    void bookFlight_failsWhenNotEnoughSeats() {
        when(userRepository.findByEmail("akhil@test.com")).thenReturn(Optional.of(user));
        when(flightRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(flight));

        assertThatThrownBy(() -> bookingService.bookFlight("akhil@test.com", new FlightBookingRequest(200L, 4)))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("3 seats left");
        assertThat(flight.getAvailableSeats()).isEqualTo(3);
    }

    @Test
    void cancelFlightBooking_releasesSeats() {
        Booking booking = Booking.builder().id(5L).user(user).type(BookingType.FLIGHT).flight(flight)
                .seats(2).totalPrice(new BigDecimal("10000")).status(BookingStatus.CONFIRMED).build();
        when(bookingRepository.findById(5L)).thenReturn(Optional.of(booking));
        when(userRepository.findByEmail("akhil@test.com")).thenReturn(Optional.of(user));
        when(flightRepository.findByIdForUpdate(200L)).thenReturn(Optional.of(flight));

        BookingResponse res = bookingService.cancel("akhil@test.com", 5L);

        assertThat(res.status()).isEqualTo("CANCELLED");
        assertThat(flight.getAvailableSeats()).isEqualTo(5);
    }
}
