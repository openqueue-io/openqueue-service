package io.openqueue.controller;

import com.alibaba.fastjson.JSONObject;
import io.openqueue.common.api.ResponseBody;
import io.openqueue.common.validation.SetupQueue;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.validation.constraints.NotNull;

/**
 * @author chenjing
 */
@RestController
@RequestMapping(value = "/api/v1/queue", produces="application/json")
@Validated
public class QueueController {

    @Autowired
    private QueueService queueService;

    @PostMapping
    public Mono<ResponseEntity<ResponseBody>> setupQueue(@RequestBody @Validated(SetupQueue.class) QueueConfigDto queueConfigDto){
        return queueService.setupQueue(queueConfigDto);
    }

    @GetMapping(value = "/status")
    public Mono<ResponseEntity<ResponseBody>> getQueueStatus(@RequestParam String queueId){
        return queueService.getQueueStatus(queueId);
    }

    @GetMapping(value = "/config")
    public Mono<ResponseEntity<ResponseBody>> getQueueConfig(@RequestParam  String queueId){
        return queueService.getQueueConfig(queueId);
    }

    @PutMapping(value = "/config")
    public Mono<ResponseEntity<ResponseBody>> updateQueueConfig(@RequestParam String queueId,
                                                    @RequestBody QueueConfigDto queueConfigDto){
        return queueService.updateQueueConfig(queueId, queueConfigDto);
    }

    @DeleteMapping
    public Mono<ResponseEntity<ResponseBody>> closeQueue(@RequestParam String queueId){
        return queueService.closeQueue(queueId);
    }
}
