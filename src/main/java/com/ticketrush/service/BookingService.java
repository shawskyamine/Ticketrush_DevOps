package com.ticketrush.service;

import com.ticketrush.entity.Seat;
import com.ticketrush.exception.SeatTakenException;
import com.ticketrush.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final SeatRepository seatRepository;

    public BookingService(SeatRepository seatRepository) {
        this.seatRepository = seatRepository;
    }

    // 1. ATOMIC (Pas besoin de @Transactional ici car le @Modifying du repo le gère)
    public void bookAtomic(Long eventId, Long seatId, String buyer) {
        int updatedRows = seatRepository.bookSeatAtomically(seatId, eventId, buyer);
        if (updatedRows == 0) {
            throw new SeatTakenException("Seat is already sold or does not exist");
        }
    }

    // 2. PESSIMISTIC
    @Transactional
    public void bookPessimistic(Long eventId, Long seatId, String buyer) {
        Seat seat = seatRepository.findByIdWithPessimisticLock(seatId, eventId)
                .orElseThrow(() -> new SeatTakenException("Seat not found"));

        if ("SOLD".equals(seat.getStatus())) {
            throw new SeatTakenException("Seat is already sold");
        }

        seat.setStatus("SOLD");
        seat.setBuyer(buyer);
        seatRepository.save(seat);
    }

    // 3. OPTIMISTIC
    @Transactional
    public void bookOptimistic(Long eventId, Long seatId, String buyer) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new SeatTakenException("Seat not found"));

        if ("SOLD".equals(seat.getStatus())) {
            throw new SeatTakenException("Seat is already sold");
        }

        seat.setStatus("SOLD");
        seat.setBuyer(buyer);
        
        try {
            seatRepository.saveAndFlush(seat);
        } catch (org.springframework.dao.OptimisticLockingFailureException e) {
            throw new SeatTakenException("Seat is already sold (optimistic lock failed)");
        }
    }

        @Transactional
    public Long bookAny(Long eventId, String buyer) {
        Long bookedSeatId = seatRepository.bookAnySeatAtomically(eventId, buyer);
        if (bookedSeatId == null) {
            throw new SeatTakenException("No available seats left for this event");
        }
        return bookedSeatId;
    }
}