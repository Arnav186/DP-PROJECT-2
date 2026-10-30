import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/** MVC controller: translates console requests into model operations. */
public final class SmartHomeController {
    private final UserRepository users;
    private final ConsoleView view;
    private final Map<Long, HomeSession> homes = new HashMap<>();

    public SmartHomeController(UserRepository users, ConsoleView view) {
        this.users = users;
        this.view = view;
    }

    public void run() {
        view.welcome();
        try {
            boolean running = true;
            while (running) {
                try {
                    switch (view.authenticationMenu()) {
                        case 1 -> register();
                        case 2 -> login();
                        case 3 -> running = false;
                        default -> throw new IllegalStateException("Invalid menu choice.");
                    }
                } catch (SQLException exception) {
                    view.message("Database operation failed: " + exception.getMessage());
                } catch (IllegalArgumentException exception) {
                    view.message("Error: " + exception.getMessage());
                }
            }
        } catch (NoSuchElementException exception) {
            view.message("\nInput closed.");
        }
        view.message("Goodbye!");
    }

    private void register() throws SQLException {
        view.message("\n--- REGISTER ---");
        String username = view.text("Username: ");
        String password = view.password("Password (minimum 4 characters): ");
        String confirmation = view.password("Confirm password: ");
        if (!password.equals(confirmation)) {
            view.message("Passwords do not match.");
            return;
        }
        view.message(users.register(username, password)
                ? "Registration successful." : "Username already exists.");
    }

    private void login() throws SQLException {
        view.message("\n--- LOGIN ---");
        String username = view.text("Username: ");
        String password = view.password("Password: ");
        Optional<UserAccount> authenticated = users.authenticate(username, password);
        if (authenticated.isEmpty()) {
            view.message("Invalid username or password.");
            view.message("[SECURITY ALERT] House-owner notification (simulated): "
                    + "failed login attempt for username " + username + ".");
            return;
        }
        UserAccount user = authenticated.get();
        HomeSession session = homes.get(user.id());
        if (session == null) {
            HomeModel model = new HomeModel(user);
            HomeMediator mediator = new SmartHomeMediator(model, new HomeHistory(), new NotificationCenter());
            session = new HomeSession(model, mediator);
            homes.put(user.id(), session);
        }
        view.message("Login successful. Welcome " + user.username() + "!");
        homeMenu(session);
    }

    private void homeMenu(HomeSession session) {
        HomeModel model = session.model();
        HomeMediator mediator = session.mediator();
        boolean loggedIn = true;
        while (loggedIn) {
            int previousActivity = model.getActivity().size();
            try {
                int choice = view.homeMenu(model.getOwner().username());
                switch (choice) {
                    case 1 -> createDevice(mediator, false);
                    case 2 -> {
                        UUID id = selectDevice(model);
                        if (id != null) { mediator.cloneDevice(id, view.text("Clone name: ")); }
                    }
                    case 3, 4 -> {
                        UUID id = selectDevice(model);
                        if (id != null) { mediator.setPower(id, choice == 3); }
                    }
                    case 5 -> view.devices(model.getDevices());
                    case 6 -> {
                        UUID id = selectDevice(model);
                        if (id != null) { mediator.changeCommunication(id, readCommunication()); }
                    }
                    case 7 -> securityEvent(mediator);
                    case 8 -> mediator.saveSnapshot();
                    case 9 -> {
                        if (!mediator.restoreSnapshot()) { view.message("No saved snapshot. Save one first."); }
                    }
                    case 10 -> {
                        if (!mediator.undo()) { view.message("No device changes to undo."); }
                    }
                    case 11 -> {
                        List<String> activity = model.getActivity();
                        if (activity.isEmpty()) { view.message("No activity yet."); }
                        else { view.messages(activity); }
                    }
                    case 12 -> createDevice(mediator, true);
                    case 13 -> {
                        loggedIn = false;
                        view.message("Logged out.");
                    }
                    default -> throw new IllegalStateException("Invalid menu choice.");
                }
            } catch (IllegalArgumentException exception) {
                view.message("Error: " + exception.getMessage());
            }
            List<String> activity = model.getActivity();
            view.messages(activity.subList(previousActivity, activity.size()));
        }
    }

    private void createDevice(HomeMediator mediator, boolean preset) {
        view.message(preset ? "1. Normal Home (Light)\n2. Security Home (Fan)"
                : "1. Light\n2. Fan");
        DeviceType type = view.choice("Choose type: ", 1, 2) == 1 ? DeviceType.LIGHT : DeviceType.FAN;
        String name = view.text("Device name: ");
        String room = view.text("Room: ");
        CommunicationStrategy communication = readCommunication();
        Device device = new Device.Builder(type, name)
                .room(room).communication(communication).initiallyOn(false).build();
        mediator.addDevice(device);
    }

    private CommunicationStrategy readCommunication() {
        view.message("1. WiFi\n2. Bluetooth");
        return view.choice("Communication: ", 1, 2) == 1 ? new WiFiStrategy() : new BluetoothStrategy();
    }

    private UUID selectDevice(HomeModel model) {
        List<Device> devices = model.getDevices();
        view.devices(devices);
        if (devices.isEmpty()) { return null; }
        int selection = view.choice("Select device (0 to cancel): ", 0, devices.size());
        return selection == 0 ? null : devices.get(selection - 1).getId();
    }

    private void securityEvent(HomeMediator mediator) {
        view.message("1. Motion\n2. Door\n3. Alarm");
        SecurityRoutine routine = switch (view.choice("Choose event: ", 1, 3)) {
            case 1 -> new MotionRoutine();
            case 2 -> new DoorRoutine();
            case 3 -> new AlarmRoutine();
            default -> throw new IllegalStateException("Invalid security event.");
        };
        mediator.securityEvent(routine, view.text("Event location: "));
    }

    private record HomeSession(HomeModel model, HomeMediator mediator) { }
}
