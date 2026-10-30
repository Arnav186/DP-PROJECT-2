import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Scanner;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Composition root: explicitly wires the MVC parts and owns the database lifetime. */
public final class Main {
    public static void main(String[] args) {
        Path database = Path.of(System.getProperty("smarthome.db", Path.of("lib", "smart_home.db").toString()));
        if (!Arrays.asList(args).contains("--console")) {
            openWindow(database);
            return;
        }
        try (DatabaseConnection db = new DatabaseConnection(database);
            Scanner input = new Scanner(System.in)) {
            UserRepository users = new UserRepository(db.getConnection(), new PasswordHasher());
            ConsoleView view = new ConsoleView(input, System.out);
            new SmartHomeController(users, view).run();
        } catch (ClassNotFoundException exception) {
            System.err.println("SQLite JDBC driver is missing. Run with lib/* on the classpath.");
            System.exit(1);
        } catch (SQLException exception) {
            System.err.println("Cannot access the database: " + exception.getMessage());
            System.exit(1);
        }
    }

    /** Running Main normally opens the Swing window. --console keeps the optional CLI. */
    private static void openWindow(Path path) {
        SwingUtilities.invokeLater(() -> {
            DatabaseConnection db = null;
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                UIManager.put("Button.font", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
                UIManager.put("Label.font", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
                db = new DatabaseConnection(path);
                UserRepository users = new UserRepository(db.getConnection(), new PasswordHasher());
                SwingHomeView view = new SwingHomeView();
                new SwingHomeController(users, view, db);
                view.setVisible(true);
            } catch (Exception exception) {
                if (db != null) {
                    try { db.close(); } catch (SQLException ignored) { }
                }
                JOptionPane.showMessageDialog(null,
                        "Cannot start Smart Home.\n" + exception.getMessage()
                                + "\nCompile the Java files and include lib/* on the classpath.",
                        "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
