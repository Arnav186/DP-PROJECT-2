/** Template Method: subclasses customize steps, never the processing sequence. */
public abstract class SecurityRoutine {
    public final Result execute(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("Security event location is required.");
        }
        String area = location.trim();
        String detection = detect(area);
        String response = respond(area);
        String alert = type() + " event at " + area + ". " + response;
        return new Result(type(), area, detection, response, alert);
    }

    public abstract String type();
    protected abstract String detect(String location);
    protected abstract String respond(String location);

    public record Result(String type, String location, String detection, String response, String ownerAlert) { }
}
