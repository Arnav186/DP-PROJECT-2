import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;

/** Tests the project's own Swing components with synthetic accounts in a temporary DB. */
public final class SwingHomeTests {
    private static SwingHomeView view;
    private static SwingHomeController controller;
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path folder = Files.createTempDirectory("smart-home-swing-tests-");
        Path database = folder.resolve("test.db");
        try (DatabaseConnection db = new DatabaseConnection(database)) {
            UserRepository users = new UserRepository(db.getConnection(), new PasswordHasher());
            users.register("other-user", "test-pass");
            onEdt(() -> {
                view = new SwingHomeView();
                controller = new SwingHomeController(users, view, db);
                check(view.getTitle().equals("Smart Home System"), "Window title");
                check(view.getRootPane().getDefaultButton() == view.loginButton, "Sign-in is the default action");
                check(!view.onButton.isEnabled(), "No-device actions disabled");
                view.loginButton.doClick();
                check(view.authenticationMessage().contains("Enter your username"), "Empty login validation");
                view.registerUsername.setText("swing-test");
                view.registerPassword.setText("test-pass");
                view.confirmPassword.setText("mismatch");
                view.registerButton.doClick();
                check(view.authenticationMessage().contains("do not match"), "Registration confirmation validation");
                view.confirmPassword.setText("test-pass");
                view.registerButton.doClick();
                check(!view.loginButton.isEnabled(), "Authentication disables duplicate requests");
            });
            awaitAuthentication();
            onEdt(() -> {
                check(view.authenticationMessage().contains("Account created"), "Swing registration succeeds");
                check(view.registerPassword.getPassword().length == 0, "Registration password cleared");
                view.loginUsername.setText("swing-test");
                view.loginPassword.setText("incorrect");
                view.loginButton.doClick();
            });
            awaitAuthentication();
            onEdt(() -> {
                check(view.authenticationMessage().contains("House-owner alert"), "Failed Swing login alerts owner");
                check(view.getRootPane().getDefaultButton() == view.loginButton, "Failed login stays on authentication");
                view.loginPassword.setText("test-pass");
                view.loginButton.doClick();
            });
            awaitAuthentication();
            onEdt(() -> {
                check(view.getRootPane().getDefaultButton() == null, "Successful login opens dashboard");
                check(view.loginPassword.getPassword().length == 0, "Login password cleared");
                controller.addDevice(new SwingHomeView.DeviceForm(DeviceType.LIGHT, "Study Lamp", "Study", "WiFi"));
                controller.addDevice(new SwingHomeView.DeviceForm(DeviceType.FAN, "Ceiling Fan", "Living Room", "Bluetooth"));
                check(view.deviceTable.getRowCount() == 2, "Builder form updates device table");
                check(view.onButton.isEnabled(), "Actions enabled after device creation");
                view.deviceTable.getRowSorter().toggleSortOrder(0);
                view.deviceTable.setRowSelectionInterval(0, 0);
                view.onButton.doClick();
                check(view.deviceTable.getValueAt(0, 0).equals("Ceiling Fan"), "Table sorts by visible device name");
                check(view.deviceTable.getValueAt(0, 4).equals("ON")
                        && view.deviceTable.getValueAt(1, 4).equals("OFF"), "Sorted selection controls the right device");
                check(view.statusMessage().contains("Ceiling Fan turned ON"), "Action feedback updates");
                view.saveButton.doClick();
                check(view.statusMessage().contains("snapshot saved"), "Snapshot button is wired");
                view.offButton.doClick();
                check(view.deviceTable.getValueAt(0, 4).equals("OFF"), "OFF button is wired");
                view.undoButton.doClick();
                check(view.deviceTable.getValueAt(0, 4).equals("ON"), "Undo button restores power");
                view.securityType.setSelectedIndex(2);
                view.securityLocation.setText("Entrance");
                view.securityButton.doClick();
                check(view.statusMessage().contains("Security event: Alarm at Entrance"), "Security controls route the selected event");
                view.securityLocation.setText(" ");
                view.securityButton.doClick();
                check(view.statusMessage().contains("location is required"), "Invalid security input shown inline");
                view.logoutButton.doClick();
                check(view.deviceTable.getRowCount() == 0 && view.selectedDevice() == null, "Logout clears private view state");
                controller.addDevice(new SwingHomeView.DeviceForm(DeviceType.FAN, "Forbidden", "Room", "WiFi"));
                check(view.deviceTable.getRowCount() == 0 && view.authenticationMessage().contains("Sign in"), "No mutation after logout");
                view.loginUsername.setText("other-user");
                view.loginPassword.setText("test-pass");
                view.loginButton.doClick();
            });
            awaitAuthentication();
            onEdt(() -> {
                check(view.deviceTable.getRowCount() == 0, "Another account has a separate home");
                view.undoButton.doClick();
                check(view.statusMessage().contains("No device changes"), "Another account has separate history");
                view.logoutButton.doClick();
                view.loginUsername.setText("swing-test");
                view.loginPassword.setText("test-pass");
                view.loginButton.doClick();
            });
            awaitAuthentication();
            onEdt(() -> {
                check(view.deviceTable.getRowCount() == 2, "Returning user keeps their home");
                view.securityLocation.setText("Entrance");
            });
            System.out.println("PASS: " + checks + " Swing checks (forms, actions, selection, history, and account isolation).");
            if (Arrays.asList(args).contains("--preview")) {
                CountDownLatch closed = new CountDownLatch(1);
                onEdt(() -> {
                    view.setTitle("Smart Home - GUI test preview");
                    view.addWindowListener(new WindowAdapter() {
                        public void windowClosed(WindowEvent event) { closed.countDown(); }
                    });
                    view.setVisible(true);
                });
                closed.await();
            }
        } finally {
            if (view != null) { onEdt(() -> view.dispose()); }
            Files.deleteIfExists(database);
            Files.deleteIfExists(folder);
        }
    }

    private static void awaitAuthentication() throws Exception {
        long deadline = System.nanoTime() + 10_000_000_000L;
        while (System.nanoTime() < deadline) {
            AtomicBoolean ready = new AtomicBoolean();
            onEdt(() -> ready.set(view.loginButton.isEnabled()));
            if (ready.get()) { return; }
            Thread.sleep(25);
        }
        throw new AssertionError("Timed out waiting for authentication.");
    }

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static void check(boolean condition, String description) {
        if (!condition) { throw new AssertionError(description); }
        checks++;
    }
}
