package io.openqueue.repo;

import io.openqueue.model.Ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
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
    private TicketRepo ticketRepo;

    private static String testTicketId;
    private static String testQueueId;
    private static Ticket ticket;

    @BeforeAll
    static void runBeforeAllTestMethod() {
        testQueueId = "q:sad1fghS";
        int position = new Random().nextInt(1000);
        testTicketId = "t:" + testQueueId + ":" + position;
        ticket = Ticket.builder()
                .authCode("1asdIU2ay")
                .issueTime(Instant.now().getEpochSecond())
                .id(testTicketId)
                .build();
    }

    @AfterEach
    void runAfterEachTestMethod() {
        cleanup();
    }

    @Test
    void testCreateFindAndRevokeTicket() {

        // Make sure no that ticket at first.
        StepVerifier.create(ticketRepo.findTicket(testTicketId))
                .expectComplete()
                .verify();

        // Create a new ticket.
        StepVerifier.create(ticketRepo.createTicket(ticket))
                .expectNext(ticket)
                .expectComplete()
                .verify();

        // Expect the ticket has been created.
        StepVerifier.create(ticketRepo.findTicket(testTicketId))
                .expectNext(ticket)
                .expectComplete()
                .verify();

        // Delete this ticket.
        StepVerifier.create(ticketRepo.revokeTicket(testTicketId))
                .expectNext(1L)
                .expectComplete()
                .verify();

        // Expect this ticket has been deleted.
        StepVerifier.create(ticketRepo.findTicket(testTicketId))
                .expectComplete()
                .verify();
    }

    @Test
    void testIncTicketUsage() {
        // 1. Create a new ticket.
        // 2. Expect current ticket usage is 0.
        StepVerifier.create(ticketRepo.createTicket(ticket)
                .then(ticketRepo.findTicket(testTicketId)))
                .assertNext(ticket1 -> {
                    assertThat(ticket1.getCountOfUsage() == 0);
                })
                .expectComplete()
                .verify();

        // Increase ticket usage.
        StepVerifier.create(ticketRepo.incTicketUsage(testTicketId))
                .expectNext(1L)
                .expectComplete()
                .verify();

        // Make sure current ticket usage is 1.
        StepVerifier.create(ticketRepo.findTicket(testTicketId))
                .assertNext(ticket1 -> {
                    assertThat(ticket1.getCountOfUsage() == 1);
                })
                .expectComplete()
                .verify();
    }

    @Test
    void testSetTicketActivateTime() {
        // 1. Create a new ticket.
        // 2. Expect current ticket active time is default value 0.
        StepVerifier.create(ticketRepo.createTicket(ticket)
                .then(ticketRepo.findTicket(testTicketId)))
                .assertNext(ticket1 -> {
                    assertThat(ticket1.getActivateTime() == 0L);
                })
                .expectComplete()
                .verify();

        long currentTime = Instant.now().getEpochSecond();
        // 1. Set ticket activate time.
        // 2. Expect current ticket activate time is {currentTime}.
        StepVerifier.create(ticketRepo.setTicketActivateTime(testTicketId, currentTime)
                .then(ticketRepo.findTicket(testTicketId)))
                .assertNext(ticket1 -> {
                    assertThat(ticket1.getActivateTime() == currentTime);
                })
                .expectComplete()
                .verify();
    }

    @Test
    void testSetTicketOccupied() {
        // 1. Create a new ticket.
        // 2. Expect current ticket is not occupied.
        StepVerifier.create(ticketRepo.createTicket(ticket).then(ticketRepo.findTicket(testTicketId)))
                .assertNext(ticket1 -> {
                    assertThat(!ticket1.isOccupied());
                })
                .expectComplete()
                .verify();

        // 1. Set ticket occupied.
        // 2. Expect current ticket is now occupied.
        StepVerifier.create(ticketRepo.setTicketOccupied(testTicketId).then(ticketRepo.findTicket(testTicketId)))
                .assertNext(ticket1 -> {
                    assertThat(ticket1.isOccupied());
                })
                .expectComplete()
                .verify();
    }

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
        StepVerifier.create(ticketRepo.addTicketToSet(setKey, testTicketId, currentTime + 10))
                .expectNext(Boolean.TRUE)
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
        StepVerifier.create(ticketRepo.addTicketToSet(setKey, testTicketId, currentTime + 10)
                .then(ticketRepo.isTicketInSet(setKey, testTicketId)))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        // Remove ticket by id
        StepVerifier.create(ticketRepo.removeTicketOutOfSetById(setKey, testTicketId))
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
        StepVerifier.create(ticketRepo.addTicketToSet(setKey, testTicketId, currentTime)
                .then(ticketRepo.isTicketInSet(setKey, testTicketId)))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        // Remove ticket by early time
        StepVerifier.create(ticketRepo.removeTicketOutOfSetByTime(setKey, currentTime - 1))
                .expectNext(0L)
                .expectComplete()
                .verify();

        // Expect ticket still in the set.
        StepVerifier.create(ticketRepo.isTicketInSet(setKey, testTicketId))
                .expectNext(Boolean.TRUE)
                .expectComplete()
                .verify();

        // Remove ticket by post time
        StepVerifier.create(ticketRepo.removeTicketOutOfSetByTime(setKey, currentTime + 1))
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
                .flatMap(ticket -> ticketRepo.addTicketToSet(setKey, ticket, new Random().nextInt()))
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
