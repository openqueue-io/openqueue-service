package io.openqueue.model;

import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.FluxSink;

public class WebSocketSender {

    private WebSocketSession webSocketSession;
    private FluxSink<WebSocketMessage> fluxSink;

    public WebSocketSender(WebSocketSession session, FluxSink<WebSocketMessage> sink) {
        this.webSocketSession = session;
        this.fluxSink = sink;
    }

    public void send(String data) {
        fluxSink.next(webSocketSession.textMessage(data));
    }

    public boolean isClosed() {
        return fluxSink.isCancelled();
    }
}
