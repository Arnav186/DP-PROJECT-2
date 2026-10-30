import java.util.UUID;

/** Mediator contract coordinating devices, security, history, and notifications. */
public interface HomeMediator {
    void addDevice(Device device);
    void cloneDevice(UUID sourceId, String newName);
    void setPower(UUID id, boolean on);
    void changeCommunication(UUID id, CommunicationStrategy strategy);
    void securityEvent(SecurityRoutine routine, String location);
    void saveSnapshot();
    boolean restoreSnapshot();
    boolean undo();
}
