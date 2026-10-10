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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final FlightRepository flightRepository;
    private final UserRepository userRepository;

    /**
     * Books a hotel room. The room row is locked (PESSIMISTIC_WRITE) for the duration of the
     * transaction, so two concurrent requests for overlapping dates cannot both succeed.
     */
    @Transactional
    public BookingResponse bookRoom(String email, HotelBookingRequest req) {
        if (!req.checkOut().isAfter(req.checkIn())) {
            throw new BadRequestException("checkOut must be after checkIn");
        }
        User user = user(email);
        Room room = roomRepository.findByIdForUpdate(req.roomId())
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id " + req.roomId()));

        if (bookingRepository.existsOverlappingBooking(room.getId(), req.checkIn(), req.checkOut())) {
            throw new BookingConflictException("Room is already booked for the selected dates");
        }

        long nights = ChronoUnit.DAYS.between(req.checkIn(), req.checkOut());
        Booking booking = Booking.builder()
                .user(user).type(BookingType.HOTEL).room(room)
                .checkIn(req.checkIn()).checkOut(req.checkOut())
                .totalPrice(room.getPricePerNight().multiply(BigDecimal.valueOf(nights)))
                .status(BookingStatus.CONFIRMED)
                .build();
        return BookingResponse.from(bookingRepository.save(booking));
    }

    /** Books seats on a flight, locking the flight row so seats are never oversold. */
    @Transactional
    public BookingResponse bookFlight(String email, FlightBookingRequest req) {
        User user = user(email);
        Flight flight = flightRepository.findByIdForUpdate(req.flightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id " + req.flightId()));

        if (flight.getAvailableSeats() < req.seats()) {
            throw new BookingConflictException("Only " + flight.getAvailableSeats() + " seats left on this flight");
        }
        flight.setAvailableSeats(flight.getAvailableSeats() - req.seats());

        Booking booking = Booking.builder()
                .user(user).type(BookingType.FLIGHT).flight(flight).seats(req.seats())
                .totalPrice(flight.getPrice().multiply(BigDecimal.valueOf(req.seats())))
                .status(BookingStatus.CONFIRMED)
                .build();
        return BookingResponse.from(bookingRepository.save(booking));
    }

    @Transactional(readOnly = true)
    public Page<BookingResponse> myBookings(String email, Pageable pageable) {
        return bookingRepository.findByUserId(user(email).getId(), pageable).map(BookingResponse::from);
    }

    @Transactional(readOnly = true)
    public BookingResponse get(String email, Long id) {
        Booking booking = findOwned(email, id);
        return BookingResponse.from(booking);
    }

    /** Cancels a booking; flight seats are released back to inventory. */
    @Transactional
    public BookingResponse cancel(String email, Long id) {
        Booking booking = findOwned(email, id);
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }
        if (booking.getType() == BookingType.FLIGHT) {
            Flight flight = flightRepository.findByIdForUpdate(booking.getFlight().getId()).orElseThrow();
            flight.setAvailableSeats(flight.getAvailableSeats() + booking.getSeats());
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return BookingResponse.from(booking);
    }

    private Booking findOwned(String email, Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id " + id));
        User user = user(email);
        if (!booking.getUser().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Not your booking");
        }
        return booking;
    }

    private User user(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
