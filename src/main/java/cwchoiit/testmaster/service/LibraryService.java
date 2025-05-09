package cwchoiit.testmaster.service;

import cwchoiit.testmaster.entity.Book;
import cwchoiit.testmaster.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LibraryService {
    private final BookRepository bookRepository;
    private final PushService pushService;

    public boolean isAvailable(String isbn) {
        return bookRepository.findBookByIsbn(isbn)
                .map(Book::isAvailable)
                .orElse(false);
    }

    public Optional<String> borrowBook(String isbn) {
        return bookRepository.findBookByIsbn(isbn)
                .filter(Book::isAvailable)
                .map(book -> {
                    pushService.notification("대출 완료: " + book.getTitle());
                    return book.getTitle();
                });
    }
}
