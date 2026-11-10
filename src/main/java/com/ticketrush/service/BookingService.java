package com.ticketrush.service;

import com.ticketrush.entity.Event;
import com.ticketrush.entity.Seat;
import com.ticketrush.exception.SeatTakenException;
import com.ticketrush.repository.EventRepository;
import com.ticketrush.repository.SeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    private final SeatRepository seatRepository;
    private final EventRepository eventRepository; // Ajouté

    public BookingService(SeatRepository seatRepository, EventRepository eventRepository) {
        this.seatRepository = seatRepository;
        this.eventRepository = eventRepository;
    }

    // --- NOUVELLE FONCTIONNALITÉ : CRÉER UN ÉVÉNEMENT ---
    @Transactional
    public Event createEvent(String name, int totalSeats) {
        Event event = new Event();
        event.setName(name);
        event.setTotalSeats(totalSeats);
        
        // 1. Sauvegarder l'événement
        Event savedEvent = eventRepository.save(event);
        
        // 2. Générer automatiquement les sièges
        List<Seat> seats = new ArrayList<>();
        for (int i = 1; i <= totalSeats; i++) {
            Seat seat = new Seat();
            seat.setEvent(savedEvent);
            seat.setLabel("Seat-" + i);
            seat.setStatus("AVAILABLE");
            seats.add(seat);
        }
        seatRepository.saveAll(seats);
        
        return savedEvent;
    }

    // --- FONCTIONNALITÉS EXISTANTES ---
    
    // 1. ATOMIC
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

    // 4. BOOK ANY
    @Transactional
    public Long bookAny(Long eventId, String buyer) {
        Long bookedSeatId = seatRepository.bookAnySeatAtomically(eventId, buyer);
        if (bookedSeatId == null) {
            throw new SeatTakenException("No available seats left for this event");
        }
        return bookedSeatId;
    }
}