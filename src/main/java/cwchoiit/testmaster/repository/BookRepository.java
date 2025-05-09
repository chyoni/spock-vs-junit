package cwchoiit.testmaster.repository;

import cwchoiit.testmaster.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {
    Optional<Book> findBookByIsbn(@NonNull String isbn);
}
