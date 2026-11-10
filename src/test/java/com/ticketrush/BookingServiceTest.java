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

@SpringBootTest
class BookingServiceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DELETE FROM seat");
        jdbcTemplate.execute("DELETE FROM event");
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
}