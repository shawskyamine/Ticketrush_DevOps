package com.ticketrush.repository;

import com.ticketrush.entity.Seat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    // 1. ATOMIC
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        UPDATE Seat s 
        SET s.status = 'SOLD', s.buyer = :buyer 
        WHERE s.id = :seatId 
          AND s.event.id = :eventId 
          AND s.status = 'AVAILABLE'
    """)
    int bookSeatAtomically(
        @Param("seatId") Long seatId, 
        @Param("eventId") Long eventId, 
        @Param("buyer") String buyer
    );

    // 2. PESSIMISTIC
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id = :seatId AND s.event.id = :eventId")
    Optional<Seat> findByIdWithPessimisticLock(@Param("seatId") Long seatId, @Param("eventId") Long eventId);


        // 4. BOOK ANY SEAT (SKIP LOCKED)
    @Query(value = """
        UPDATE seat 
        SET status = 'SOLD', buyer = :buyer 
        WHERE id = (
            SELECT id FROM seat 
            WHERE event_id = :eventId AND status = 'AVAILABLE' 
            ORDER BY id LIMIT 1 
            FOR UPDATE SKIP LOCKED
        ) 
        RETURNING id
    """, nativeQuery = true)
    Long bookAnySeatAtomically(@Param("eventId") Long eventId, @Param("buyer") String buyer);
}