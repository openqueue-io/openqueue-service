package io.openqueue.service;

import io.openqueue.dto.QueueConfigDto;
import org.springframework.http.ResponseEntity;

/**
 * @author chenjing
 */
public class QueueService {
    public ResponseEntity<Object> setupQueue(QueueConfigDto queueConfigDto){
        return null;
    }

    public ResponseEntity<Object> getQueueStatus(String queueId){
        return null;
    }

    public ResponseEntity<Object> getQueueConfig(String queueId){
        return null;
    }

    public ResponseEntity<Object> updateQueueConfig(String queueId, QueueConfigDto queueConfigDto){
        return null;
    }

    public ResponseEntity<Object> closeQueue(String queueId){
        return null;
    }
}
