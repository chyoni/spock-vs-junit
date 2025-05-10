package cwchoiit.testmaster.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TimerHandler extends TextWebSocketHandler {

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[afterConnectionEstablished] session id: {}", session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[handleTransportError] session id: {}, exception: {}", session.getId(), exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("[afterConnectionClosed] session id: {}, status: {}", session.getId(), status.getReason());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("[handleTextMessage] session id: {}, message: {}", session.getId(), message.getPayload());

        try {
            long seconds = Long.parseLong(message.getPayload());
            long timestamp = Instant.now().toEpochMilli();

            executorService.schedule(
                    () -> sendMessage(session, String.format("%d에 등록한 %d초 타이머 완료.", timestamp, seconds)),
                    seconds,
                    TimeUnit.SECONDS
            );
            sendMessage(session, String.format("%d에 등록한 %d초 타이머 등록 완료", timestamp, seconds));
        } catch (Exception e) {
            sendMessage(session, "정수가 아님. 타이머 등록 실패");
        }
    }

    private static void sendMessage(WebSocketSession session, String message) {
        try {
            session.sendMessage(new TextMessage(message));
        } catch (IOException e) {
            log.error("[sendMessage] 메세지 전송 실패: to {}, error : {}", session.getId(), e.getMessage());
        }
    }
}
