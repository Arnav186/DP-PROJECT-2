import java.util.Objects;
import java.util.UUID;

public final class SmartHomeMediator implements HomeMediator {
    private final HomeModel home;
    private final HomeHistory history;
    private final NotificationCenter notifications;

    public SmartHomeMediator(HomeModel home, HomeHistory history,
                             NotificationCenter notifications) {
        this.home = Objects.requireNonNull(home);
        this.history = Objects.requireNonNull(history);
        this.notifications = Objects.requireNonNull(notifications);
    }

    public void addDevice(Device device) {
        HomeModel.Snapshot previous = home.save();
        home.addDevice(device);
        history.remember(previous);
        announce("Device created: " + device.getName() + ".");
    }

    public void cloneDevice(UUID sourceId, String newName) {
        Device source = home.getDevice(sourceId);
        Device clone = source.copy(newName);
        HomeModel.Snapshot previous = home.save();
        home.addDevice(clone);
        history.remember(previous);
        announce("Device cloned: " + source.getName() + " -> " + clone.getName() + ".");
    }

    public void setPower(UUID id, boolean on) {
        Device device = home.getDevice(id);
        if (device.isOn() == on) {
            home.recordActivity(device.getName() + " is already " + (on ? "ON." : "OFF."));
            return;
        }
        HomeModel.Snapshot previous = home.save();
        String delivery = home.setPower(id, on);
        history.remember(previous);
        home.recordActivity(delivery);
        announce(device.getName() + " turned " + (on ? "ON." : "OFF."));
    }

    public void changeCommunication(UUID id, CommunicationStrategy strategy) {
        Device device = home.getDevice(id);
        Objects.requireNonNull(strategy, "Communication is required.");
        if (device.getCommunicationName().equals(strategy.name())) {
            home.recordActivity(device.getName() + " already uses " + strategy.name() + ".");
            return;
        }
        HomeModel.Snapshot previous = home.save();
        home.changeCommunication(id, strategy);
        history.remember(previous);
        announce(device.getName() + " now uses " + strategy.name() + ".");
    }

    public void securityEvent(SecurityRoutine routine, String location) {
        SecurityRoutine.Result result = routine.execute(location);
        home.recordActivity(result.detection());
        home.recordActivity(result.response());
        notifications.alertOwner(home, result.ownerAlert());
        notifications.notifyDeviceChange(home,
                "Security event: " + result.type() + " at " + result.location() + ".");
    }

    public void saveSnapshot() {
        history.save(home);
        home.recordActivity("Home snapshot saved.");
    }

    public boolean restoreSnapshot() {
        if (!history.restoreSaved(home)) { return false; }
        announce("Saved home snapshot restored.");
        return true;
    }

    public boolean undo() {
        if (!history.undo(home)) { return false; }
        announce("Last device change undone.");
        return true;
    }

    private void announce(String message) {
        home.recordActivity(message);
        notifications.notifyDeviceChange(home, message);
    }
}
