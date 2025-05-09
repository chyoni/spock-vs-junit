package cwchoiit.testmaster

import cwchoiit.testmaster.entity.Book
import cwchoiit.testmaster.repository.BookRepository
import cwchoiit.testmaster.service.LibraryService
import cwchoiit.testmaster.service.PushService
import org.spockframework.spring.EnableSharedInjection
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional
import spock.lang.Shared
import spock.lang.Specification

@Transactional
@SpringBootTest
@EnableSharedInjection
class SpringBootUsingDbLibraryServiceSpec extends Specification {

    @Shared
    @Autowired
    BookRepository bookRepository
    @MockitoBean
    PushService pushService


    def setupSpec() {
        bookRepository.save(new Book("1234", "Spock", true))
        bookRepository.save(new Book("5678", "JUnit", true))
        bookRepository.flush()
    }

    def "도서 이용 가능 여부를 확인한다."() {
        given:

        def libraryService = new LibraryService(bookRepository, pushService)

        when:
        def available = libraryService.isAvailable("1234")

        then:
        available
    }

    def "대여 요청 시 도서 상태에 따른 처리 결과를 확인한다."() {
        given:

        def libraryService = new LibraryService(bookRepository, pushService)

        when:
        def book = libraryService.borrowBook(isbn)

        then:
        expected == book

        where:
        isbn   | expected
        "1234" | Optional.of("Spock")
        "5678" | Optional.of("JUnit")
        "9999" | Optional.empty()
    }
}
