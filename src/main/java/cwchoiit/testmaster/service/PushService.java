package cwchoiit.testmaster.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PushService {
    private static final String TOPIC = "push.notification";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void notification(String message) {
        kafkaTemplate.send(TOPIC, message);
    }
}
