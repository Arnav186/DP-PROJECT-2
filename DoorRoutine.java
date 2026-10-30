public final class DoorRoutine extends SecurityRoutine {
    public String type() { return "Door"; }

    protected String detect(String location) {
        return "Door event detected at " + location + ".";
    }

    protected String respond(String location) {
        return "Verify the entry and door lock at " + location + ".";
    }
}
