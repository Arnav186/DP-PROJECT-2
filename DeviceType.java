public enum DeviceType {
    LIGHT("Light"), FAN("Fan");

    private final String label;

    DeviceType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
