package dadi;

import java.io.*;
import java.net.*;
import java.util.*;

public class FTPConnectionManager {
    private Socket controlSocket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public void connect(String server, int port) throws IOException {
        controlSocket = new Socket(server, port);
        reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));
        readResponse(); // Lee la respuesta inicial del servidor.
    }

    public void login(String username, String password) throws IOException {
        sendCommand("USER " + username);
        readResponse();
        sendCommand("PASS " + password);
        readResponse();
    }

    public List<String> listFiles() throws IOException {
        sendCommand("PASV"); // Entra en modo pasivo.
        String response = readResponse();
        Socket dataSocket = createDataSocket(response);

        sendCommand("LIST");
        readResponse();

        List<String> files = new ArrayList<>();
        try (BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()))) {
            String line;
            while ((line = dataReader.readLine()) != null) {
                files.add(line);
            }
        }
        dataSocket.close();
        readResponse();
        return files;
    }

    private void sendCommand(String command) throws IOException {
        writer.write(command + "\r\n");
        writer.flush();
    }

    private String readResponse() throws IOException {
        return reader.readLine();
    }

    private Socket createDataSocket(String response) throws IOException {
        // Parse la respuesta del modo PASV para determinar el puerto de datos.
        String[] parts = response.split(",");
        String host = parts[0].substring(parts[0].lastIndexOf('(') + 1) + "." +
                      parts[1] + "." + parts[2] + "." + parts[3];
        int port = Integer.parseInt(parts[4]) * 256 + Integer.parseInt(parts[5].substring(0, parts[5].indexOf(')')));
        return new Socket(host, port);
    }

    public void disconnect() throws IOException {
        sendCommand("QUIT");
        readResponse();
        controlSocket.close();
    }
}
