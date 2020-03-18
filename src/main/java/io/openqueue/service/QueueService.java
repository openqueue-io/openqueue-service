package io.openqueue.service;

import io.openqueue.common.api.ResponseBody;
import io.openqueue.common.api.ResultCode;
import io.openqueue.common.util.RandomCodeGenerator;
import io.openqueue.common.util.TypeConverter;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.dto.QueueSetupDto;
import io.openqueue.dto.QueueStatusDto;
import io.openqueue.model.Queue;
import io.openqueue.repo.QueueRepo;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author chenjing
 */
@Service
public class QueueService {

    @Autowired
    private QueueRepo queueRepo;

    public Mono<ResponseEntity<ResponseBody>> setupQueue(QueueConfigDto queueConfigDto) {

        Set<String> allQueueIds = queueRepo.findAllId()
                .reduce(new HashSet<String>(), (set, id) -> {
                    set.add(id);
                    return set;
                })
                .defaultIfEmpty(new HashSet<>())
                .block();

        String qid = "";
        while (Strings.isBlank(qid)) {
            String newId = RandomCodeGenerator.getQueueId();
            if (!Objects.requireNonNull(allQueueIds).contains(newId)) {
                qid = newId;
            }
        }

        Queue queue = TypeConverter.cast(queueConfigDto, Queue.class);
        queue.setId(qid);

        QueueSetupDto queueSetupDto = QueueSetupDto.builder()
                .queueId(qid)
                .queueUrl("webapp.openqueue.cloud/q/" + qid.split(":")[1])
                .callbackFormat(queueConfigDto.getCallbackURL() + "?opqticket=xxxxxx")
                .build();

        ResponseBody responseBody = new ResponseBody(ResultCode.SETUP_QUEUE_SUCCESS, queueSetupDto);

        return queueRepo.createOrUpdate(queue)
                .then(queueRepo.addToSet(qid))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(responseBody));

    }

    public Mono<ResponseEntity<ResponseBody>> getQueueStatus(String queueId) {
        return queueRepo.findById(queueId)
                .flatMap(queue -> {
                    QueueStatusDto queueStatusDto = QueueStatusDto.builder()
                            .head(queue.getHead())
                            .tail(queue.getTail())
                            .build();
                    ResponseBody responseBody = new ResponseBody(ResultCode.GET_QUEUE_STATUS_SUCCESS, queueStatusDto);
                    return Mono.just(ResponseEntity.ok(responseBody));
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseBody(ResultCode.QUEUE_NOT_EXIST_EXCEPTION)));
    }

    public Mono<ResponseEntity<ResponseBody>> getQueueConfig(String queueId) {
        return queueRepo.findById(queueId)
                .flatMap(queue -> {
                    QueueConfigDto queueConfigDto = TypeConverter.cast(queue, QueueConfigDto.class);
                    ResponseBody responseBody = new ResponseBody(ResultCode.GET_QUEUE_CONFIG_SUCCESS, queueConfigDto);
                    return Mono.just(ResponseEntity.ok(responseBody));
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseBody(ResultCode.QUEUE_NOT_EXIST_EXCEPTION)));
    }

    public Mono<ResponseEntity<ResponseBody>> updateQueueConfig(String queueId, QueueConfigDto queueConfigDto) {
        return queueRepo.findById(queueId)
                .flatMap(queue -> {
                    Queue newQueue = TypeConverter.cast(queueConfigDto, Queue.class);
                    newQueue.setId(queue.getId());
                    return queueRepo.createOrUpdate(newQueue);
                })
                .flatMap(queue -> Mono.just(ResponseEntity.ok().body(new ResponseBody(ResultCode.UPDATE_QUEUE_CONFIG_SUCCESS))))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseBody(ResultCode.QUEUE_NOT_EXIST_EXCEPTION)));
    }

    public Mono<ResponseEntity<ResponseBody>> closeQueue(String queueId) {
        return queueRepo.findById(queueId)
                .flatMap(queue -> queueRepo.close(queue.getId()))
                .flatMap(success -> {
                    if (success) {
                        ResponseBody responseBody = new ResponseBody(ResultCode.CLOSE_QUEUE_SUCCESS);
                        return Mono.just(ResponseEntity.ok(responseBody));
                    } else {
                        ResponseBody responseBody = new ResponseBody(ResultCode.CLOSE_QUEUE_FAILED);
                        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseBody));
                    }
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ResponseBody(ResultCode.QUEUE_NOT_EXIST_EXCEPTION)));
    }

}
