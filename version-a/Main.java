import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        Library library = new Library();

        while (true) {

            System.out.println("\n1.Add Book");
            System.out.println("2.Remove Book");
            System.out.println("3.Search");
            System.out.println("4.Add Member");
            System.out.println("5.Borrow");
            System.out.println("6.Return");
            System.out.println("7.List Books");
            System.out.println("8.List Members");
            System.out.println("9.Exit");

            System.out.print("Enter choice: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    System.out.print("Book ID: ");
                    String bid = sc.nextLine();

                    System.out.print("Title: ");
                    String title = sc.nextLine();

                    System.out.print("Author: ");
                    String author = sc.nextLine();

                    System.out.print("Copies: ");
                    int copies = sc.nextInt();
                    sc.nextLine();

                    library.addBook(bid, title, author, copies);
                    break;

                case 2:
                    System.out.print("Book ID: ");
                    library.removeBook(sc.nextLine());
                    break;

                case 3:
                    System.out.print("Keyword: ");
                    library.searchBook(sc.nextLine());
                    break;

                case 4:
                    System.out.print("Member ID: ");
                    String mid = sc.nextLine();

                    System.out.print("Name: ");
                    String name = sc.nextLine();

                    library.addMember(mid, name);
                    break;

                case 5:
                    System.out.print("Member ID: ");
                    String m1 = sc.nextLine();

                    System.out.print("Book ID: ");
                    String b1 = sc.nextLine();

                    library.borrowBook(m1, b1);
                    break;

                case 6:
                    System.out.print("Member ID: ");
                    String m2 = sc.nextLine();

                    System.out.print("Book ID: ");
                    String b2 = sc.nextLine();

                    library.returnBook(m2, b2);
                    break;

                case 7:
                    library.listBooks();
                    break;

                case 8:
                    library.listMembers();
                    break;

                case 9:
                    System.out.println("Goodbye!");
                    return;

                default:
                    System.out.println("Invalid choice");
            }
        }
    }
}