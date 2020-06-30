package io.openqueue.repo;

import io.openqueue.model.Ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.io.Serializable;
import java.time.Instant;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class TicketRepoTest {

    @Autowired
    private ReactiveRedisTemplate<String, Serializable> reactiveRedisTemplate;

    @Autowired
    private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

    @Autowired
    private TicketRepo ticketRepo;

    @Autowired
    private QueueRepo queueRepo;

    private static String testTicketId;
    private static String testQueueId;
    private static Ticket ticket;

    @BeforeAll
    static void runBeforeAllTestMethod() {
        testQueueId = "q:sad1fghS";
        testTicketId = "t:" + testQueueId + ":1";
        ticket = Ticket.builder()
                .queueId(testQueueId)
                .id(testTicketId)
                .authCode("1asdIU2ay")
                .issueTime(Instant.now().getEpochSecond())
                .build();
    }

    @AfterEach
    void runAfterEachTestMethod() {
        cleanup();
    }

    @Test
    void testCreateFindAndRevokeTicket() {
        reactiveStringRedisTemplate.opsForHash().put(testQueueId, "id", testQueueId).block();
        reactiveRedisTemplate.opsForHash().put(testQueueId, "tail", 0).block();
        // Make sure no that ticket at first.
        StepVerifier.create(ticketRepo.findById(testTicketId))
                .expectComplete()
                .verify();

        // Create a new ticket.
        StepVerifier.create(ticketRepo.applyOne(ticket))
                .expectNext(1L)
                .expectComplete()
                .verify();

//        // Expect the ticket has been created.
//        StepVerifier.create(ticketRepo.findById(testTicketId))
//                .expectNext(ticket)
//                .expectComplete()
//                .verify();
//
//        // Delete this ticket.
//        StepVerifier.create(ticketRepo.revoke(testTicketId))
//                .expectNext(1L)
//                .expectComplete()
//                .verify();
//
//        // Expect this ticket has been deleted.
//        StepVerifier.create(ticketRepo.findById(testTicketId))
//                .expectComplete()
//                .verify();
    }

//    @Test
//    void testSetTicketOccupied() {
//        // 1. Create a new ticket.
//        // 2. Expect current ticket is not occupied.
//        StepVerifier.create(ticketRepo.create(ticket).then(ticketRepo.findById(testTicketId)))
//                .assertNext(ticket1 -> {
//                    assertThat(!ticket1.isOccupied());
//                })
//                .expectComplete()
//                .verify();
//
//        // 1. Set ticket occupied.
//        // 2. Expect current ticket is now occupied.
//        StepVerifier.create(ticketRepo.setOccupied(testTicketId).then(ticketRepo.findById(testTicketId)))
//                .assertNext(ticket1 -> {
//                    assertThat(ticket1.isOccupied());
//                })
//                .expectComplete()
//                .verify();
//    }

    @Test
    void testAddTicketToSet() {
        String setKey = "set:test";

        // Make sure no ticket in the set at first.
        StepVerifier.create(ticketRepo.isTicketInSet(setKey, testTicketId))
                .expectNext(Boolean.FALSE)
                .expectComplete()
                .verify();

        // Add ticket to set.
        long currentTime = Instant.now().getEpochSecond();
        StepVerifier.create(ticketRepo.addToSet(setKey, testTicketId, currentTime + 10))
                .expectComplete()
                .verify();

        // Expect ticket in set now.
        StepVerifier.create(ticketRepo.isTicketInSet(setKey, testTicketId))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();
    }

    @Test
    void testRemoveTicketById() {
        String setKey = "set:test";

        // 1. Add ticket to set.
        // 2. Make sure ticket in set at first.
        long currentTime = Instant.now().getEpochSecond();
        StepVerifier.create(ticketRepo.addToSet(setKey, testTicketId, currentTime + 10)
                .then(ticketRepo.isTicketInSet(setKey, testTicketId)))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        // Remove ticket by id
        StepVerifier.create(ticketRepo.removeOutOfSetById(setKey, testTicketId))
                .expectNext(1L)
                .expectComplete()
                .verify();

        // Make sure no ticket in the set now.
        StepVerifier.create(ticketRepo.isTicketInSet(setKey, testTicketId))
                .expectNext(Boolean.FALSE)
                .expectComplete()
                .verify();
    }

    @Test
    void testRemoveTicketByTime() {
        String setKey = "set:test";

        // 1. Add ticket to set.
        // 2. Make sure ticket in set at first.
        long currentTime = Instant.now().getEpochSecond();
        StepVerifier.create(ticketRepo.addToSet(setKey, testTicketId, currentTime)
                .then(ticketRepo.isTicketInSet(setKey, testTicketId)))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        // Remove ticket by early time
        StepVerifier.create(ticketRepo.removeOutOfSetByTime(setKey, currentTime - 1))
                .expectNext(0L)
                .expectComplete()
                .verify();

        // Expect ticket still in the set.
        StepVerifier.create(ticketRepo.isTicketInSet(setKey, testTicketId))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        // Remove ticket by post time
        StepVerifier.create(ticketRepo.removeOutOfSetByTime(setKey, currentTime + 1))
                .expectNext(1L)
                .expectComplete()
                .verify();

        // Expect ticket not in the set.
        StepVerifier.create(ticketRepo.isTicketInSet(setKey, testTicketId))
                .expectNext(Boolean.FALSE)
                .expectComplete()
                .verify();
    }


    @Test
    void testCountTicketInSet() {
        String setKey = "set:test";

        // Make sure has no ticket in set at first.
        StepVerifier.create(ticketRepo.countTicketInSet(setKey))
                .expectNext(0L)
                .expectComplete()
                .verify();

        // Add 999 tickets to set.
        Long count = Flux.fromStream(
                IntStream.range(1, 1000)
                        .boxed()
                        .map(index -> "ticket:test:" + index)
        )
                .flatMap(ticket -> ticketRepo.addToSet(setKey, ticket, new Random().nextInt()))
                .count()
                .block();

        assertThat(count != null && count.intValue() == 999);

        // Expect 999 tickets in set now.
        StepVerifier.create(ticketRepo.countTicketInSet(setKey))
                .expectNext(999L)
                .expectComplete()
                .verify();

        cleanup();

        // Expect no tickets left in set now.
        StepVerifier.create(ticketRepo.countTicketInSet(setKey))
                .expectNext(0L)
                .expectComplete()
                .verify();
    }

    void cleanup() {
        reactiveRedisTemplate
                .keys("*")
                .flatMap(key -> reactiveRedisTemplate.delete(key))
                .blockLast();
    }
}
