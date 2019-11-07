package io.openqueue.repo;

import io.openqueue.dto.QueueConfigDto;
import io.openqueue.dto.QueueStatusDto;
import org.springframework.stereotype.Repository;

/**
 * @author chenjing
 */
@Repository
public class QueueRepo {
    public void setupQueue(String queueId, QueueConfigDto queueConfigDto) { }

    public QueueStatusDto getQueueStatus(String queueId){
        return null;
    }

    public QueueConfigDto getQueueConfig(String queueId){
        return null;
    }

    public void updateQueueConfig(String queueId, QueueConfigDto queueConfigDto) { }

    public void closeQueue(String queueId) { }
}
