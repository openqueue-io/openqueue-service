package io.openqueue.controller;

import io.openqueue.dto.QueueConfigDto;
import io.openqueue.service.QueueService;
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
@RequestMapping(value = "/v1/queue", produces="application/json")
@Validated
public class QueueController {

    @Autowired
    private QueueService queueService;

    static  final Logger logger = LoggerFactory.getLogger(QueueController.class);

    @PostMapping(value = "/setup", consumes="application/json")
    public ResponseEntity setupQueue(@RequestBody QueueConfigDto queueConfigDto){
        return queueService.setupQueue(queueConfigDto);
    }

    @GetMapping(value = "/{queueId}/status")
    public ResponseEntity getQueueStatus(@PathVariable("queueId") String queueId){
        return queueService.getQueueStatus(queueId);
    }

    @GetMapping(value = "/{queueId}/config")
    public ResponseEntity getQueueConfig(@PathVariable("queueId") String queueId){
        return queueService.getQueueConfig(queueId);
    }

    @PutMapping(value = "/{queueId}/config", consumes="application/json")
    public ResponseEntity updateQueueConfig(@PathVariable("queueId") String queueId,
                                                    @RequestBody QueueConfigDto queueConfigDto){
        return queueService.updateQueueConfig(queueId, queueConfigDto);
    }

    @DeleteMapping(value = "/{queueId}/close")
    public ResponseEntity closeQueue(@PathVariable("queueId") String queueId){
        return queueService.closeQueue(queueId);
    }
}
