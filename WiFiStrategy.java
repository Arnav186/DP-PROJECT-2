public final class WiFiStrategy implements CommunicationStrategy {
    public String name() {
        return "WiFi";
    }

    public String send(String deviceName, String command) {
        return "[WiFi] " + command + " -> " + deviceName;
    }
}
