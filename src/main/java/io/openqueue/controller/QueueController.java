package io.openqueue.controller;

import com.alibaba.fastjson.JSONObject;
import io.openqueue.common.api.ResponseBody;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * @author chenjing
 */
@RestController
@RequestMapping(value = "/v1/queue", produces="application/json")
@Validated
public class QueueController {

    @Autowired
    private QueueService queueService;

    @PostMapping(value = "/setup")
    public Mono<ResponseEntity<ResponseBody>> setupQueue(@RequestBody QueueConfigDto queueConfigDto){
        return queueService.setupQueue(queueConfigDto);
    }

    @GetMapping(value = "/{queueId}/status")
    public Mono<ResponseEntity<ResponseBody>> getQueueStatus(@PathVariable("queueId") String queueId){
        return queueService.getQueueStatus(queueId);
    }

    @GetMapping(value = "/{queueId}/config")
    public Mono<ResponseEntity<ResponseBody>> getQueueConfig(@PathVariable("queueId") String queueId){
        return queueService.getQueueConfig(queueId);
    }

    @PutMapping(value = "/{queueId}/config", consumes="application/json")
    public Mono<ResponseEntity<ResponseBody>> updateQueueConfig(@PathVariable("queueId") String queueId,
                                                    @RequestBody QueueConfigDto queueConfigDto){
        return queueService.updateQueueConfig(queueId, queueConfigDto);
    }

    @DeleteMapping(value = "/{queueId}/close")
    public Mono<ResponseEntity<ResponseBody>> closeQueue(@PathVariable("queueId") String queueId){
        return queueService.closeQueue(queueId);
    }
}
