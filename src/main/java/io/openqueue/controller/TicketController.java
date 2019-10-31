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

    @PostMapping()
    public ResponseEntity<Object> applyTicket(@RequestBody String queueId){
        return new ResponseEntity<>("createTicket:" + queueId, HttpStatus.OK);
    }

    @GetMapping(value = "/{ticketId}")
    public ResponseEntity<Object> getTicketInfo(@PathVariable("ticketId") String ticketId){
        return new ResponseEntity<>("getTicketInfo:" + ticketId , HttpStatus.OK);
    }

    @GetMapping(value = "/verify")
    public ResponseEntity<Object> verifyTicket(@RequestParam("ticketId") String ticketId){
        return new ResponseEntity<>("verifyTicket:" + ticketId , HttpStatus.OK);
    }



    @PutMapping()
    public ResponseEntity<Object> updateQueue(@PathVariable("queueId") String queueId){
        return new ResponseEntity<>("getQueueInfoById" + queueId , HttpStatus.OK);
    }
}
