package io.openqueue.config;

import io.openqueue.model.WebSocketSender;
import org.eclipse.jetty.util.MultiMap;
import org.eclipse.jetty.util.UrlEncoded;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class QueueStatusHandler implements WebSocketHandler {

    @Autowired
    public ConcurrentHashMap<String, Set<WebSocketSender>> queueSubscriber;

    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {
        HandshakeInfo handshakeInfo = webSocketSession.getHandshakeInfo();
        Optional<String> query = Optional.ofNullable(handshakeInfo.getUri().getQuery());
        MultiMap<String> values = new MultiMap<>();
        UrlEncoded.decodeTo(query.orElse(""), values, "UTF-8");
        String queueId = values.getValue("queueId", 0);
        if (StringUtils.isEmpty(queueId)) {
            Mono<WebSocketMessage> errorMessage = Mono.just(webSocketSession.textMessage("Missing query parameter 'queueId'."));
            return webSocketSession.send(errorMessage).then();
        }

        return webSocketSession.send(Flux.create(sink -> {
            String qid = "q:" + queueId;
            if (queueSubscriber.containsKey(qid)) {
                queueSubscriber.get(qid).add(new WebSocketSender(webSocketSession, sink));
            } else {
                sink.next(webSocketSession.textMessage("Queue not found."));
            }
        }));
    }

}
