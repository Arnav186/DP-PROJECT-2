
/** A mediator colleague. Delivery is simulated as messages in the home activity log. */
public final class NotificationCenter {
    public void notifyDeviceChange(HomeModel home, String message) {
        home.recordActivity("[Mobile App] " + message);
        home.recordActivity("[Admin] " + message);
    }

    public void alertOwner(HomeModel home, String message) {
        home.recordActivity("[SECURITY ALERT] House owner ("
                + home.getOwner().username() + "): " + message);
    }
}
