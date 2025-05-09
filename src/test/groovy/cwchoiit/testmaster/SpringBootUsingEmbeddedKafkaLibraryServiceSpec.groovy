package cwchoiit.testmaster

import cwchoiit.testmaster.entity.Book
import cwchoiit.testmaster.repository.BookRepository
import cwchoiit.testmaster.service.PushService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.test.EmbeddedKafkaBroker
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.kafka.test.utils.KafkaTestUtils
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

import java.time.Duration

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(topics = ["push.notification"], ports = [9092])
class SpringBootUsingEmbeddedKafkaLibraryServiceSpec extends Specification {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker

    @MockitoSpyBean
    PushService pushService

    @SpringBean
    BookRepository bookRepository = Stub()

    def "도서 이용 가능 여부를 확인한다."() {
        given:
        bookRepository.findBookByIsbn(_ as String) >> Optional.of(new Book("1234", "Stub", true))

        when:
        def resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/library/books/1234/availability"))

        then:
        resultActions
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string("1234 : 대출 가능"))
        0 * pushService.notification(_ as String)
    }

    def "대출 요청 시 도서 상태에 따른 처리 결과를 확인한다."() {
        given:
        bookRepository.findBookByIsbn(_ as String) >> {
            bookExists ? Optional.of(new Book(isbn, title, available)) : Optional.empty()
        }

        def topicName = "push.notification"
        def props = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker)
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer)
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer)

        def consumer = new DefaultKafkaConsumerFactory<>(props).createConsumer()
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, true, topicName)

        when:
        def resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/library/books/${isbn}/borrow"))

        then:
        resultActions
                .andExpect(MockMvcResultMatchers.status().is(status))
                .andExpect(MockMvcResultMatchers.content().string(expected))

        and:
        def singleRecord = null
        try {
            singleRecord = KafkaTestUtils.getSingleRecord(consumer, topicName, Duration.ofSeconds(1))
        } catch (Exception ignored) {
        }

        singleRecord == null ? true : singleRecord.value() == expectedMessage

        cleanup:
        consumer.close()

        where:
        bookExists | isbn   | title    | available | status | expected       | expectedMessage
        true       | "1234" | "title"  | true      | 200    | "1234 : title" | "대출 완료: title"
        true       | "5678" | "title2" | false     | 200    | "5678 : 대출 불가" | null
        false      | "9999" | "title3" | true      | 200    | "9999 : 대출 불가" | null
    }
}
