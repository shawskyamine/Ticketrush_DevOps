package com.ticketrush;

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
class BookAnyRaceTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Clear existing data
        jdbcTemplate.execute("DELETE FROM seat");
        jdbcTemplate.execute("DELETE FROM event");
        
        // Create event 1
        jdbcTemplate.execute("INSERT INTO event (id, name, total_seats) VALUES (1, 'Concert A', 50)");
        
        // Create 50 seats for event 1
        for (int i = 1; i <= 50; i++) {
            jdbcTemplate.execute("INSERT INTO seat (event_id, label, status) VALUES (1, 'B" + i + "', 'AVAILABLE')");
        }
    }

    @Test
    void test50ThreadsBookAnySeat() throws InterruptedException {
        int threadCount = 50;
        
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGun.await();
                    bookingService.bookAny(1L, "User-" + Thread.currentThread().getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startGun.countDown();
        finishLine.await();

        assertEquals(50, successCount.get(), "All 50 threads should successfully book a seat");
        assertEquals(0, failCount.get(), "No thread should fail because we have 50 seats and 50 users");
    }
}