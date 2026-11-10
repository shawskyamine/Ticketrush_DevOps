package com.ticketrush.controller;

import com.ticketrush.service.BookingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/seats")
public class BookingController {

    private final BookingService bookingService;
    private final String strategy;

    public BookingController(BookingService bookingService, 
                             @Value("${ticketrush.booking.strategy:atomic}") String strategy) {
        this.bookingService = bookingService;
        this.strategy = strategy;
    }

    @PostMapping("/{seatId}/book")
    public ResponseEntity<String> bookSeat(
            @PathVariable Long eventId,
            @PathVariable Long seatId,
            @RequestParam String buyer) {
        
        // ICI on appelle le service de l'extérieur ! Spring ouvre bien la transaction.
        if ("pessimistic".equals(strategy)) {
            bookingService.bookPessimistic(eventId, seatId, buyer);
        } else if ("optimistic".equals(strategy)) {
            bookingService.bookOptimistic(eventId, seatId, buyer);
        } else {
            bookingService.bookAtomic(eventId, seatId, buyer);
        }
        
        return ResponseEntity.ok("Seat booked successfully for " + buyer);
    }

     @PostMapping("/book-any")
    public ResponseEntity<String> bookAnySeat(
            @PathVariable Long eventId,
            @RequestParam String buyer) {
        
        Long seatId = bookingService.bookAny(eventId, buyer);
        return ResponseEntity.ok("Successfully booked seat ID: " + seatId + " for " + buyer);
    }
}