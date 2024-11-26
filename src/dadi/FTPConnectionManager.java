package dadi;

import java.io.*;
import java.net.*;

public class FTPConnectionManager {
    private Socket controlSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean isLoggedIn = false;

    public void connect(String server, int port) throws IOException {
        try {
            // Crear un socket simple para conectarse al servidor FTP
            controlSocket = new Socket(server, port);
            reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));

            // Leer el mensaje de bienvenida
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Mensaje de bienvenida: " + line);
                if (line.startsWith("220 ")) { // Fin del mensaje de bienvenida
                    break;
                }
            }

            System.out.println("Conexión establecida con el servidor FTP.");
        } catch (Exception e) {
            throw new IOException("Error al conectar al servidor FTP: " + e.getMessage(), e);
        }
    }

    public void login(String username, String password) throws IOException {
        sendCommand("USER " + username);
        String userResponse = readResponse();
        System.out.println("Respuesta USER: " + userResponse);

        if (!userResponse.startsWith("331")) {
            throw new IOException("El servidor no aceptó el usuario: " + userResponse);
        }

        sendCommand("PASS " + password);
        String passResponse = readResponse();
        System.out.println("Respuesta PASS: " + passResponse);

        if (!passResponse.startsWith("230")) {
            throw new IOException("Error al iniciar sesión: " + passResponse);
        }

        isLoggedIn = true;
        System.out.println("Inicio de sesión exitoso.");
    }

    public void disconnect() throws IOException {
        sendCommand("QUIT");
        readResponse();
        controlSocket.close();
        System.out.println("Conexión cerrada.");
    }

    private void sendCommand(String command) throws IOException {
        System.out.println("Enviando comando: " + command);
        writer.write(command + "\r\n");
        writer.flush();
    }

    private String readResponse() throws IOException {
        String response = reader.readLine();
        System.out.println("Respuesta del servidor: " + response);
        return response;
    }

    public void listFiles() throws IOException {
        if (!isLoggedIn) {
            throw new IllegalStateException("Debes iniciar sesión primero.");
        }

        try {
            // Configuración de modo pasivo extendido usando EPSV
            sendCommand("EPSV");
            String epsvResponse = readResponse();
            System.out.println("Respuesta EPSV: " + epsvResponse);

            // Extraer datos del modo pasivo extendido
            String[] pasvData = extractPassiveModeData(epsvResponse);
            String dataIP = pasvData[0];
            int dataPort = Integer.parseInt(pasvData[1]);

            // Crear socket de datos
            Socket dataSocket = new Socket(dataIP, dataPort);

            System.out.println("Conexión establecida en el canal de datos.");

            // Enviar comando LIST
            sendCommand("LIST");

            // Leer datos desde el canal de datos
            try (BufferedReader dataReader = new BufferedReader(
                    new InputStreamReader(dataSocket.getInputStream()))) {
                String line;
                System.out.println("Archivos en el directorio raíz:");
                while ((line = dataReader.readLine()) != null) {
                    System.out.println(line);
                }
            } finally {
                dataSocket.close();
            }

            // Leer respuesta final del comando LIST
            String finalResponse = readResponse();
            System.out.println("Respuesta final LIST: " + finalResponse);

        } catch (Exception e) {
            throw new IOException("Error al listar archivos: " + e.getMessage(), e);
        }
    }

    private String[] extractPassiveModeData(String response) {
        if (response.startsWith("229")) {
            // Manejo de respuesta EPSV: "229 Entering Extended Passive Mode (|||port|)"
            int start = response.indexOf("(");
            int end = response.indexOf(")");
            if (start == -1 || end == -1) {
                throw new IllegalStateException("Respuesta EPSV malformada: " + response);
            }

            String data = response.substring(start + 1, end);
            String[] parts = data.split("\\|");
            if (parts.length < 4 || parts[3].isEmpty()) {
                throw new IllegalStateException("Respuesta EPSV malformada: " + response);
            }

            String portString = parts[3];
            int port = Integer.parseInt(portString);

            // Usamos la misma IP del canal de control
            String ip = controlSocket.getInetAddress().getHostAddress();
            return new String[]{ip, String.valueOf(port)};
        } else if (response.startsWith("227")) {
            // Manejo de respuesta PASV: "227 Entering Passive Mode (h1,h2,h3,h4,p1,p2)"
            int start = response.indexOf("(");
            int end = response.indexOf(")");
            if (start == -1 || end == -1) {
                throw new IllegalStateException("Respuesta PASV malformada: " + response);
            }

            String[] parts = response.substring(start + 1, end).split(",");
            if (parts.length != 6) {
                throw new IllegalStateException("Respuesta PASV malformada: " + response);
            }

            String ip = String.join(".", parts[0], parts[1], parts[2], parts[3]);
            int port = Integer.parseInt(parts[4]) * 256 + Integer.parseInt(parts[5]);
            return new String[]{ip, String.valueOf(port)};
        } else {
            throw new IllegalStateException("Respuesta de modo pasivo desconocida: " + response);
        }
    }
}
