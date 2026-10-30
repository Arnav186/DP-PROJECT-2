/** Strategy: the device delegates command delivery to a replaceable algorithm. */
public interface CommunicationStrategy {
    String name();
    String send(String deviceName, String command);
}
