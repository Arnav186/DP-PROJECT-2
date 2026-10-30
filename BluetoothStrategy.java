public final class BluetoothStrategy implements CommunicationStrategy {
    public String name() {
        return "Bluetooth";
    }

    public String send(String deviceName, String command) {
        return "[Bluetooth] " + command + " -> " + deviceName;
    }
}
