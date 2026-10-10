package com.akhil.tripdesk.repository;

import com.akhil.tripdesk.entity.Hotel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
    Page<Hotel> findByCityIgnoreCase(String city, Pageable pageable);
}
