import java.util.Objects;
import java.util.UUID;

public final class Device {
    private final UUID id;
    private final DeviceType type;
    private final String name;
    private final String room;
    private CommunicationStrategy communication;
    private boolean on;

    private Device(UUID id, DeviceType type, String name, String room,
                   CommunicationStrategy communication, boolean on) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.room = room;
        this.communication = communication;
        this.on = on;
    }

    public UUID getId() { return id; }
    public DeviceType getType() { return type; }
    public String getName() { return name; }
    public String getRoom() { return room; }
    public String getCommunicationName() { return communication.name(); }
    public boolean isOn() { return on; }

    // Prototype: duplicate a configured device, giving the clone a new identity.
    public Device copy(String newName) {
        return new Device(UUID.randomUUID(), type, required(newName, "Device name"),
                room, communication, on);
    }

    // Internal snapshot copy retains identity. The supplied strategies are stateless.
    Device snapshotCopy() {
        return new Device(id, type, name, room, communication, on);
    }

    String setPower(boolean power) {
        String delivery = communication.send(name, power ? "ON" : "OFF");
        on = power;
        return delivery;
    }

    void setCommunication(CommunicationStrategy strategy) {
        communication = Objects.requireNonNull(strategy, "Communication is required.");
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " cannot be empty.");
        }
        return value.trim();
    }

    /** Builder: construct a device step by step with defaults and validation. */
    public static final class Builder {
        private final DeviceType type;
        private final String name;
        private String room = "Living Room";
        private CommunicationStrategy communication = new WiFiStrategy();
        private boolean on;

        public Builder(DeviceType type, String name) {
            this.type = Objects.requireNonNull(type, "Device type is required.");
            this.name = required(name, "Device name");
        }

        public Builder room(String room) {
            this.room = required(room, "Room");
            return this;
        }

        public Builder communication(CommunicationStrategy strategy) {
            communication = Objects.requireNonNull(strategy, "Communication is required.");
            return this;
        }

        public Builder initiallyOn(boolean on) {
            this.on = on;
            return this;
        }

        public Device build() {
            return new Device(UUID.randomUUID(), type, name, room, communication, on);
        }
    }
}
