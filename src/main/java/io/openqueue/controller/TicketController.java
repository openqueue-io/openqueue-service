package io.openqueue.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
    static  final Logger logger = LoggerFactory.getLogger(TicketController.class);

    @PostMapping(value = "/apply")
    public ResponseEntity<Object> applyTicket(@RequestParam String queueId){
        return new ResponseEntity<>("createTicket:" + queueId, HttpStatus.OK);
    }

    @GetMapping(value = "/{ticketId}/detail")
    public ResponseEntity<Object> getTicketDetail(@PathVariable("ticketId") String ticketId){
        return new ResponseEntity<>("getTicketDetail:" + ticketId , HttpStatus.OK);
    }

    @GetMapping(value = "/{ticketId}/usage_stat")
    public ResponseEntity<Object> getTicketUsage(@PathVariable("ticketId") String ticketId){
        return new ResponseEntity<>("getTicketUsage:" + ticketId , HttpStatus.OK);
    }

    @GetMapping(value = "/{ticketId}/availability")
    public ResponseEntity<Object> checkTicketAvailability(@PathVariable("ticketId") String ticketId){
        return new ResponseEntity<>("isTicketAvailable:" + ticketId , HttpStatus.OK);
    }

    @PutMapping(value = "/{ticketId}/mark_in_use")
    public ResponseEntity<Object> markTicketAsInUse(@PathVariable("ticketId") String ticketId){
        return new ResponseEntity<>("markTicketAsUsed:" + ticketId , HttpStatus.OK);
    }

    @PutMapping(value = "/{ticketId}/activate")
    public ResponseEntity<Object> activateTicket(@PathVariable("ticketId") String ticketId){
        return new ResponseEntity<>("activateTicket:" + ticketId , HttpStatus.OK);
    }

    @DeleteMapping(value = "/{ticketId}/revoke")
    public ResponseEntity<Object> revokeTicket(@PathVariable("ticketId") String ticketId){
        return new ResponseEntity<>("revokeTicket:" + ticketId, HttpStatus.OK);
    }
}
