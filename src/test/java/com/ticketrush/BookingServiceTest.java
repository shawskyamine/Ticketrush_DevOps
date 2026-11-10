package com.ticketrush;

import com.ticketrush.exception.SeatTakenException;
import com.ticketrush.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Nettoie la base avant chaque test
        jdbcTemplate.execute("DELETE FROM seat");
        jdbcTemplate.execute("DELETE FROM event");
        
        // Insère un événement et un siège pour les tests de réservation
        jdbcTemplate.execute("INSERT INTO event (id, name, total_seats) VALUES (1, 'Concert A', 2)");
        jdbcTemplate.execute("INSERT INTO seat (id, event_id, label, status) VALUES (1, 1, 'A1', 'AVAILABLE')");
    }

    @Test
    void testBookNonExistentSeat() {
        // Cover the "Seat not found" branch of bookPessimistic
        assertThrows(SeatTakenException.class, () -> {
            bookingService.bookPessimistic(1L, 999L, "Alice");
        });
    }

    @Test
    void testBookNonExistentEvent() {
        // Cover the "Seat not found" branch of bookAtomic / bookAny
        assertThrows(SeatTakenException.class, () -> {
            bookingService.bookAny(999L, "Alice");
        });
    }

    @Test
    void testBookAlreadySoldSeatOptimistic() {
        // Cover the "SOLD" check in bookOptimistic
        bookingService.bookOptimistic(1L, 1L, "Alice");
        
        assertThrows(SeatTakenException.class, () -> {
            bookingService.bookOptimistic(1L, 1L, "Bob");
        });
    }

    @Test
    void testBookAlreadySoldSeatPessimistic() {
        // Cover the "SOLD" check in bookPessimistic
        bookingService.bookPessimistic(1L, 1L, "Alice");
        
        assertThrows(SeatTakenException.class, () -> {
            bookingService.bookPessimistic(1L, 1L, "Bob");
        });
    }

    @Test
    void testCreateEvent() {
        // 1. Créer un événement avec 10 sièges
        var event = bookingService.createEvent("Test Concert", 10);
        
        // 2. Vérifier que l'événement est créé avec un ID
        assertNotNull(event.getId());
        assertEquals("Test Concert", event.getName());
        assertEquals(10, event.getTotalSeats());

        // 3. Vérifier que les 10 sièges ont bien été générés dans la base
        Integer seatCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM seat WHERE event_id = ? AND status = 'AVAILABLE'", 
            Integer.class, 
            event.getId()
        );
        
        assertNotNull(seatCount);
        assertEquals(10, seatCount);
    }
}