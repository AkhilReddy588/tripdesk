package com.akhil.tripdesk.controller;

import com.akhil.tripdesk.dto.FlightDtos.FlightRequest;
import com.akhil.tripdesk.dto.FlightDtos.FlightResponse;
import com.akhil.tripdesk.dto.PageResponse;
import com.akhil.tripdesk.service.FlightService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
@Tag(name = "Flights", description = "Search flights; admins add flights")
public class FlightController {

    private final FlightService flightService;

    @GetMapping
    public PageResponse<FlightResponse> search(
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @PageableDefault(size = 10, sort = "price") Pageable pageable) {
        return PageResponse.from(flightService.search(source, destination, date, pageable));
    }

    @GetMapping("/{id}")
    public FlightResponse get(@PathVariable Long id) {
        return flightService.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FlightResponse create(@Valid @RequestBody FlightRequest request) {
        return flightService.create(request);
    }
}
