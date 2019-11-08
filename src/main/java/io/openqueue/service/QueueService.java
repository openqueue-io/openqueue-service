package io.openqueue.service;

import io.openqueue.common.api.ResponseBody;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.dto.QueueStatusDto;
import io.openqueue.repo.QueueRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;


import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author chenjing
 */
@Service
public class QueueService {
    @Autowired
    private QueueRepo queueRepo;

    public ResponseEntity setupQueue(QueueConfigDto queueConfigDto) {
        String id = "Q" + Math.abs(UUID.randomUUID().toString().hashCode());

        queueRepo.setupQueue(id, queueConfigDto);

        Map<String, String> data = new HashMap<>(1);
        data.put("queue_id", id);

        ResponseBody responseBody = ResponseBody.builder()
                    .data(data)
                    .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody);
    }

    public ResponseEntity getQueueStatus(String queueId){
        QueueStatusDto queueStatusDto = queueRepo.getQueueStatus(queueId);

        ResponseBody responseBody = ResponseBody.builder()
                .data(queueStatusDto)
                .build();
        return ResponseEntity.ok(responseBody);
    }

    public ResponseEntity getQueueConfig(String queueId){
        QueueConfigDto queueConfigDto = queueRepo.getQueueConfig(queueId);

        ResponseBody responseBody = ResponseBody.builder()
                .data(queueConfigDto)
                .build();
        return ResponseEntity.ok(responseBody);
    }

    public ResponseEntity updateQueueConfig(String queueId, QueueConfigDto queueConfigDto){
        queueRepo.updateQueueConfig(queueId, queueConfigDto);

        ResponseBody responseBody = ResponseBody.builder().build();
        return ResponseEntity.ok(responseBody);
    }

    public ResponseEntity closeQueue(String queueId) {
        queueRepo.closeQueue(queueId);

        ResponseBody responseBody = ResponseBody.builder().build();
        return ResponseEntity.ok(responseBody);
    }

}
