package cwchoiit.testmaster;

import cwchoiit.testmaster.entity.Book;
import cwchoiit.testmaster.repository.BookRepository;
import cwchoiit.testmaster.service.LibraryService;
import cwchoiit.testmaster.service.PushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
public class SpringBootUsingDbLibraryServiceTest {

    @MockitoBean
    PushService pushService;

    @Autowired
    LibraryService libraryService;

    @Autowired
    BookRepository bookRepository;

    @BeforeEach
    void setUp() {
        bookRepository.save(new Book("1234", "Spock", true));
        bookRepository.save(new Book("5678", "JUnit", true));
    }

    @Test
    @DisplayName("도서 이용 가능 여부 확인 - 도서가 없는 경우 대여 불가해야 한다.")
    void isAvailable() {
        boolean available = libraryService.isAvailable("0000");
        assertThat(available).isFalse();
    }

    @Test
    @DisplayName("도서 이용 가능 여부 확인 - 도서가 있는 경우 대여 가능해야 한다.")
    void isAvailable2() {
        boolean available = libraryService.isAvailable("1234");
        assertThat(available).isTrue();
    }

    public static Stream<Arguments> borrowBookDataProvider() {
        return Stream.of(
                Arguments.of("1234", Optional.of("Spock")),
                Arguments.of("5678", Optional.of("JUnit")),
                Arguments.of("0000", Optional.empty())
        );
    }

    @ParameterizedTest(name = "isbn={0}, expected: {1}")
    @MethodSource("borrowBookDataProvider")
    @DisplayName("대여 요청 시 도서 상태에 따른 처리 결과를 확인한다.")
    void borrowBook(String isbn, Optional<String> expected) {
        assertThat(libraryService.borrowBook(isbn)).isEqualTo(expected);
    }
}
