package io.openqueue.controller;

import io.openqueue.common.constant.TicketState;
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
    public ResponseEntity applyTicket(@RequestParam String qid){
        return ticketService.applyTicket(qid);
    }

    @GetMapping(value = "/{ticket}/stat")
    public ResponseEntity getTicketUsageStat(@PathVariable("ticket") String ticketIdAuth){
        return ticketService.getTicketUsageStat(ticketIdAuth);
    }

    @GetMapping(value = "/{ticket}/authorization")
    public ResponseEntity getTicketAuthorization(@PathVariable("{ticket}") String ticketIdAuth,
                                                 @RequestParam String qid){
        return ticketService.getTicketAuthorization(ticketIdAuth, qid);
    }

    @PutMapping(value = "/{ticket}/state")
    public ResponseEntity updateTicketState(@PathVariable("ticket") String ticket, @RequestParam String state){
        switch (state) {
            case TicketState.ACTIVE:
                return ticketService.activateTicket(ticket);
            case TicketState.USED:
                return ticketService.markTicketInUse(ticket);
            case TicketState.REVOKED:
                return ticketService.revokeTicket(ticket);
            default:
                return null;
        }
    }
}
