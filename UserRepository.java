import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public final class UserRepository {
    private final Connection connection;
    private final PasswordHasher passwords;

    public UserRepository(Connection connection, PasswordHasher passwords) {
        this.connection = connection;
        this.passwords = passwords;
    }

    public boolean register(String username, String password) throws SQLException {
        if (username == null || username.isBlank() || username.trim().length() > 64) {
            throw new IllegalArgumentException("Username must contain 1 to 64 characters.");
        }
        if (password == null || password.isBlank() || password.length() < 4) {
            throw new IllegalArgumentException("Password must contain at least 4 characters.");
        }
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO users(username, password_hash) VALUES (?, ?)")) {
            statement.setString(1, username.trim());
            statement.setString(2, passwords.hash(password));
            statement.executeUpdate();
            return true;
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 19
                    && exception.getMessage().contains("users.username")) {
                return false;
            }
            throw exception;
        }
    }

    public Optional<UserAccount> authenticate(String username, String password) throws SQLException {
        if (username == null || password == null) { return Optional.empty(); }
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT id, username, password_hash FROM users WHERE username = ?")) {
            statement.setString(1, username.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && passwords.matches(password, result.getString("password_hash"))) {
                    return Optional.of(new UserAccount(result.getLong("id"), result.getString("username")));
                }
            }
        }
        return Optional.empty();
    }
}
