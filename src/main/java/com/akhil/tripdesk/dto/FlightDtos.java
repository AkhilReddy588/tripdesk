package com.akhil.tripdesk.dto;

import com.akhil.tripdesk.entity.Flight;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public final class FlightDtos {
    private FlightDtos() {}

    public record FlightRequest(
            @NotBlank String flightNumber,
            @NotBlank String airline,
            @NotBlank String source,
            @NotBlank String destination,
            @NotNull @Future LocalDateTime departureTime,
            @NotNull @Future LocalDateTime arrivalTime,
            @NotNull @Positive BigDecimal price,
            @NotNull @Min(1) Integer totalSeats) {}

    public record FlightResponse(Long id, String flightNumber, String airline, String source, String destination,
                                 LocalDateTime departureTime, LocalDateTime arrivalTime, BigDecimal price,
                                 Integer availableSeats) {
        public static FlightResponse from(Flight f) {
            return new FlightResponse(f.getId(), f.getFlightNumber(), f.getAirline(), f.getSource(),
                    f.getDestination(), f.getDepartureTime(), f.getArrivalTime(), f.getPrice(),
                    f.getAvailableSeats());
        }
    }
}
