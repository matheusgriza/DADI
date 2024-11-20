package dadi;
import java.util.*;

public class UserManager {
    private Map<String, User> users;

    public UserManager() {
        this.users = new HashMap<>();
        loadDefaultAdmin();
    }

    private void loadDefaultAdmin() {
        users.put("admin", new User("admin", "admin", Role.ADMIN));
    }

    public boolean authenticate(String username, String password) {
        User user = users.get(username);
        return user != null && user.getPassword().equals(password);
    }

    public boolean isAdmin(String username) {
        User user = users.get(username);
        return user != null && user.getRole() == Role.ADMIN;
    }

    public void createUser(String adminUsername, String username, String password, Role role) throws Exception {
        if (!isAdmin(adminUsername)) {
            throw new Exception("Solo los administradores pueden crear usuarios.");
        }
        users.put(username, new User(username, password, role));
    }
}
