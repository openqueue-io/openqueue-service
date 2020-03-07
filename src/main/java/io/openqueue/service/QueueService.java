package io.openqueue.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.openqueue.common.api.ResponseBody;
import io.openqueue.common.api.ResultCode;
import io.openqueue.common.util.RandomCodeGenerator;
import io.openqueue.dto.QueueConfigDto;
import io.openqueue.dto.QueueSetupDto;
import io.openqueue.dto.QueueStatusDto;
import io.openqueue.model.Queue;
import io.openqueue.repo.QueueRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * @author chenjing
 */
@Service
public class QueueService {
    @Autowired
    private QueueRepo queueRepo;

    public ResponseEntity<JSONObject> setupQueue(QueueConfigDto queueConfigDto) {
        String qid = "q:" + RandomCodeGenerator.getCode();

        JSONObject jsonObject = (JSONObject) JSON.toJSON(queueConfigDto);
        Queue queue = jsonObject.toJavaObject(Queue.class);
        queue.setId(qid);

        queueRepo.setupQueue(queue);

        QueueSetupDto queueSetupDto = QueueSetupDto.builder()
                .queueId(qid)
                .queueUrl("webapp.openqueue.cloud/q/" + qid.split(":")[1])
                .callbackFormat(queueConfigDto.getCallbackURL() + "?opqticket=xxxxxx")
                .build();

        ResponseBody responseBody = ResponseBody.builder()
                    .resultCode(ResultCode.SETUP_QUEUE_SUCCESS)
                    .data(queueSetupDto)
                    .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(responseBody.toJSON());
    }

    public ResponseEntity<JSONObject> getQueueStatus(String queueId){
        Queue queue = queueRepo.getQueue(queueId);
        QueueStatusDto queueStatusDto = QueueStatusDto.builder()
                .head(queue.getHead())
                .tail(queue.getTail())
                .build();

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.GET_QUEUE_STATUS_SUCCESS)
                .data(queueStatusDto)
                .build();
        return ResponseEntity.ok(responseBody.toJSON());
    }

    public ResponseEntity<JSONObject> getQueueConfig(String queueId){
        Queue queue = queueRepo.getQueue(queueId);

        QueueConfigDto queueConfigDto = QueueConfigDto.builder()
                .callbackURL(queue.getCallbackURL())
                .capacity(queue.getCapacity())
                .maxActiveUsers(queue.getMaxActiveUsers())
                .name(queue.getName())
                .availableSecondPerUser(queue.getAvailableSecondPerUser())
                .build();

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.GET_QUEUE_CONFIG_SUCCESS)
                .data(queueConfigDto)
                .build();
        return ResponseEntity.ok(responseBody.toJSON());
    }

    public ResponseEntity<JSONObject> updateQueueConfig(String queueId, QueueConfigDto queueConfigDto){
        queueRepo.updateQueueConfig(queueId, queueConfigDto);

        ResponseBody responseBody = ResponseBody.builder()
                .resultCode(ResultCode.UPDATE_QUEUE_CONFIG_SUCCESS)
                .build();
        return ResponseEntity.ok(responseBody.toJSON());
    }

    public ResponseEntity<JSONObject> closeQueue(String queueId) {
        queueRepo.closeQueue(queueId);

        ResponseBody responseBody = ResponseBody
                .builder()
                .resultCode(ResultCode.CLOSE_QUEUE_SUCCESS)
                .build();
        return ResponseEntity.ok(responseBody.toJSON());
    }

}
