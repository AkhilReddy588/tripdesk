package com.akhil.tripdesk.service;

import com.akhil.tripdesk.dto.FlightDtos.FlightRequest;
import com.akhil.tripdesk.dto.FlightDtos.FlightResponse;
import com.akhil.tripdesk.entity.Flight;
import com.akhil.tripdesk.exception.BadRequestException;
import com.akhil.tripdesk.exception.BookingConflictException;
import com.akhil.tripdesk.exception.ResourceNotFoundException;
import com.akhil.tripdesk.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;

    @Transactional(readOnly = true)
    public Page<FlightResponse> search(String source, String destination, LocalDate date, Pageable pageable) {
        if (source == null || destination == null || date == null) {
            return flightRepository.findAll(pageable).map(FlightResponse::from);
        }
        return flightRepository
                .findBySourceIgnoreCaseAndDestinationIgnoreCaseAndDepartureTimeBetween(
                        source.trim(), destination.trim(), date.atStartOfDay(),
                        date.plusDays(1).atStartOfDay().minusNanos(1), pageable)
                .map(FlightResponse::from);
    }

    @Transactional(readOnly = true)
    public FlightResponse get(Long id) {
        return FlightResponse.from(flightRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id " + id)));
    }

    @Transactional
    public FlightResponse create(FlightRequest req) {
        if (!req.arrivalTime().isAfter(req.departureTime())) {
            throw new BadRequestException("arrivalTime must be after departureTime");
        }
        if (flightRepository.existsByFlightNumber(req.flightNumber())) {
            throw new BookingConflictException("Flight number already exists: " + req.flightNumber());
        }
        Flight flight = Flight.builder()
                .flightNumber(req.flightNumber()).airline(req.airline())
                .source(req.source()).destination(req.destination())
                .departureTime(req.departureTime()).arrivalTime(req.arrivalTime())
                .price(req.price()).totalSeats(req.totalSeats()).availableSeats(req.totalSeats())
                .build();
        return FlightResponse.from(flightRepository.save(flight));
    }
}
