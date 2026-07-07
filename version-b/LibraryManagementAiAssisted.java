import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

class LibraryException extends Exception {
    public LibraryException(String message) {
        super(message);
    }
}

class BookNotFoundException extends LibraryException {
    public BookNotFoundException(String message) { super(message); }
}

class MemberNotFoundException extends LibraryException {
    public MemberNotFoundException(String message) { super(message); }
}

class NoCopiesAvailableException extends LibraryException {
    public NoCopiesAvailableException(String message) { super(message); }
}

class DuplicateRecordException extends LibraryException {
    public DuplicateRecordException(String message) { super(message); }
}

class NoActiveLoanException extends LibraryException {
    public NoActiveLoanException(String message) { super(message); }
}

// --------------------------------------------------------------------------- //
//  Domain Models
// --------------------------------------------------------------------------- //
enum LoanStatus {
    ACTIVE,
    RETURNED
}

class Book {
    private final String bookId;
    private final String title;
    private final String author;
    private final int totalCopies;
    private int availableCopies;

    public Book(String bookId, String title, String author, int totalCopies) {
        if (totalCopies < 0) {
            throw new IllegalArgumentException("totalCopies cannot be negative");
        }
        this.bookId = bookId;
        this.title = title;
        this.author = author;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    public String getBookId() { return bookId; }
    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public int getTotalCopies() { return totalCopies; }
    public int getAvailableCopies() { return availableCopies; }

    public boolean isAvailable() { return availableCopies > 0; }
    public void decreaseCopy() { availableCopies--; }
    public void increaseCopy() { availableCopies++; }

    @Override
    public String toString() {
        return String.format("Book(id=%s, title=%s, author=%s, total=%d, available=%d)",
                bookId, title, author, totalCopies, availableCopies);
    }
}

class Member {
    private final String memberId;
    private final String name;

    public Member(String memberId, String name) {
        this.memberId = memberId;
        this.name = name;
    }

    public String getMemberId() { return memberId; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return String.format("Member(id=%s, name=%s)", memberId, name);
    }
}

class LoanRecord {
    private static final int FINE_PER_DAY = 10;
    private static final int LOAN_PERIOD_DAYS = 14;

    private final String memberId;
    private final String bookId;
    private final LocalDate borrowDate;
    private LocalDate returnDate;
    private LoanStatus status;

    public LoanRecord(String memberId, String bookId, LocalDate borrowDate) {
        this.memberId = memberId;
        this.bookId = bookId;
        this.borrowDate = borrowDate;
        this.status = LoanStatus.ACTIVE;
    }

    public String getMemberId() { return memberId; }
    public String getBookId() { return bookId; }
    public LocalDate getBorrowDate() { return borrowDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public LoanStatus getStatus() { return status; }
    public boolean isActive() { return status == LoanStatus.ACTIVE; }

    public void markReturned(LocalDate date) {
        this.returnDate = date;
        this.status = LoanStatus.RETURNED;
    }

    /** Returns the overdue fine (in currency units) for this loan. */
    public int calculateFine(LocalDate asOf) {
        LocalDate endDate = (returnDate != null) ? returnDate : (asOf != null ? asOf : LocalDate.now());
        long daysHeld = ChronoUnit.DAYS.between(borrowDate, endDate);
        long overdueDays = Math.max(0, daysHeld - LOAN_PERIOD_DAYS);
        return (int) (overdueDays * FINE_PER_DAY);
    }

    @Override
    public String toString() {
        return String.format("LoanRecord(member=%s, book=%s, borrow=%s, return=%s, status=%s)",
                memberId, bookId, borrowDate, returnDate, status);
    }
}

// --------------------------------------------------------------------------- //
//  Library Service (core business logic)
// --------------------------------------------------------------------------- //
class Library {
    private static final Logger logger = Logger.getLogger(Library.class.getName());

    private final Map<String, Book> books = new LinkedHashMap<>();
    private final Map<String, Member> members = new LinkedHashMap<>();
    private final List<LoanRecord> loans = new ArrayList<>();

