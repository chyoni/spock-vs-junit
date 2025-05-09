package cwchoiit.testmaster

import cwchoiit.testmaster.entity.Book
import cwchoiit.testmaster.repository.BookRepository
import cwchoiit.testmaster.service.PushService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import spock.lang.Specification

@SpringBootTest
@AutoConfigureMockMvc
class SpringBootUsingControllerLibraryServiceSpec extends Specification {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
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
    }

    def "대출 요청 시 도서 상태에 따른 처리 결과를 확인한다."() {
        given:
        bookRepository.findBookByIsbn(_ as String) >> {
            bookExists ? Optional.of(new Book(isbn, title, available)) : Optional.empty()
        }

        when:
        def resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/library/books/${isbn}/borrow"))

        then:
        resultActions
                .andExpect(MockMvcResultMatchers.status().is(status))
                .andExpect(MockMvcResultMatchers.content().string(expected))

        where:
        bookExists | isbn   | title    | available | status | expected
        true       | "1234" | "title"  | true      | 200 | "1234 : title"
        true       | "5678" | "title2" | false     | 200 | "5678 : 대출 불가"
        false      | "9999" | "title3" | true      | 200 | "9999 : 대출 불가"
    }
}
