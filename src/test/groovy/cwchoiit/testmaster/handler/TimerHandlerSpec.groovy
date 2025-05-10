package cwchoiit.testmaster.handler

import cwchoiit.testmaster.TestMasterApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.client.standard.StandardWebSocketClient
import org.springframework.web.socket.handler.TextWebSocketHandler
import spock.lang.Specification

import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit

@SpringBootTest(
        classes = TestMasterApplication,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class TimerHandlerSpec extends Specification {

    @LocalServerPort
    int port;

    def "Timer 동작 테스트"() {
        given:
        def url = "ws://localhost:${port}/ws/timer"

        StandardWebSocketClient client = new StandardWebSocketClient()
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(1)
        WebSocketSession webSocketSession = client.execute(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                queue.put(message.payload)
            }
        }, url).get()

        when:
        webSocketSession.sendMessage(new TextMessage("2"))

        then:
        queue.poll(1, TimeUnit.SECONDS).contains("등록 완료")

        and:
        queue.poll(5, TimeUnit.SECONDS).contains("타이머 완료")

        cleanup:
        webSocketSession.close()
    }

}
