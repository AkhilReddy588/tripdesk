package com.akhil.tripdesk.controller;

import com.akhil.tripdesk.dto.BookingDtos.*;
import com.akhil.tripdesk.dto.PageResponse;
import com.akhil.tripdesk.service.BookingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Book rooms and flights, view and cancel your bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/hotel")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse bookRoom(@AuthenticationPrincipal UserDetails user,
                                    @Valid @RequestBody HotelBookingRequest request) {
        return bookingService.bookRoom(user.getUsername(), request);
    }

    @PostMapping("/flight")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse bookFlight(@AuthenticationPrincipal UserDetails user,
                                      @Valid @RequestBody FlightBookingRequest request) {
        return bookingService.bookFlight(user.getUsername(), request);
    }

    @GetMapping("/me")
    public PageResponse<BookingResponse> myBookings(@AuthenticationPrincipal UserDetails user,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.from(bookingService.myBookings(user.getUsername(), pageable));
    }

    @GetMapping("/{id}")
    public BookingResponse get(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return bookingService.get(user.getUsername(), id);
    }

    @PatchMapping("/{id}/cancel")
    public BookingResponse cancel(@AuthenticationPrincipal UserDetails user, @PathVariable Long id) {
        return bookingService.cancel(user.getUsername(), id);
    }
}
