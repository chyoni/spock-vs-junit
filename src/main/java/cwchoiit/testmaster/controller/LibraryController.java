package cwchoiit.testmaster.controller;

import cwchoiit.testmaster.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/library")
public class LibraryController {
    private final LibraryService libraryService;

    @GetMapping("/books/{isbn}/availability")
    public ResponseEntity<String> isBookAvailable(@PathVariable("isbn") String isbn) {
        return ResponseEntity.ok(
                libraryService.isAvailable(isbn) ?
                        "%s : %s".formatted(isbn, "대출 가능") :
                        "%s : %s".formatted(isbn, "대출 불가")
        );
    }

    @PostMapping("/books/{isbn}/borrow")
    public ResponseEntity<String> borrowBook(@PathVariable("isbn") String isbn) {
        String borrowBook = libraryService.borrowBook(isbn)
                .map(title -> "%s : %s".formatted(isbn, title))
                .orElseGet(() -> "%s : %s".formatted(isbn, "대출 불가"));
        return ResponseEntity.ok(borrowBook);
    }
}
