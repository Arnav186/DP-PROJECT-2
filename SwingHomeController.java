import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.swing.SwingWorker;

/** MVC controller for the window: all device operations still go through Mediator. */
public final class SwingHomeController {
    private final UserRepository users;
    private final SwingHomeView view;
    private final DatabaseConnection database;
    private final Map<Long, Session> sessions = new HashMap<>();
    private Session current;
    private boolean authenticating;

    public SwingHomeController(UserRepository users, SwingHomeView view, DatabaseConnection database) {
        this.users = users;
        this.view = view;
        this.database = database;
        view.loginButton.addActionListener(event -> authenticate(false));
        view.registerButton.addActionListener(event -> authenticate(true));
        view.logoutButton.addActionListener(event -> logout());
        view.addButton.addActionListener(event -> createDevice(false));
        view.presetButton.addActionListener(event -> createDevice(true));
        view.cloneButton.addActionListener(event -> cloneDevice());
        view.onButton.addActionListener(event -> power(true));
        view.offButton.addActionListener(event -> power(false));
        view.connectionButton.addActionListener(event -> changeConnection());
        view.securityButton.addActionListener(event -> securityEvent());
        view.saveButton.addActionListener(event -> execute(() -> current.mediator().saveSnapshot()));
        view.restoreButton.addActionListener(event -> restore());
        view.undoButton.addActionListener(event -> execute(() -> {
            if (!current.mediator().undo()) { throw new IllegalArgumentException("No device changes to undo."); }
        }));
        view.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) { close(); }
        });
    }

    private void authenticate(boolean registering) {
        if (authenticating) { return; }
        String username = (registering ? view.registerUsername : view.loginUsername).getText().trim();
        char[] entered = (registering ? view.registerPassword : view.loginPassword).getPassword();
        char[] confirmation = view.confirmPassword.getPassword();
        if (username.isEmpty() || entered.length == 0) {
            view.setAuthenticationStatus("Enter your username and password.", true);
            Arrays.fill(entered, '\0');
            Arrays.fill(confirmation, '\0');
            return;
        }
        if (registering && !Arrays.equals(entered, confirmation)) {
            view.setAuthenticationStatus("Passwords do not match.", true);
            Arrays.fill(entered, '\0');
            Arrays.fill(confirmation, '\0');
            return;
        }
        String password = new String(entered);
        Arrays.fill(entered, '\0');
        Arrays.fill(confirmation, '\0');
        authenticating = true;
        view.setAuthenticationBusy(true);
        view.setAuthenticationStatus(registering ? "Creating your account..." : "Signing in...", false);
        // Hashing and SQLite access run off the Swing event thread to keep the window responsive.
        new SwingWorker<AuthenticationResult, Void>() {
            protected AuthenticationResult doInBackground() throws SQLException {
                if (registering) { return new AuthenticationResult(users.register(username, password), null); }
                Optional<UserAccount> user = users.authenticate(username, password);
                return new AuthenticationResult(user.isPresent(), user.orElse(null));
            }

            protected void done() {
                authenticating = false;
                view.setAuthenticationBusy(false);
                try {
                    AuthenticationResult result = get();
                    if (registering) {
                        if (result.success()) { view.registrationComplete(username); }
                        else { view.setAuthenticationStatus("Username already exists. Choose another.", true); }
                    } else if (result.success()) {
                        openHome(result.user());
                    } else {
                        view.loginPassword.setText("");
                        view.setAuthenticationStatus("Invalid username or password. House-owner alert simulated.", true);
                    }
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    view.setAuthenticationStatus("Sign-in was interrupted. Please try again.", true);
                } catch (ExecutionException exception) {
                    Throwable cause = exception.getCause();
                    view.setAuthenticationStatus(cause instanceof IllegalArgumentException
                            ? cause.getMessage() : "Cannot access the account database. Please try again.", true);
                }
            }
        }.execute();
    }

    private void openHome(UserAccount user) {
        current = sessions.get(user.id());
        if (current == null) {
            HomeModel model = new HomeModel(user);
            current = new Session(model, new SmartHomeMediator(model, new HomeHistory(), new NotificationCenter()));
            sessions.put(user.id(), current);
        }
        view.showHome(current.model());
    }

    private void logout() {
        current = null;
        view.showAuthentication();
        view.setAuthenticationStatus("You have been logged out.", false);
    }

    private void createDevice(boolean preset) {
        if (current == null) { return; }
        SwingHomeView.DeviceForm data = view.requestDevice(preset);
        if (data == null) { return; }
        addDevice(data);
    }

    // Also usable by local integration tests without opening a modal form.
    void addDevice(SwingHomeView.DeviceForm data) {
        execute(() -> current.mediator().addDevice(new Device.Builder(data.type(), data.name())
                .room(data.room()).communication(communication(data.connection())).initiallyOn(false).build()));
    }

    private void cloneDevice() {
        if (current == null) { return; }
        UUID id = view.selectedDevice();
        if (id == null) { return; }
        String name = view.requestCloneName(current.model().getDevice(id).getName());
        if (name != null) { execute(() -> current.mediator().cloneDevice(id, name)); }
    }

    private void power(boolean on) {
        UUID id = view.selectedDevice();
        if (id != null) { execute(() -> current.mediator().setPower(id, on)); }
    }

    private void changeConnection() {
        if (current == null) { return; }
        UUID id = view.selectedDevice();
        if (id == null) { return; }
        String connection = view.requestConnection(current.model().getDevice(id).getCommunicationName());
        if (connection != null) {
            execute(() -> current.mediator().changeCommunication(id, communication(connection)));
        }
    }

    private void securityEvent() {
        SecurityRoutine routine = switch (view.securityType.getSelectedIndex()) {
            case 0 -> new MotionRoutine();
            case 1 -> new DoorRoutine();
            case 2 -> new AlarmRoutine();
            default -> throw new IllegalStateException("Select a security event.");
        };
        execute(() -> current.mediator().securityEvent(routine, view.securityLocation.getText()));
    }

    private void restore() {
        if (current == null || !view.confirmRestore()) { return; }
        execute(() -> {
            if (!current.mediator().restoreSnapshot()) {
                throw new IllegalArgumentException("No saved snapshot. Save your home first.");
            }
        });
    }

    private void execute(Runnable action) {
        if (current == null) {
            view.setAuthenticationStatus("Sign in to manage your home.", true);
            return;
        }
        try {
            action.run();
            view.render(current.model());
            List<String> activity = current.model().getActivity();
            String last = activity.isEmpty() ? "Ready." : activity.get(activity.size() - 1);
            view.showStatus(last.replaceFirst("^\\[(Mobile App|Admin)\\] ", ""), false);
        } catch (IllegalArgumentException exception) {
            view.showStatus(exception.getMessage(), true);
        }
    }

    private CommunicationStrategy communication(String name) {
        return switch (name) {
            case "WiFi" -> new WiFiStrategy();
            case "Bluetooth" -> new BluetoothStrategy();
            default -> throw new IllegalArgumentException("Choose WiFi or Bluetooth.");
        };
    }

    private void close() {
        if (authenticating) {
            view.setAuthenticationStatus("Please wait for the account operation to finish, then close.", false);
            return;
        }
        try {
            database.close();
            view.dispose();
        } catch (SQLException exception) {
            view.showError("Could not close the database: " + exception.getMessage());
        }
    }

    private record Session(HomeModel model, HomeMediator mediator) { }
    private record AuthenticationResult(boolean success, UserAccount user) { }
}
