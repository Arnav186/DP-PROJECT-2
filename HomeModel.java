import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** MVC model and Memento originator; one instance belongs to one account. */
public final class HomeModel {
    private final UserAccount owner;
    private final UUID homeId = UUID.randomUUID();
    private final Map<UUID, Device> devices = new LinkedHashMap<>();
    private final List<String> activity = new ArrayList<>();

    public HomeModel(UserAccount owner) {
        this.owner = Objects.requireNonNull(owner);
    }

    public UserAccount getOwner() { return owner; }

    public List<Device> getDevices() {
        return devices.values().stream().map(Device::snapshotCopy).toList();
    }

    public Device getDevice(UUID id) {
        return requireDevice(id).snapshotCopy();
    }

    public List<String> getActivity() {
        return List.copyOf(activity);
    }

    public void recordActivity(String message) {
        activity.add(message);
    }

    public void addDevice(Device device) {
        Objects.requireNonNull(device);
        if (devices.containsKey(device.getId())) {
            throw new IllegalArgumentException("This device already belongs to the home.");
        }
        devices.put(device.getId(), device.snapshotCopy());
    }

    public String setPower(UUID id, boolean on) {
        return requireDevice(id).setPower(on);
    }

    public void changeCommunication(UUID id, CommunicationStrategy strategy) {
        requireDevice(id).setCommunication(strategy);
    }

    private Device requireDevice(UUID id) {
        Device device = devices.get(id);
        if (device == null) {
            throw new IllegalArgumentException("Device does not belong to this home.");
        }
        return device;
    }

    public Snapshot save() {
        return new Snapshot(homeId, getDevices());
    }

    public void restore(Snapshot snapshot) {
        Objects.requireNonNull(snapshot, "Snapshot is required.");
        if (!homeId.equals(snapshot.homeId)) {
            throw new IllegalArgumentException("Cannot restore another home's snapshot.");
        }
        devices.clear();
        for (Device device : snapshot.devices) {
            devices.put(device.getId(), device.snapshotCopy());
        }
    }

    /** Opaque Memento: only HomeModel can access or restore the saved device state. */
    public static final class Snapshot {
        private final UUID homeId;
        private final List<Device> devices;

        private Snapshot(UUID homeId, List<Device> devices) {
            this.homeId = homeId;
            this.devices = devices;
        }
    }
}
