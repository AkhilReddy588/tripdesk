package com.akhil.tripdesk.repository;

import com.akhil.tripdesk.entity.Flight;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Flight f where f.id = :id")
    Optional<Flight> findByIdForUpdate(@Param("id") Long id);

    boolean existsByFlightNumber(String flightNumber);

    Page<Flight> findBySourceIgnoreCaseAndDestinationIgnoreCaseAndDepartureTimeBetween(
            String source, String destination, LocalDateTime from, LocalDateTime to, Pageable pageable);
}
