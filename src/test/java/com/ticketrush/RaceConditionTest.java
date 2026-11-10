package com.ticketrush;

import com.ticketrush.exception.SeatTakenException;
import com.ticketrush.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RaceConditionTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data (in case tests run multiple times)
        jdbcTemplate.execute("DELETE FROM seat");
        jdbcTemplate.execute("DELETE FROM event");
        
        // Create event 1 and seat 2 (A2)
        jdbcTemplate.execute("INSERT INTO event (id, name, total_seats) VALUES (1, 'Concert A', 2)");
        jdbcTemplate.execute("INSERT INTO seat (id, event_id, label, status) VALUES (1, 1, 'A1', 'AVAILABLE')");
        jdbcTemplate.execute("INSERT INTO seat (id, event_id, label, status) VALUES (2, 1, 'A2', 'AVAILABLE')");
    }

    @Test
    void test50ThreadsBookSameSeat() throws InterruptedException {
        int threadCount = 50;
        
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger seatTakenCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGun.await();
                    // Use seat 2 which we just created
                    bookingService.bookAtomic(1L, 2L, "User-" + Thread.currentThread().getId());
                    successCount.incrementAndGet();
                } catch (SeatTakenException e) {
                    seatTakenCount.incrementAndGet();
                } catch (Exception e) {
                    // Ignore other exceptions
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGun.countDown();
        finishLine.await();

        assertEquals(1, successCount.get(), "Only one thread should successfully book the seat");
        assertEquals(49, seatTakenCount.get(), "49 threads should fail with SeatTakenException");
    }
}