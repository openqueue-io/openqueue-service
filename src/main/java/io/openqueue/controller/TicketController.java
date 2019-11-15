package io.openqueue.controller;

import io.openqueue.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author chenjing
 */
@RestController
@RequestMapping("/v1/ticket")
@Validated
public class TicketController {

    @Autowired
    private TicketService ticketService;

    static  final Logger logger = LoggerFactory.getLogger(TicketController.class);

    @PostMapping(value = "/apply")
    public ResponseEntity applyTicket(@RequestParam String queueId){
        return ticketService.applyTicket(queueId);
    }

    @GetMapping(value = "/{ticketId}/usage_stat")
    public ResponseEntity getTicketUsage(@PathVariable("ticketId") String ticketId){
        return ticketService.getTicketUsage(ticketId);
    }

    @GetMapping(value = "/{ticketId}/availability")
    public ResponseEntity checkTicketAvailability(@PathVariable("ticketId") String ticketId){
        return ticketService.checkTicketAvailability(ticketId);
    }

    @PutMapping(value = "/{ticketId}/mark_ticket_in_use")
    public ResponseEntity markTicketInUse(@PathVariable("ticketId") String ticketId){
        return ticketService.markTicketInUse(ticketId);
    }

    @PutMapping(value = "/{ticketId}/activate")
    public ResponseEntity activateTicket(@PathVariable("ticketId") String ticketId){
        return ticketService.activateTicket(ticketId);
    }

    @DeleteMapping(value = "/{ticketId}/revoke")
    public ResponseEntity revokeTicket(@PathVariable("ticketId") String ticketId){
        return ticketService.revokeTicket(ticketId);
    }
}
