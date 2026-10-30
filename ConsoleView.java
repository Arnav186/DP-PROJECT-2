import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/** MVC view: all console input, menus, and rendering live here. */
public final class ConsoleView {
    private final Scanner input;
    private final PrintStream output;

    public ConsoleView(Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public void welcome() {
        message("=================================");
        message("       SMART HOME SYSTEM");
        message("=================================");
    }

    public int authenticationMenu() {
        message("\n1. Register\n2. Login\n3. Exit");
        return choice("Enter choice: ", 1, 3);
    }

    public int homeMenu(String username) {
        message("\n========== HOME: " + username + " ==========");
        message("1. Create Device");
        message("2. Clone Device");
        message("3. Turn ON");
        message("4. Turn OFF");
        message("5. Device Status");
        message("6. Change Communication");
        message("7. Security Event");
        message("8. Save Home Snapshot");
        message("9. Restore Home Snapshot");
        message("10. Undo Last Device Change");
        message("11. Activity Log");
        message("12. Create Home Preset");
        message("13. Logout");
        return choice("Enter choice: ", 1, 13);
    }

    public String text(String prompt) {
        while (true) {
            output.print(prompt);
            String value = input.nextLine().trim();
            if (!value.isEmpty()) { return value; }
            message("Cannot be empty.");
        }
    }

    public String password(String prompt) {
        output.print(prompt);
        return input.nextLine();
    }

    public int choice(String prompt, int minimum, int maximum) {
        while (true) {
            try {
                int value = Integer.parseInt(text(prompt));
                if (value >= minimum && value <= maximum) { return value; }
                message("Choose a number from " + minimum + " to " + maximum + ".");
            } catch (NumberFormatException exception) {
                message("Enter a number.");
            }
        }
    }

    public void devices(List<Device> devices) {
        if (devices.isEmpty()) {
            message("No devices available.");
            return;
        }
        message("\n--- YOUR DEVICES ---");
        for (int index = 0; index < devices.size(); index++) {
            Device device = devices.get(index);
            message((index + 1) + ". " + device.getName() + " | " + device.getType().label()
                    + " | " + device.getRoom() + " | " + device.getCommunicationName()
                    + " | " + (device.isOn() ? "ON" : "OFF"));
        }
    }

    public void messages(List<String> messages) {
        messages.forEach(this::message);
    }

    public void message(String message) {
        output.println(message);
    }
}
