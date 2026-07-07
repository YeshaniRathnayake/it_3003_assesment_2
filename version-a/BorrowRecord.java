import java.time.LocalDate;

public class BorrowRecord {
    String memberId;
    String bookId;
    LocalDate borrowDate;
    LocalDate returnDate;

    public BorrowRecord(String memberId, String bookId) {
        this.memberId = memberId;
        this.bookId = bookId;
        borrowDate = LocalDate.now();
        returnDate = null;
    }
}