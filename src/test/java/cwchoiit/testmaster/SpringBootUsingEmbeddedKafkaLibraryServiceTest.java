package cwchoiit.testmaster;

import cwchoiit.testmaster.entity.Book;
import cwchoiit.testmaster.repository.BookRepository;
import cwchoiit.testmaster.service.PushService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
@DirtiesContext
@EmbeddedKafka(topics = {"push.notification"}, ports = {19092}, partitions = 1)
public class SpringBootUsingEmbeddedKafkaLibraryServiceTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoSpyBean
    PushService pushService;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker; // 실행 문제 없음

    @Autowired
    BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.save(new Book("1234", "Spock", true));
        bookRepository.save(new Book("5678", "JUnit", false));
    }

    @Test
    @DisplayName("도서 이용 가능 여부 확인 - 도서가 있는 경우 대여 가능해야 한다.")
    void isAvailable() throws Exception {
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/library/books/1234/availability"));

        assertThat(resultActions.andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(resultActions.andReturn().getResponse().getContentAsString()).isEqualTo("1234 : 대출 가능");

        verify(pushService, times(0)).notification(any());
    }

    @Test
    @DisplayName("도서 이용 가능 여부 확인 - 도서가 없는 경우 대여 불가능해야 한다.")
    void isNotAvailable() throws Exception {
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/library/books/0000/availability"));

        assertThat(resultActions.andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(resultActions.andReturn().getResponse().getContentAsString()).isEqualTo("0000 : 대출 불가");

        verify(pushService, times(0)).notification(any());
    }

    @Test
    @DisplayName("도서 이용 가능 여부 확인 - 도서가 있지만, 대출 가능하지 않은 경우 대여 불가능해야 한다.")
    void isNotAvailable2() throws Exception {
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/library/books/5678/availability"));

        assertThat(resultActions.andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(resultActions.andReturn().getResponse().getContentAsString()).isEqualTo("5678 : 대출 불가");

        verify(pushService, times(0)).notification(any());
    }

    public static Stream<Arguments> borrowBookDataProvider() {
        return Stream.of(
                Arguments.of("1234", "1234 : Spock", "대출 완료: Spock")
        );
    }

    @ParameterizedTest(name = "isbn={0}, expected: {1}, expectedMessage: {2}")
    @MethodSource("borrowBookDataProvider")
    @DisplayName("대여 요청 시 도서 상태에 따른 처리 결과를 확인한다.")
    void borrowBook(String isbn, String expected, String expectedMessage) throws Exception {
        Map<String, Object> props = KafkaTestUtils.consumerProps("testGroup", "true", embeddedKafkaBroker);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (Consumer<Object, Object> consumer = new DefaultKafkaConsumerFactory<>(props).createConsumer()) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "push.notification");

            ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/library/books/" + isbn + "/borrow"));

            assertThat(resultActions.andReturn().getResponse().getStatus()).isEqualTo(200);
            assertThat(resultActions.andReturn().getResponse().getContentAsString()).isEqualTo(expected);

            ConsumerRecord<Object, Object> singleRecord = KafkaTestUtils.getSingleRecord(consumer, "push.notification", Duration.ofSeconds(1));

            assertThat(singleRecord.value()).isEqualTo(expectedMessage);
        }
    }
}
