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
@RequestMapping("/v1/queue")
@Validated
public class QueueController {
    static  final Logger logger = LoggerFactory.getLogger(QueueController.class);

    @PostMapping()
    public ResponseEntity<Object> createQueue(@RequestBody String queueId){
        return new ResponseEntity<>("createQueue:", HttpStatus.OK);
    }

    @DeleteMapping(value = "/{queueId}")
    public ResponseEntity<Object> deleteQueue(@PathVariable("queueId") String queueId){
        return new ResponseEntity<>("createQueue:", HttpStatus.OK);
    }

    @PutMapping()
    public ResponseEntity<Object> updateQueue(@PathVariable("queueId") String queueId){
        return new ResponseEntity<>("getQueueInfoById:" + queueId , HttpStatus.OK);
    }

    @GetMapping(value = "/{queueId}")
    public ResponseEntity<Object> getQueueInfo(@PathVariable("queueId") String queueId){
        return new ResponseEntity<>("getQueueInfoById:" + queueId , HttpStatus.OK);
    }
}