    // ---- Book management -------------------------------------------------
    public Book addBook(String bookId, String title, String author, int copies) throws DuplicateRecordException {
        if (books.containsKey(bookId)) {
            throw new DuplicateRecordException("Book ID '" + bookId + "' already exists.");
        }
        Book book = new Book(bookId, title, author, copies);
        books.put(bookId, book);
        logger.info(() -> "Book added: " + title + " (" + bookId + ")");
        return book;
    }

    public void removeBook(String bookId) throws BookNotFoundException {
        if (!books.containsKey(bookId)) {
            throw new BookNotFoundException("Book ID '" + bookId + "' not found.");
        }
        books.remove(bookId);
        logger.info(() -> "Book removed: " + bookId);
    }

    public List<Book> searchBooks(String keyword) {
        String kw = keyword.toLowerCase();
        List<Book> result = new ArrayList<>();
        for (Book b : books.values()) {
            if (b.getTitle().toLowerCase().contains(kw) || b.getAuthor().toLowerCase().contains(kw)) {
                result.add(b);
            }
        }
        return result;
    }

    public Book getBook(String bookId) throws BookNotFoundException {
        Book book = books.get(bookId);
        if (book == null) {
            throw new BookNotFoundException("Book ID '" + bookId + "' not found.");
        }
        return book;
    }

    // ---- Member management -------------------------------------------------
    public Member addMember(String memberId, String name) throws DuplicateRecordException {
        if (members.containsKey(memberId)) {
            throw new DuplicateRecordException("Member ID '" + memberId + "' already exists.");
        }
        Member member = new Member(memberId, name);
        members.put(memberId, member);
        logger.info(() -> "Member added: " + name + " (" + memberId + ")");
        return member;
    }

    public Member getMember(String memberId) throws MemberNotFoundException {
        Member member = members.get(memberId);
        if (member == null) {
            throw new MemberNotFoundException("Member ID '" + memberId + "' not found.");
        }
        return member;
    }

    // ---- Borrowing / returning ---------------------------------------------
    public LoanRecord borrowBook(String memberId, String bookId)
            throws MemberNotFoundException, BookNotFoundException, NoCopiesAvailableException {
        Member member = getMember(memberId);
        Book book = getBook(bookId);

        if (!book.isAvailable()) {
            throw new NoCopiesAvailableException("No available copies of '" + book.getTitle() + "'.");
        }

        book.decreaseCopy();
        LoanRecord record = new LoanRecord(member.getMemberId(), book.getBookId(), LocalDate.now());
        loans.add(record);
        logger.info(() -> member.getName() + " borrowed '" + book.getTitle() + "'");
        return record;
    }

    /** Returns a book and gives back the fine amount owed (0 if on time). */
    public int returnBook(String memberId, String bookId) throws NoActiveLoanException, BookNotFoundException {
        for (LoanRecord record : loans) {
            if (record.getMemberId().equals(memberId)
                    && record.getBookId().equals(bookId)
                    && record.isActive()) {
                record.markReturned(LocalDate.now());
                Book book = getBook(bookId);
                book.increaseCopy();
                int fine = record.calculateFine(null);
                final int fFine = fine;
                logger.info(() -> "Book '" + bookId + "' returned by member " + memberId + ". Fine: " + fFine);
                return fine;
            }
        }
        throw new NoActiveLoanException(
                "No active loan found for member '" + memberId + "' and book '" + bookId + "'.");
    }

    // ---- Reporting -------------------------------------------------------
    public List<Book> listBooks() { return new ArrayList<>(books.values()); }

    public List<Member> listMembers() { return new ArrayList<>(members.values()); }

    public List<LoanRecord> activeLoans() {
        List<LoanRecord> active = new ArrayList<>();
        for (LoanRecord r : loans) {
            if (r.isActive()) active.add(r);
        }
        return active;
    }

    // ---- Persistence (AI-suggested addition) ------------------------------
    /** Writes the current library state to a JSON file (no external libraries required). */
    public void saveToJson(Path path) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        sb.append("  \"books\": [\n");
        int i = 0;
        for (Book b : books.values()) {
            sb.append("    {\"book_id\": \"").append(b.getBookId())
                    .append("\", \"title\": \"").append(escape(b.getTitle()))
                    .append("\", \"author\": \"").append(escape(b.getAuthor()))
                    .append("\", \"total_copies\": ").append(b.getTotalCopies())
                    .append(", \"available_copies\": ").append(b.getAvailableCopies())
                    .append("}");
            sb.append(++i < books.size() ? ",\n" : "\n");
        }
        sb.append("  ],\n");

