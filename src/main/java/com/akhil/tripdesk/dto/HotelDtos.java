package com.akhil.tripdesk.dto;

import com.akhil.tripdesk.entity.Hotel;
import com.akhil.tripdesk.entity.Room;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public final class HotelDtos {
    private HotelDtos() {}

    public record HotelRequest(
            @NotBlank String name,
            @NotBlank String city,
            String address,
            @DecimalMin("0.0") @DecimalMax("5.0") Double rating) {}

    public record HotelResponse(Long id, String name, String city, String address, Double rating) {
        public static HotelResponse from(Hotel h) {
            return new HotelResponse(h.getId(), h.getName(), h.getCity(), h.getAddress(), h.getRating());
        }
    }

    public record RoomRequest(
            @NotBlank String roomNumber,
            @NotBlank String type,
            @NotNull @Positive BigDecimal pricePerNight) {}

    public record RoomResponse(Long id, Long hotelId, String roomNumber, String type, BigDecimal pricePerNight) {
        public static RoomResponse from(Room r) {
            return new RoomResponse(r.getId(), r.getHotel().getId(), r.getRoomNumber(), r.getType(),
                    r.getPricePerNight());
        }
    }
}
