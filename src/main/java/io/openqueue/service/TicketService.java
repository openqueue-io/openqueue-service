package io.openqueue.service;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * @author chenjing
 */
@Service
public class TicketService {

    public ResponseEntity applyTicket(String queueId){
        return null;
    }

    public ResponseEntity getTicketUsage(String ticketId){
        return null;
    }

    public ResponseEntity checkTicketAvailability(String ticketId){
        return null;
    }

    public ResponseEntity markTicketInUse(String ticketId){
        return null;
    }

    public ResponseEntity activateTicket(String ticketId){
        return null;
    }

    public ResponseEntity revokeTicket(String ticketId){
        return null;
    }
}
