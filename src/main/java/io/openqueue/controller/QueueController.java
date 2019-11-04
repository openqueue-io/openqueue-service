package io.openqueue.controller;

import io.openqueue.dto.QueueConfigDto;
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

    @PostMapping(value = "/setup")
    public ResponseEntity<Object> setupQueue(@RequestBody QueueConfigDto queueConfigDto){
        return new ResponseEntity<>(queueConfigDto, HttpStatus.OK);
    }

    @GetMapping(value = "/{queueId}/status")
    public ResponseEntity<Object> getQueueStatus(@PathVariable("queueId") String queueId){
        return new ResponseEntity<>("getQueueStatus:" + queueId , HttpStatus.OK);
    }

    @GetMapping(value = "/{queueId}/config")
    public ResponseEntity<Object> getQueueConfig(@PathVariable("queueId") String queueId){
        return new ResponseEntity<>("getQueueConfig:" + queueId , HttpStatus.OK);
    }

    @PutMapping(value = "/{queueId}/config")
    public ResponseEntity<Object> updateQueueConfig(@PathVariable("queueId") String queueId,
                                                    @RequestBody QueueConfigDto queueConfigDto){
        return new ResponseEntity<>("adjustQueue:" + queueId , HttpStatus.OK);
    }

    @DeleteMapping(value = "/{queueId}/close")
    public ResponseEntity<Object> closeQueue(@PathVariable("queueId") String queueId){
        return new ResponseEntity<>("closeQueue:" + queueId, HttpStatus.OK);
    }
}
