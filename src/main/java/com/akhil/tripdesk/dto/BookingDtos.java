package com.akhil.tripdesk.dto;

import com.akhil.tripdesk.entity.Booking;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class BookingDtos {
    private BookingDtos() {}

    public record HotelBookingRequest(
            @NotNull Long roomId,
            @NotNull @FutureOrPresent LocalDate checkIn,
            @NotNull @Future LocalDate checkOut) {}

    public record FlightBookingRequest(
            @NotNull Long flightId,
            @NotNull @Min(1) @Max(9) Integer seats) {}

    public record BookingResponse(Long id, String type, String status, Long roomId, Long flightId,
                                  LocalDate checkIn, LocalDate checkOut, Integer seats,
                                  BigDecimal totalPrice, LocalDateTime createdAt) {
        public static BookingResponse from(Booking b) {
            return new BookingResponse(b.getId(), b.getType().name(), b.getStatus().name(),
                    b.getRoom() != null ? b.getRoom().getId() : null,
                    b.getFlight() != null ? b.getFlight().getId() : null,
                    b.getCheckIn(), b.getCheckOut(), b.getSeats(), b.getTotalPrice(), b.getCreatedAt());
        }
    }
}