        sb.append("  \"members\": [\n");
        i = 0;
        for (Member m : members.values()) {
            sb.append("    {\"member_id\": \"").append(m.getMemberId())
                    .append("\", \"name\": \"").append(escape(m.getName()))
                    .append("\"}");
            sb.append(++i < members.size() ? ",\n" : "\n");
        }
        sb.append("  ],\n");

        sb.append("  \"loans\": [\n");
        for (i = 0; i < loans.size(); i++) {
            LoanRecord r = loans.get(i);
            sb.append("    {\"member_id\": \"").append(r.getMemberId())
                    .append("\", \"book_id\": \"").append(r.getBookId())
                    .append("\", \"borrow_date\": \"").append(r.getBorrowDate())
                    .append("\", \"return_date\": ").append(r.getReturnDate() == null ? "null" : "\"" + r.getReturnDate() + "\"")
                    .append(", \"status\": \"").append(r.getStatus())
                    .append("\"}");
            sb.append(i + 1 < loans.size() ? ",\n" : "\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(sb.toString());
        }
        logger.info(() -> "Library state saved to " + path);
    }

    private static String escape(String s) {
        return s.replace("\"", "\\\"");
    }
}


public class LibraryManagementAiAssisted {
    private static final Logger logger = Logger.getLogger(LibraryManagementAiAssisted.class.getName());

    public static void main(String[] args) {
        Library library = new Library();
        Scanner scanner = new Scanner(System.in);

        String menuText = "1.Add Book | 2.Remove Book | 3.Search | 4.Add Member | "
                + "5.Borrow | 6.Return | 7.List Books | 8.List Members | 9.Exit";

        while (true) {
            System.out.println("\n" + menuText);
            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1": {
                        System.out.print("Book ID: "); String bid = scanner.nextLine();
                        System.out.print("Title: "); String title = scanner.nextLine();
                        System.out.print("Author: "); String author = scanner.nextLine();
                        System.out.print("Copies: "); int copies = Integer.parseInt(scanner.nextLine());
                        library.addBook(bid, title, author, copies);
                        break;
                    }
                    case "2": {
                        System.out.print("Book ID: "); String bid = scanner.nextLine();
                        library.removeBook(bid);
                        break;
                    }
                    case "3": {
                        System.out.print("Keyword: "); String kw = scanner.nextLine();
                        for (Book b : library.searchBooks(kw)) System.out.println(b);
                        break;
                    }
                    case "4": {
                        System.out.print("Member ID: "); String mid = scanner.nextLine();
                        System.out.print("Name: "); String name = scanner.nextLine();
                        library.addMember(mid, name);
                        break;
                    }
                    case "5": {
                        System.out.print("Member ID: "); String mid = scanner.nextLine();
                        System.out.print("Book ID: "); String bid = scanner.nextLine();
                        library.borrowBook(mid, bid);
                        break;
                    }
                    case "6": {
                        System.out.print("Member ID: "); String mid = scanner.nextLine();
                        System.out.print("Book ID: "); String bid = scanner.nextLine();
                        int fine = library.returnBook(mid, bid);
                        System.out.println("Returned. Fine due: " + fine);
                        break;
                    }
                    case "7":
                        for (Book b : library.listBooks()) System.out.println(b);
                        break;
                    case "8":
                        for (Member m : library.listMembers()) System.out.println(m);
                        break;
                    case "9":
                        scanner.close();
                        return;
                    default:
                        System.out.println("Invalid choice, please try again.");
                }
            } catch (LibraryException exc) {
                logger.log(Level.WARNING, "Operation failed: {0}", exc.getMessage());
                System.out.println("Error: " + exc.getMessage());
            } catch (NumberFormatException exc) {
                System.out.println("Error: please enter a valid number for copies.");
            }
        }
    }
}