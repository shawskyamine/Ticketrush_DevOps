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
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // Variables pour stocker les IDs réels générés par PostgreSQL
    private Long eventId;
    private Long seatId;

    @BeforeEach
    void setUp() {
        // 1. Nettoyage
        jdbcTemplate.execute("DELETE FROM seat");
        jdbcTemplate.execute("DELETE FROM event");

        // 2. Créer l'événement (sans forcer l'ID)
        jdbcTemplate.execute("INSERT INTO event (name, total_seats) VALUES ('Concert A', 2)");
        
        // 3. Récupérer l'ID généré pour l'événement
        eventId = jdbcTemplate.queryForObject("SELECT id FROM event LIMIT 1", Long.class);

        // 4. Créer le siège (sans forcer l'ID)
        jdbcTemplate.execute("INSERT INTO seat (event_id, label, status) VALUES (" + eventId + ", 'A1', 'AVAILABLE')");
        
        // 5. Récupérer l'ID généré pour le siège
        seatId = jdbcTemplate.queryForObject("SELECT id FROM seat LIMIT 1", Long.class);
    }

    @Test
    void testBookNonExistentSeat() {
        assertThrows(SeatTakenException.class, () -> {
            // Utilise l'eventId réel, mais un seatId qui n'existe pas (999)
            bookingService.bookPessimistic(eventId, 999L, "Alice");
        });
    }

    @Test
    void testBookNonExistentEvent() {
        assertThrows(SeatTakenException.class, () -> {
            // Utilise un eventId qui n'existe pas (999)
            bookingService.bookAny(999L, "Alice");
        });
    }

    @Test
    void testBookAlreadySoldSeatOptimistic() {
        // 1er achat avec les vrais IDs
        bookingService.bookOptimistic(eventId, seatId, "Alice");
        
        // 2ème achat avec les mêmes vrais IDs -> doit échouer
        assertThrows(SeatTakenException.class, () -> {
            bookingService.bookOptimistic(eventId, seatId, "Bob");
        });
    }

    @Test
    void testBookAlreadySoldSeatPessimistic() {
        // 1er achat avec les vrais IDs
        bookingService.bookPessimistic(eventId, seatId, "Alice");
        
        // 2ème achat avec les mêmes vrais IDs -> doit échouer
        assertThrows(SeatTakenException.class, () -> {
            bookingService.bookPessimistic(eventId, seatId, "Bob");
        });
    }

    @Test
    void testCreateEvent() {
        // Créer un événement avec 10 sièges
        var event = bookingService.createEvent("Test Concert", 10);
        
        // Vérifications
        assertNotNull(event.getId());
        assertEquals("Test Concert", event.getName());
        assertEquals(10, event.getTotalSeats());

        // Vérifier que les 10 sièges ont bien été générés dans la base
        Integer seatCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM seat WHERE event_id = ? AND status = 'AVAILABLE'", 
            Integer.class, 
            event.getId()
        );
        
        assertNotNull(seatCount);
        assertEquals(10, seatCount);
    }
}