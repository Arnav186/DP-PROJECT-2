/** Authentication data exposed to the application never contains a password. */
public record UserAccount(long id, String username) { }
