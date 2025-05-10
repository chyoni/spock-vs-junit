package cwchoiit.testmaster.handler;

import cwchoiit.testmaster.TestMasterApplication;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(
        classes = TestMasterApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class TimerHandlerTest {

    @LocalServerPort
    int port;

    @Test
    @DisplayName("WebSocket Test")
    void websocket() throws ExecutionException, InterruptedException, IOException {
        String url = "ws://localhost:%s/ws/timer".formatted(port);

        StandardWebSocketClient client = new StandardWebSocketClient();
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(1);

        WebSocketSession webSocketSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
                queue.put(message.getPayload());
            }
        }, url).get();

        webSocketSession.sendMessage(new TextMessage("2"));

        String firstResponse = queue.poll(1, TimeUnit.SECONDS);
        assertThat(firstResponse).contains("등록 완료");

        String secondResponse = queue.poll(5, TimeUnit.SECONDS);
        assertThat(secondResponse).contains("타이머 완료");

        webSocketSession.close();
    }
}
