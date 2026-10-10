package com.akhil.tripdesk.service;

import com.akhil.tripdesk.dto.HotelDtos.*;
import com.akhil.tripdesk.entity.Hotel;
import com.akhil.tripdesk.entity.Room;
import com.akhil.tripdesk.exception.BadRequestException;
import com.akhil.tripdesk.exception.ResourceNotFoundException;
import com.akhil.tripdesk.repository.HotelRepository;
import com.akhil.tripdesk.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Transactional(readOnly = true)
    public Page<HotelResponse> search(String city, Pageable pageable) {
        Page<Hotel> page = (city == null || city.isBlank())
                ? hotelRepository.findAll(pageable)
                : hotelRepository.findByCityIgnoreCase(city.trim(), pageable);
        return page.map(HotelResponse::from);
    }

    @Transactional(readOnly = true)
    public HotelResponse get(Long id) {
        return HotelResponse.from(find(id));
    }

    @Transactional
    public HotelResponse create(HotelRequest req) {
        Hotel hotel = Hotel.builder().name(req.name()).city(req.city())
                .address(req.address()).rating(req.rating()).build();
        return HotelResponse.from(hotelRepository.save(hotel));
    }

    @Transactional
    public HotelResponse update(Long id, HotelRequest req) {
        Hotel hotel = find(id);
        hotel.setName(req.name());
        hotel.setCity(req.city());
        hotel.setAddress(req.address());
        hotel.setRating(req.rating());
        return HotelResponse.from(hotel);
    }

    @Transactional
    public void delete(Long id) {
        hotelRepository.delete(find(id));
    }

    @Transactional
    public RoomResponse addRoom(Long hotelId, RoomRequest req) {
        Hotel hotel = find(hotelId);
        Room room = Room.builder().hotel(hotel).roomNumber(req.roomNumber())
                .type(req.type()).pricePerNight(req.pricePerNight()).build();
        return RoomResponse.from(roomRepository.save(room));
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> rooms(Long hotelId, LocalDate checkIn, LocalDate checkOut) {
        find(hotelId);
        if (checkIn == null || checkOut == null) {
            return roomRepository.findByHotelId(hotelId).stream().map(RoomResponse::from).toList();
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new BadRequestException("checkOut must be after checkIn");
        }
        return roomRepository.findAvailableRooms(hotelId, checkIn, checkOut)
                .stream().map(RoomResponse::from).toList();
    }

    private Hotel find(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found with id " + id));
    }
}
