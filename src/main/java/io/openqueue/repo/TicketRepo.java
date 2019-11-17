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

    public boolean isTicketActive(String ticketAuthCode, String queueId){
        return false;
    }

    public boolean isTicketReadyForActivate(String queueId, int position){
        return false;
    }

    public void setTicketOccupied(String ticketAuthCode){

    }

    public void activateTicket(Ticket ticket){

    }

    public void revokeTicket(Ticket ticket){

    }
}
