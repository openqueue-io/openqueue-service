package io.openqueue.service;

import io.openqueue.common.api.Response;
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

    public ResponseEntity<Object> setupQueue(QueueConfigDto queueConfigDto) {
        String id = "Q" + Math.abs(UUID.randomUUID().toString().hashCode());

        queueRepo.setupQueue(id, queueConfigDto);

        Map<String, String> data = new HashMap<>(1);
        data.put("queue_id", id);
        Response<Map> response = new Response<>();
        response.setData(data);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<Object> getQueueStatus(String queueId){
        QueueStatusDto queueStatusDto = queueRepo.getQueueStatus(queueId);

        Response<QueueStatusDto> response = new Response<>();
        response.setData(queueStatusDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<Object> getQueueConfig(String queueId){
        QueueConfigDto queueConfigDto = queueRepo.getQueueConfig(queueId);

        Response<QueueConfigDto> response = new Response<>();
        response.setData(queueConfigDto);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<Object> updateQueueConfig(String queueId, QueueConfigDto queueConfigDto){
        queueRepo.updateQueueConfig(queueId, queueConfigDto);

        Response response = new Response<>();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public ResponseEntity<Object> closeQueue(String queueId) {
        queueRepo.closeQueue(queueId);

        Response response = new Response<>();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
