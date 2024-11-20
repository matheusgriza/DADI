package dadi;

public class FTPConfiguration {
    private String serverAddress;
    private int port;
    private String username;
    private String password;

    public FTPConfiguration(String serverAddress, int port, String username, String password) {
        this.serverAddress = serverAddress;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    public String getServerAddress() { return serverAddress; }
    public int getPort() { return port; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
}
