package cwchoiit.testmaster;

import cwchoiit.testmaster.entity.Book;
import cwchoiit.testmaster.repository.BookRepository;
import cwchoiit.testmaster.service.PushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
public class SpringBootUsingControllerLibraryServiceTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PushService pushService;

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
    }

    @Test
    @DisplayName("도서 이용 가능 여부 확인 - 도서가 없는 경우 대여 불가능해야 한다.")
    void isNotAvailable() throws Exception {
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/library/books/0000/availability"));

        assertThat(resultActions.andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(resultActions.andReturn().getResponse().getContentAsString()).isEqualTo("0000 : 대출 불가");
    }

    @Test
    @DisplayName("도서 이용 가능 여부 확인 - 도서가 있지만, 대출 가능하지 않은 경우 대여 불가능해야 한다.")
    void isNotAvailable2() throws Exception {
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/library/books/5678/availability"));

        assertThat(resultActions.andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(resultActions.andReturn().getResponse().getContentAsString()).isEqualTo("5678 : 대출 불가");
    }

    public static Stream<Arguments> borrowBookDataProvider() {
        return Stream.of(
                Arguments.of("1234", "1234 : Spock"),
                Arguments.of("5678", "5678 : 대출 불가"),
                Arguments.of("0000", "0000 : 대출 불가")
        );
    }

    @ParameterizedTest(name = "isbn={0}, expected: {1}")
    @MethodSource("borrowBookDataProvider")
    @DisplayName("대여 요청 시 도서 상태에 따른 처리 결과를 확인한다.")
    void borrowBook(String isbn, String expected) throws Exception {
        ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/library/books/" + isbn + "/borrow"));

        assertThat(resultActions.andReturn().getResponse().getStatus()).isEqualTo(200);
        assertThat(resultActions.andReturn().getResponse().getContentAsString()).isEqualTo(expected);
    }
}
