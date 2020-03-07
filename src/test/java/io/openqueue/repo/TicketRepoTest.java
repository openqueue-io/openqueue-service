package io.openqueue.repo;

import io.openqueue.model.Ticket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import java.io.Serializable;
import java.time.Instant;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class TicketRepoTest {

    @Autowired
    private RedisTemplate<String, Serializable> redisTemplate;

    @Autowired
    private TicketRepo ticketRepo;

    private static String testTicketId;
    private static String testQueueId;

    @BeforeAll
    static void runBeforeAllTestMethod() {
        testQueueId = "q:sad1fghS";
        int position = new Random().nextInt(1000);
        testTicketId = "t:" + testQueueId + ":" + position;
    }

    @AfterEach
    void runAfterEachTestMethod() {
        cleanup();
    }

    @Test
    void testCreateFindAndRevokeTicket() {
        System.out.println("ticketId: " + testTicketId);

        Ticket ticket = Ticket.builder()
                .authCode("1asdIU2ay")
                .issueTime(Instant.now().getEpochSecond())
                .id(testTicketId)
                .build();
        ticketRepo.createTicket(ticket);

        Ticket ticket1 = ticketRepo.findTicket(testTicketId);
        assertThat(ticket).isEqualTo(ticket1);

        ticketRepo.revokeTicket(testTicketId);
        Ticket ticket2 = ticketRepo.findTicket(testTicketId);
        assertThat(ticket2).isNull();
    }

    @Test
    void testIncUsageAndSetActivateTime() {
        Ticket ticket = Ticket.builder()
                .authCode("1asdIU2ay")
                .issueTime(Instant.now().getEpochSecond())
                .id(testTicketId)
                .build();

        ticketRepo.createTicket(ticket);

        ticketRepo.incUsage(ticket.getId());

        long currentTime = Instant.now().getEpochSecond();
        ticketRepo.setActivateTime(ticket.getId(), currentTime);

        Ticket ticket1 = ticketRepo.findTicket(testTicketId);
        assertThat(ticket1.getCountOfUsage()).isEqualTo(1);
        assertThat(ticket1.getActivateTime()).isEqualTo(currentTime);
    }

    @Test
    void testAddCheckDeleteElementInSet() {
        String activeSet = "set:active:" + testQueueId;

        ticketRepo.addElementToSet(testTicketId, activeSet, 300);

        assertThat(ticketRepo.isElementInSet(testTicketId, activeSet)).isTrue();
        assertThat(ticketRepo.isElementInSet(testTicketId + "1", activeSet)).isFalse();

        ticketRepo.removeElementOutOfSet(testTicketId, activeSet);

        assertThat(ticketRepo.isElementInSet(testTicketId, activeSet)).isFalse();
    }

    @Test
    void testSetTicketOccupied() {
        Ticket ticket = Ticket.builder()
                .authCode("1asdIU2ay")
                .issueTime(Instant.now().getEpochSecond())
                .id(testTicketId)
                .build();
        ticketRepo.createTicket(ticket);

        Ticket ticket1 = ticketRepo.findTicket(testTicketId);
        assertThat(ticket1.isOccupied()).isFalse();

        ticketRepo.setTicketOccupied(testTicketId);
        Ticket ticket2 = ticketRepo.findTicket(testTicketId);
        assertThat(ticket2.isOccupied()).isTrue();
    }

    void cleanup() {
        Set<String> keys = redisTemplate.keys("*");
        redisTemplate.delete(keys);
    }
}
