package io.openqueue.repo;

import io.openqueue.model.Ticket;
import org.springframework.stereotype.Repository;

/**
 * @author chenjing
 */
@Repository
public class TicketRepo {

    public Ticket createTicket(String queueId){
        return null;
    }

    public Ticket findTicket(String ticketId){
        return null;
    }

    public boolean isTicketActive(String ticketId){
        return false;
    }

    public boolean isTicketReadyForActivate(String ticketId){
        return false;
    }

    public void markTicketInUse(String ticketId){

    }

    public void activateTicket(String ticketId){

    }

    public void revokeTicket(String ticketId){

    }
}
