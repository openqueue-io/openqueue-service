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
@RequestMapping("/v1/queue")
@Validated
public class QueueController {

    @Autowired
    private QueueService queueService;

    static  final Logger logger = LoggerFactory.getLogger(QueueController.class);

    @PostMapping(value = "/setup")
    public ResponseEntity<Object> setupQueue(@RequestBody QueueConfigDto queueConfigDto){
        return queueService.setupQueue(queueConfigDto);
    }

    @GetMapping(value = "/{queueId}/status")
    public ResponseEntity<Object> getQueueStatus(@PathVariable("queueId") String queueId){
        return queueService.getQueueStatus(queueId);
    }

    @GetMapping(value = "/{queueId}/config")
    public ResponseEntity<Object> getQueueConfig(@PathVariable("queueId") String queueId){
        return queueService.getQueueConfig(queueId);
    }

    @PutMapping(value = "/{queueId}/config")
    public ResponseEntity<Object> updateQueueConfig(@PathVariable("queueId") String queueId,
                                                    @RequestBody QueueConfigDto queueConfigDto){
        return queueService.updateQueueConfig(queueId, queueConfigDto);
    }

    @DeleteMapping(value = "/{queueId}/close")
    public ResponseEntity<Object> closeQueue(@PathVariable("queueId") String queueId){
        return queueService.closeQueue(queueId);
    }
}
