package com.akhil.tripdesk.repository;

import com.akhil.tripdesk.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    @Query("""
           select count(b) > 0 from Booking b
           where b.room.id = :roomId
             and b.status = com.akhil.tripdesk.entity.BookingStatus.CONFIRMED
             and b.checkIn < :checkOut
             and b.checkOut > :checkIn
           """)
    boolean existsOverlappingBooking(@Param("roomId") Long roomId,
                                     @Param("checkIn") LocalDate checkIn,
                                     @Param("checkOut") LocalDate checkOut);
}
