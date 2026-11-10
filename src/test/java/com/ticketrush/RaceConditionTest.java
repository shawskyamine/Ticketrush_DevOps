package com.ticketrush;

import com.ticketrush.exception.SeatTakenException;
import com.ticketrush.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class RaceConditionTest {

    @Autowired
    private BookingService bookingService;

    @Test
    void test50ThreadsBookSameSeat() throws InterruptedException {
        int threadCount = 50;
        
        // Le pistolet de départ
        CountDownLatch startGun = new CountDownLatch(1);
        // La ligne d'arrivée
        CountDownLatch finishLine = new CountDownLatch(threadCount);
        
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger seatTakenCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // Tous les threads attendent ici
                    startGun.await();
                    
                    // On appelle explicitement la stratégie ATOMIC pour ce test
                    bookingService.bookAtomic(1L, 2L, "User-" + Thread.currentThread().getId());
                    successCount.incrementAndGet();
                } catch (SeatTakenException e) {
                    seatTakenCount.incrementAndGet();
                } catch (Exception e) {
                    // On ignore les autres exceptions pour ce test
                } finally {
                    finishLine.countDown();
                }
            });
        }

        // FEU ! Tous les threads partent en même temps
        startGun.countDown();
        
        // On attend que tout le monde ait fini
        finishLine.await();

        // Vérifications finales
        assertEquals(1, successCount.get(), "Only one thread should successfully book the seat");
        assertEquals(49, seatTakenCount.get(), "49 threads should fail with SeatTakenException");
    }
}