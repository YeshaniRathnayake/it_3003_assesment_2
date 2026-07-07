import java.util.ArrayList;
import java.time.temporal.ChronoUnit;
import java.time.LocalDate;

public class Library {

    ArrayList<Book> books = new ArrayList<>();
    ArrayList<Member> members = new ArrayList<>();
    ArrayList<BorrowRecord> borrowRecords = new ArrayList<>();


    public void addBook(String id, String title, String author, int copies) {

        for (Book b : books) {
            if (b.id.equals(id)) {
                System.out.println("Book ID already exists!");
                return;
            }
        }

        books.add(new Book(id, title, author, copies));
        System.out.println("Book added: " + title);
    }

    public void removeBook(String id) {
        for (Book b : books) {
            if (b.id.equals(id)) {
                books.remove(b);
                System.out.println("Book removed.");
                return;
            }
        }

        System.out.println("Book not found.");
    }

    public void searchBook(String keyword) {
        for (Book b : books) {
            if (b.title.toLowerCase().contains(keyword.toLowerCase())
                    || b.author.toLowerCase().contains(keyword.toLowerCase())) {

                System.out.println(b.id + " " + b.title + " " + b.author + " " + b.copies);
            }
        }
    }

    public void addMember(String id, String name) {

        for (Member m : members) {
            if (m.id.equals(id)) {
                System.out.println("Member already exists.");
                return;
            }
        }

        members.add(new Member(id, name));
        System.out.println("Member added: " + name);
    }

    public Book findBook(String id) {
        for (Book b : books) {
            if (b.id.equals(id))
                return b;
        }
        return null;
    }


    public Member findMember(String id) {
        for (Member m : members) {
            if (m.id.equals(id))
                return m;
        }
        return null;
    }

    public void borrowBook(String memberId, String bookId) {

        Member m = findMember(memberId);
        Book b = findBook(bookId);

        if (m == null) {
            System.out.println("No such member");
            return;
        }

        if (b == null) {
            System.out.println("No such book");
            return;
        }

        if (b.copies <= 0) {
            System.out.println("No copies available");
            return;
        }

        b.copies--;

        borrowRecords.add(new BorrowRecord(memberId, bookId));

        System.out.println(m.name + " borrowed " + b.title);
    }

    public void returnBook(String memberId, String bookId) {

        for (BorrowRecord r : borrowRecords) {

            if (r.memberId.equals(memberId)
                    && r.bookId.equals(bookId)
                    && r.returnDate == null) {

                r.returnDate = LocalDate.now();

                Book b = findBook(bookId);
                b.copies++;

                long days = ChronoUnit.DAYS.between(r.borrowDate, r.returnDate);

                int fine = 0;

                if (days > 14) {
                    fine = (int) (days - 14) * 10;
                }

                System.out.println("Returned. Fine = " + fine);
                return;
            }
        }

        System.out.println("No active borrow record found.");
    }

    public void listBooks() {

        for (Book b : books) {
            System.out.println(b.id + " " + b.title + " " + b.author + " " + b.copies);
        }
    }

    public void listMembers() {

        for (Member m : members) {
            System.out.println(m.id + " " + m.name);
        }
    }
}