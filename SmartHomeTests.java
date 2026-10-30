import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/** Dependency-free behavior and integration tests. No production data is touched. */
public final class SmartHomeTests {
    private static int checks;

    public static void main(String[] args) throws Exception {
        builderPrototypeAndStrategy();
        mementoAndIsolation();
        mediatorAndSecurityTemplate();
        failedOperationsDoNotChangeState();
        Path directory = Files.createTempDirectory("smart-home-tests-");
        Path database = directory.resolve("test.db");
        try {
            authentication(database);
            controllerWorkflow(database);
        } finally {
            Files.deleteIfExists(database);
            Files.deleteIfExists(directory);
        }
        System.out.println("PASS: " + checks + " checks across all six patterns, authentication, and MVC workflows.");
    }

    private static void builderPrototypeAndStrategy() {
        Device light = new Device.Builder(DeviceType.LIGHT, " Desk Lamp ").build();
        check(light.getName().equals("Desk Lamp"), "Builder normalizes the name");
        check(light.getRoom().equals("Living Room") && !light.isOn(), "Builder defaults");
        expectFailure(() -> new Device.Builder(DeviceType.FAN, " "), "Reject empty device names");
        expectFailure(() -> new Device.Builder(DeviceType.FAN, "Fan").room(""), "Reject empty rooms");

        Device configured = new Device.Builder(DeviceType.FAN, "Ceiling Fan")
                .room("Bedroom").communication(new BluetoothStrategy()).initiallyOn(true).build();
        Device clone = configured.copy("Backup Fan");
        check(!clone.getId().equals(configured.getId()), "Prototype has its own identity");
        check(clone.getName().equals("Backup Fan") && clone.isOn(), "Prototype copies state");
        check(clone.getType() == DeviceType.FAN && clone.getRoom().equals("Bedroom"), "Prototype copies configuration");
        check(clone.getCommunicationName().equals("Bluetooth"), "Prototype copies communication");

        HomeModel model = home(1);
        SmartHomeMediator mediator = mediator(model);
        mediator.addDevice(configured);
        mediator.addDevice(clone);
        mediator.setPower(clone.getId(), false);
        check(model.getDevice(configured.getId()).isOn(), "Clone power is independent");
        check(!model.getDevice(clone.getId()).isOn(), "Only clone is changed");
        check(model.getActivity().contains("[Bluetooth] OFF -> Backup Fan"), "Bluetooth strategy delivers command");
        mediator.addDevice(light);
        mediator.setPower(light.getId(), true);
        check(model.getActivity().contains("[WiFi] ON -> Desk Lamp"), "Default WiFi strategy delivers command");
        mediator.changeCommunication(light.getId(), new BluetoothStrategy());
        mediator.setPower(light.getId(), false);
        check(model.getActivity().contains("[Bluetooth] OFF -> Desk Lamp"), "Strategy switches at runtime");
    }

    private static void mementoAndIsolation() {
        HomeModel model = home(2);
        SmartHomeMediator mediator = mediator(model);
        check(!mediator.undo() && !mediator.restoreSnapshot(), "Empty history is handled");
        Device lamp = new Device.Builder(DeviceType.LIGHT, "Lamp").build();
        mediator.addDevice(lamp);
        mediator.saveSnapshot();
        mediator.setPower(lamp.getId(), true);
        mediator.changeCommunication(lamp.getId(), new BluetoothStrategy());
        mediator.cloneDevice(lamp.getId(), "Copy");
        check(model.getDevices().size() == 2, "Mediator creates prototype clone");
        check(mediator.restoreSnapshot(), "Saved snapshot restores");
        check(model.getDevices().size() == 1 && !model.getDevice(lamp.getId()).isOn(), "Snapshot restores membership and power");
        check(model.getDevice(lamp.getId()).getCommunicationName().equals("WiFi"), "Snapshot restores strategy");
        check(mediator.undo() && model.getDevices().size() == 2, "Restore itself can be undone");
        check(model.getDevice(lamp.getId()).isOn(), "Undo restore recovers power");
        mediator.restoreSnapshot();
        mediator.setPower(lamp.getId(), true);
        mediator.restoreSnapshot();
        check(!model.getDevice(lamp.getId()).isOn(), "Snapshot can be reused without mutation");
        check(model.getActivity().stream().anyMatch(s -> s.contains("Device cloned")), "Restore retains event history");

        HomeModel other = home(3);
        expectFailure(() -> other.restore(model.save()), "Cross-account restore rejected");
        expectFailure(() -> other.setPower(lamp.getId(), true), "Cross-account device access rejected");
        check(other.getDevices().isEmpty(), "Other user's home stays empty");
        Device detached = model.getDevice(lamp.getId());
        mediator.setPower(lamp.getId(), true);
        check(!detached.isOn(), "Model read returns an independent copy");

        HomeModel undoHome = home(4);
        SmartHomeMediator undoMediator = mediator(undoHome);
        undoMediator.addDevice(lamp);
        undoMediator.setPower(lamp.getId(), false);
        check(undoMediator.undo() && undoHome.getDevices().isEmpty(), "No-op command does not consume undo");
    }

