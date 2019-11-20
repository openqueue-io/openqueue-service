package io.openqueue.service;

import io.openqueue.repo.TicketRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.verify;

@SpringBootTest
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @MockBean
    private TicketRepo ticketRepo;

    @Test
    void testApplyTicket(){
        ticketService.applyTicket("1234");

//        verify(ticketRepo).createTicket("1234");
    }

    @Test
    void testTicketUsageStat(){
//        ticketService.getTicketUsageStat("1234");

        verify(ticketRepo).findTicket("1234");
    }

    @Test
    void testGetTicketAuthorization(){
//        ticketService.getTicketAuthorization("1234", "qtest");
//
//        verify(ticketRepo).
    }

    @Test
    void testMarkTicketInUse(){
//        ticketService.setTicketOccupied("1234");

        verify(ticketRepo).setTicketOccupied("1234");
    }

    @Test
    void testActivateTicket(){
//        ticketService.activateTicket("1234");

//        verify(ticketRepo).activateTicket("1234");
    }

    @Test
    void testRevokeTicket(){
//        ticketService.revokeTicket("1234");

//        verify(ticketRepo).revokeTicket("1234");
    }
}
