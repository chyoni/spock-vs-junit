package cwchoiit.testmaster;

import cwchoiit.testmaster.entity.Book;
import cwchoiit.testmaster.repository.BookRepository;
import cwchoiit.testmaster.service.LibraryService;
import cwchoiit.testmaster.service.PushService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
class SpringBootLibraryServiceTest {

    @MockitoBean
    BookRepository bookRepository;

    @MockitoBean
    PushService pushService;

    @Autowired
    LibraryService libraryService;

    @Test
    @DisplayName("도서 이용 가능 여부 확인")
    void isAvailable() {
        when(bookRepository.findBookByIsbn(eq("1234")))
                .thenReturn(Optional.of(new Book("1234", "title", true)));

        boolean available = libraryService.isAvailable("1234");
        assertThat(available).isTrue();
    }

    public static Stream<Arguments> borrowBookDataProvider() {
        return Stream.of(
                Arguments.of(true, "1234", "title", true, Optional.of("title")),
                Arguments.of(true, "5678", "title2", false, Optional.empty()),
                Arguments.of(false, "9999", "title3", true, Optional.empty())
        );
    }

    @ParameterizedTest(name = "bookExists={0}, isbn={1}, title={2}, available={3} => expected: {4}")
    @MethodSource("borrowBookDataProvider")
    @DisplayName("대여 요청 시 도서 상태에 따른 처리 결과를 확인한다.")
    void borrowBook(boolean bookExists, String isbn, String title, boolean available, Optional<String> expected) {
        when(bookRepository.findBookByIsbn(any()))
                .thenReturn(bookExists ? Optional.of(new Book(isbn, title, available)) : Optional.empty());

        assertThat(libraryService.borrowBook(isbn)).isEqualTo(expected);
    }

    @Test
    @DisplayName("대출에 성공하면 알림이 발송되어야 한다.")
    void borrowBookPushNotification() {
        when(bookRepository.findBookByIsbn(eq("1234")))
                .thenReturn(Optional.of(new Book("1234", "title", true)));

        assertThat(libraryService.borrowBook("1234")).isEqualTo(Optional.of("title"));

        verify(pushService, times(1)).notification(eq("대출 완료: " + "title"));
    }

    @Test
    @DisplayName("도서 조회 중 예외가 발생하면 대출 요청 시 예외를 던진다.")
    void borrowBookException() {
        when(bookRepository.findBookByIsbn(eq("1234")))
                .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> libraryService.borrowBook("1234"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }

    @Test
    @DisplayName("Spy 테스트")
    void borrowBookUsingSpy() {
        LibraryService libraryService = spy(new LibraryService(bookRepository, pushService));

        doReturn(Optional.of("Override spy")).when(libraryService).borrowBook("1234");

        when(bookRepository.findBookByIsbn(eq("1234")))
                .thenReturn(Optional.of(new Book("1234", "title", true)));

        Optional<String> book = libraryService.borrowBook("1234");
        boolean available = libraryService.isAvailable("1234");

        assertThat(book).isEqualTo(Optional.of("Override spy"));
        assertThat(available).isTrue();
    }
}