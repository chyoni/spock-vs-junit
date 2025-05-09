package cwchoiit.testmaster

import cwchoiit.testmaster.entity.Book
import cwchoiit.testmaster.repository.BookRepository
import cwchoiit.testmaster.service.LibraryService
import cwchoiit.testmaster.service.PushService
import spock.lang.Specification

class LibraryServiceSpec extends Specification {

    def "도서 이용 가능 여부를 확인한다."() {
        given:
        BookRepository bookRepository = Stub()
        PushService pushService = Stub()
        bookRepository.findBookByIsbn("1234") >> Optional.of(new Book("1234", "Title", true))

        def libraryService = new LibraryService(bookRepository, pushService)

        when:
        def available = libraryService.isAvailable("1234")

        then:
        available
    }

    def "대여 요청 시 도서 상태에 따른 처리 결과를 확인한다."() {
        given:
        BookRepository bookRepository = Stub()
        PushService pushService = Stub()
        bookRepository.findBookByIsbn("1234") >> {
            bookExists ? Optional.of(new Book(isbn, title, available)) : Optional.empty()
        }

        def libraryService = new LibraryService(bookRepository, pushService)

        when:
        def book = libraryService.borrowBook(isbn)

        then:
        expected == book

        where:
        bookExists | isbn   | title    | available | expected
        true       | "1234" | "title"  | true      | Optional.of("title")
        true       | "5678" | "title2" | false     | Optional.empty()
        false      | "9999" | "title3" | true      | Optional.empty()
    }

    def "대출에 성공하면 알림이 발송되어야 한다."() {
        given:
        BookRepository bookRepository = Stub()
        PushService pushService = Mock()
        bookRepository.findBookByIsbn("1234") >> Optional.of(new Book("1234", "Title", true))
        def libraryService = new LibraryService(bookRepository, pushService)

        when:
        def book = libraryService.borrowBook("1234")

        then:
        Optional.of("Title") == book
        1 * pushService.notification("대출 완료: " + "Title")
    }

    def "도서 조회 중 예외가 발생하면 대출 요청 시 예외를 던진다."() {
        given:
        BookRepository bookRepository = Stub()
        PushService pushService = Mock()
        bookRepository.findBookByIsbn("1234") >> {
            throw new RuntimeException("Database error")
        }
        def libraryService = new LibraryService(bookRepository, pushService)

        when:
        libraryService.borrowBook("1234")

        then:
        thrown(RuntimeException)
    }

    def "Spy 테스트"() {
        given:
        BookRepository bookRepository = Stub()
        PushService pushService = Mock()

        bookRepository.findBookByIsbn("1234") >> Optional.of(new Book("1234", "Title", true))

        LibraryService libraryService = Spy(constructorArgs: [bookRepository, pushService]) {
            borrowBook(_ as String) >> Optional.of("Override Spy")
        }

        when:
        def book = libraryService.borrowBook("1234")
        def available = libraryService.isAvailable("1234")

        then:
        available
        Optional.of("Override Spy") == book
    }
}