    private static void mediatorAndSecurityTemplate() {
        StringBuilder steps = new StringBuilder();
        SecurityRoutine ordered = new SecurityRoutine() {
            public String type() { return "Test"; }
            protected String detect(String location) { steps.append("detect;"); return "Detected " + location; }
            protected String respond(String location) { steps.append("respond;"); return "Responded " + location; }
        };
        SecurityRoutine.Result result = ordered.execute(" Garage ");
        check(steps.toString().equals("detect;respond;"), "Template keeps step order");
        check(result.location().equals("Garage") && result.ownerAlert().contains("Responded Garage"), "Template validates and assembles alert");
        expectFailure(() -> ordered.execute(" "), "Template rejects empty event location");

        HomeModel model = home(5);
        SmartHomeMediator mediator = mediator(model);
        for (SecurityRoutine routine : List.of(new MotionRoutine(), new DoorRoutine(), new AlarmRoutine())) {
            mediator.securityEvent(routine, "Entrance");
            check(model.getActivity().stream().anyMatch(s -> s.contains("[Admin] Security event: " + routine.type())),
                    routine.type() + " reaches admin through mediator");
        }
        check(model.getActivity().stream().filter(s -> s.startsWith("[SECURITY ALERT]")).count() == 3,
                "Mediator routes each security alert to owner");
        check(model.getActivity().stream().filter(s -> s.startsWith("[Mobile App]")).count() == 3,
                "Mediator coordinates mobile notifications");
        check(!mediator.undo(), "Security events do not enter device undo history");
    }

    private static void failedOperationsDoNotChangeState() {
        HomeModel model = home(6);
        SmartHomeMediator mediator = mediator(model);
        Device lamp = new Device.Builder(DeviceType.LIGHT, "Lamp").build();
        mediator.addDevice(lamp);
        expectFailure(() -> mediator.addDevice(lamp), "Duplicate identity rejected");
        expectFailure(() -> mediator.cloneDevice(lamp.getId(), " "), "Invalid clone rejected");
        expectFailure(() -> mediator.setPower(UUID.randomUUID(), true), "Unknown device rejected");
        check(mediator.undo() && model.getDevices().isEmpty(), "Failed operations leave undo unchanged");

        CommunicationStrategy broken = new CommunicationStrategy() {
            public String name() { return "Broken"; }
            public String send(String device, String command) { throw new IllegalArgumentException("Delivery failed"); }
        };
        Device offline = new Device.Builder(DeviceType.FAN, "Offline Fan").communication(broken).build();
        mediator.addDevice(offline);
        expectFailure(() -> mediator.setPower(offline.getId(), true), "Communication failure reported");
        check(!model.getDevice(offline.getId()).isOn(), "Failed delivery preserves power state");
        check(mediator.undo() && model.getDevices().isEmpty(), "Failed delivery does not add undo entry");
    }

    private static void authentication(Path database) throws Exception {
        try (DatabaseConnection db = new DatabaseConnection(database)) {
            UserRepository users = new UserRepository(db.getConnection(), new PasswordHasher());
            check(users.register("alice", "test-pass"), "Registration succeeds");
            check(!users.register("alice", "different-pass"), "Duplicate registration rejected");
            check(users.authenticate("alice", "test-pass").isPresent(), "Correct password authenticates");
            check(users.authenticate("alice", "wrong").isEmpty(), "Wrong password rejected");
            check(users.authenticate("missing", "test-pass").isEmpty(), "Unknown account rejected");
            check(users.authenticate("' OR 1=1 --", "test-pass").isEmpty(), "Parameterized login resists SQL input");
            check(users.register("bob", "test-pass"), "Second account registered");
            try (Statement statement = db.getConnection().createStatement();
                 ResultSet rows = statement.executeQuery("SELECT password_hash FROM users ORDER BY id")) {
                rows.next();
                String first = rows.getString(1);
                rows.next();
                String second = rows.getString(1);
                check(!first.contains("test-pass") && !first.equals(second), "Passwords hashed with independent salts");
            }
            PasswordHasher hasher = new PasswordHasher();
            check(!hasher.matches("test-pass", "corrupt"), "Malformed password record rejected safely");
        }
        try (DatabaseConnection reopened = new DatabaseConnection(database)) {
            UserRepository users = new UserRepository(reopened.getConnection(), new PasswordHasher());
            check(users.authenticate("alice", "test-pass").isPresent(), "Accounts survive database reopen");
        }
    }

