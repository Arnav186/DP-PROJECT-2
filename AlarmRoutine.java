public final class AlarmRoutine extends SecurityRoutine {
    public String type() { return "Alarm"; }

    protected String detect(String location) {
        return "Alarm activated at " + location + ".";
    }

    protected String respond(String location) {
        return "Emergency alert raised for " + location + ".";
    }
}
