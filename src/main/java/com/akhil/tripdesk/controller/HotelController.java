package com.akhil.tripdesk.controller;

import com.akhil.tripdesk.dto.HotelDtos.*;
import com.akhil.tripdesk.dto.PageResponse;
import com.akhil.tripdesk.service.HotelService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Tag(name = "Hotels", description = "Search hotels and rooms; admins manage inventory")
public class HotelController {

    private final HotelService hotelService;

    @GetMapping
    public PageResponse<HotelResponse> search(@RequestParam(required = false) String city,
                                              @PageableDefault(size = 10, sort = "rating") Pageable pageable) {
        return PageResponse.from(hotelService.search(city, pageable));
    }

    @GetMapping("/{id}")
    public HotelResponse get(@PathVariable Long id) {
        return hotelService.get(id);
    }

    @GetMapping("/{id}/rooms")
    public List<RoomResponse> rooms(@PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return hotelService.rooms(id, checkIn, checkOut);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HotelResponse create(@Valid @RequestBody HotelRequest request) {
        return hotelService.create(request);
    }

    @PutMapping("/{id}")
    public HotelResponse update(@PathVariable Long id, @Valid @RequestBody HotelRequest request) {
        return hotelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        hotelService.delete(id);
    }

    @PostMapping("/{id}/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse addRoom(@PathVariable Long id, @Valid @RequestBody RoomRequest request) {
        return hotelService.addRoom(id, request);
    }
}