    private static void controllerWorkflow(Path database) throws Exception {
        String script = String.join("\n",
                "oops", "99",                       // invalid authentication choices
                "1", "charlie", "pass123", "bad", // mismatched confirmation
                "1", "charlie", "pass123", "pass123", // register
                "1", "charlie", "pass123", "pass123", // duplicate
                "2", "alice", "wrong",            // rejected login
                "2", "alice", "test-pass",        // authenticated session
                "9", "10", "5",                  // no snapshot, undo, devices
                "1", "1", "Desk", "Study", "1", // create WiFi light
                "3", "0",                         // cancel selection
                "3", "1",                         // power on
                "2", "1", "Desk Copy",           // clone on light
                "6", "2", "2",                   // switch clone to Bluetooth
                "4", "2",                         // power clone off
                "8",                               // save two devices
                "12", "2", "Security Fan", "Hall", "2", // home preset via Builder
                "10",                              // undo preset creation
                "3", "2",                         // clone on
                "9", "5",                         // restore clone off, then status
                "7", "1", "Study",               // motion
                "7", "2", "Entrance",            // door
                "7", "3", "Hall",                // alarm
                "11", "13",                       // log, logout
                "2", "bob", "test-pass", "5", "9", "13", // isolated home/history
                "2", "alice", "test-pass", "5", "13",     // resume own home
                "3", "");
        try (DatabaseConnection db = new DatabaseConnection(database);
             Scanner scanner = new Scanner(new StringReader(script));
             ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            UserRepository users = new UserRepository(db.getConnection(), new PasswordHasher());
            new SmartHomeController(users, new ConsoleView(scanner, output)).run();
            String rendered = bytes.toString(StandardCharsets.UTF_8);
            check(rendered.contains("Enter a number.") && rendered.contains("Choose a number from 1 to 3."), "Invalid console input recovers");
            check(rendered.contains("Passwords do not match.") && rendered.contains("Registration successful."), "Console registration and confirmation");
            check(rendered.contains("Username already exists."), "Console duplicate registration");
            check(rendered.contains("House-owner notification (simulated)"), "Failed login shows owner alert");
            check(rendered.contains("[WiFi] ON -> Desk"), "MVC power-on workflow");
            check(rendered.contains("[Bluetooth] OFF -> Desk Copy"), "MVC clone and strategy workflow");
            check(rendered.contains("Desk Copy | Light | Study | Bluetooth | OFF"), "MVC restored status");
            check(rendered.contains("Device created: Security Fan."), "Home preset uses Builder");
            check(rendered.contains("Motion detected") && rendered.contains("Door event detected")
                    && rendered.contains("Alarm activated"), "All console security events execute");
            int bobStart = rendered.indexOf("Login successful. Welcome bob!");
            int bobEnd = rendered.indexOf("Logged out.", bobStart);
            String bobSession = rendered.substring(bobStart, bobEnd);
            check(bobSession.contains("No devices available.") && bobSession.contains("No saved snapshot."), "MVC account isolation");
            int aliceReturn = rendered.lastIndexOf("Login successful. Welcome alice!");
            check(rendered.substring(aliceReturn).contains("Desk Copy | Light | Study | Bluetooth | OFF"), "Re-login retains own home in current process");
            check(rendered.contains("Goodbye!") && !rendered.contains("Input closed."), "Script reaches normal exit");
        }
        try (DatabaseConnection db = new DatabaseConnection(database);
             Scanner scanner = new Scanner("2\nalice\ntest-pass\n1\n");
             ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new SmartHomeController(new UserRepository(db.getConnection(), new PasswordHasher()),
                    new ConsoleView(scanner, output)).run();
            check(bytes.toString(StandardCharsets.UTF_8).contains("Input closed."), "EOF inside a menu exits cleanly");
        }
    }

    private static HomeModel home(long id) {
        return new HomeModel(new UserAccount(id, "owner" + id));
    }

    private static SmartHomeMediator mediator(HomeModel model) {
        return new SmartHomeMediator(model, new HomeHistory(), new NotificationCenter());
    }

    private static void check(boolean condition, String description) {
        if (!condition) { throw new AssertionError(description); }
        checks++;
    }

    private static void expectFailure(Runnable operation, String description) {
        try {
            operation.run();
            throw new AssertionError(description);
        } catch (IllegalArgumentException expected) {
            checks++;
        }
    }
}
