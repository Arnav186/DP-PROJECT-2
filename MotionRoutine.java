public final class MotionRoutine extends SecurityRoutine {
    public String type() { return "Motion"; }

    protected String detect(String location) {
        return "Motion detected at " + location + ".";
    }

    protected String respond(String location) {
        return "Check the camera feed for " + location + ".";
    }
}
