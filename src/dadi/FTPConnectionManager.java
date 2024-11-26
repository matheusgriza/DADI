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

    Socket dataSocket = null;
    BufferedReader dataReader = null;

    try {
        // 1. Configurar modo pasivo extendido (EPSV)
        sendCommand("EPSV");
        String epsvResponse = readResponse();
        if (!epsvResponse.startsWith("229")) {
            throw new IOException("Respuesta de modo pasivo desconocida: " + epsvResponse);
        }
        System.out.println("Respuesta EPSV: " + epsvResponse);

        // 2. Extraer IP y puerto del canal de datos
        String[] pasvData = extractPassiveModeData(epsvResponse);
        String dataIP = pasvData[0];
        int dataPort = Integer.parseInt(pasvData[1]);

        // 3. Abrir canal de datos
        dataSocket = new Socket(dataIP, dataPort);
        System.out.println("Canal de datos abierto en: " + dataIP + ":" + dataPort);

        // 4. Enviar el comando LIST
        sendCommand("LIST");

        // 5. Leer respuesta 150 (inicio de transferencia)
        String listResponse = readResponse();
        if (!listResponse.startsWith("150")) {
            throw new IOException("Error al iniciar el comando LIST: " + listResponse);
        }
        System.out.println("Respuesta LIST: " + listResponse);

        // 6. Leer datos del canal de datos
        dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()));
        System.out.println("Archivos en el directorio actual:");
        String line;
        while ((line = dataReader.readLine()) != null) {
            System.out.println(line);
        }

        // 7. Leer respuesta final 226 (fin de transferencia)
        String finalResponse = readResponse();
        if (!finalResponse.startsWith("226")) {
            throw new IOException("Error al completar el comando LIST: " + finalResponse);
        }
        System.out.println("Respuesta final LIST: " + finalResponse);

    } catch (Exception e) {
        throw new IOException("Error al listar archivos: " + e.getMessage(), e);
    } finally {
        // 8. Cerrar recursos
        if (dataReader != null) {
            try {
                dataReader.close();
            } catch (IOException ignored) {}
        }
        if (dataSocket != null && !dataSocket.isClosed()) {
            try {
                dataSocket.close();
            } catch (IOException ignored) {}
        }
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
    
    public void changeDirectory(String directory) throws IOException {
    sendCommand("CWD " + directory);
    String response = readResponse();
    if (!response.startsWith("250")) {
        throw new IOException("Error al cambiar de directorio: " + response);
    }
    System.out.println("Directorio cambiado a: " + directory);
}


public void downloadFile(String remoteFilePath, String localFilePath) throws IOException {
    if (!isLoggedIn) {
        throw new IllegalStateException("Debes iniciar sesión primero.");
    }

    Socket dataSocket = null;

    try {
        // 1. Configurar modo pasivo extendido (EPSV)
        sendCommand("EPSV");
        String epsvResponse = readResponse();
        if (!epsvResponse.startsWith("229")) {
            throw new IOException("Respuesta de modo pasivo desconocida: " + epsvResponse);
        }
        System.out.println("Respuesta EPSV: " + epsvResponse);

        // 2. Extraer IP y puerto del canal de datos
        String[] pasvData = extractPassiveModeData(epsvResponse);
        String dataIP = pasvData[0];
        int dataPort = Integer.parseInt(pasvData[1]);

        // 3. Abrir el canal de datos
        dataSocket = new Socket(dataIP, dataPort);
        System.out.println("Canal de datos abierto en: " + dataIP + ":" + dataPort);

        // 4. Enviar comando RETR para descargar el archivo
        sendCommand("RETR " + remoteFilePath);
        String response = readResponse();
        if (!response.startsWith("150")) {
            throw new IOException("Error al iniciar la descarga del archivo: " + response);
        }
        System.out.println("Respuesta RETR: " + response);

        // 5. Guardar el archivo descargado
        try (InputStream dataInputStream = dataSocket.getInputStream();
             FileOutputStream fileOutputStream = new FileOutputStream(localFilePath + "/" + new File(remoteFilePath).getName())) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = dataInputStream.read(buffer)) != -1) {
                fileOutputStream.write(buffer, 0, bytesRead);
            }
        }

        // 6. Leer respuesta final del servidor
        String finalResponse = readResponse();
        if (!finalResponse.startsWith("226")) {
            throw new IOException("Error al completar la descarga: " + finalResponse);
        }
        System.out.println("Archivo descargado exitosamente: " + localFilePath);

    } catch (Exception e) {
        throw new IOException("Error al descargar el archivo: " + e.getMessage(), e);
    } finally {
        // 7. Cerrar el socket de datos
        if (dataSocket != null && !dataSocket.isClosed()) {
            try {
                dataSocket.close();
            } catch (IOException ignored) {}
        }
    }
}


public void uploadFile(String localFilePath, String remoteFilePath) throws IOException {
    if (!isLoggedIn) {
        throw new IllegalStateException("Debes iniciar sesión primero.");
    }

    Socket dataSocket = null;

    try {
        // 1. Configurar modo pasivo extendido (EPSV)
        sendCommand("EPSV");
        String epsvResponse = readResponse();
        if (!epsvResponse.startsWith("229")) {
            throw new IOException("Respuesta de modo pasivo desconocida: " + epsvResponse);
        }
        System.out.println("Respuesta EPSV: " + epsvResponse);

        // 2. Extraer IP y puerto del canal de datos
        String[] pasvData = extractPassiveModeData(epsvResponse);
        String dataIP = pasvData[0];
        int dataPort = Integer.parseInt(pasvData[1]);

        // 3. Abrir el canal de datos
        dataSocket = new Socket(dataIP, dataPort);
        System.out.println("Canal de datos abierto en: " + dataIP + ":" + dataPort);

        // 4. Enviar el comando STOR
        sendCommand("STOR " + remoteFilePath);
        String response = readResponse();
        if (!response.startsWith("150")) {
            throw new IOException("Error al iniciar la subida del archivo: " + response);
        }
        System.out.println("Respuesta STOR: " + response);

        // 5. Enviar el archivo local a través del canal de datos
        try (FileInputStream fileInputStream = new FileInputStream(localFilePath);
             OutputStream dataOutputStream = dataSocket.getOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                dataOutputStream.write(buffer, 0, bytesRead);
            }
            dataOutputStream.flush();
        }

        // 6. Leer respuesta final del servidor
        String finalResponse = readResponse();
        if (!finalResponse.startsWith("226")) {
            throw new IOException("Error al completar la subida del archivo: " + finalResponse);
        }
        System.out.println("Archivo subido exitosamente: " + remoteFilePath);

    } catch (Exception e) {
        throw new IOException("Error al subir el archivo: " + e.getMessage(), e);
    } finally {
        // 7. Cerrar el canal de datos
        if (dataSocket != null && !dataSocket.isClosed()) {
            try {
                dataSocket.close();
            } catch (IOException ignored) {}
        }
    }
}

public void createDirectory(String directoryName) throws IOException {
    sendCommand("MKD " + directoryName);
    String response = readResponse();
    if (!response.startsWith("257") && !response.startsWith("226")) {
        throw new IOException("Error al crear el directorio: " + response);
    }
    System.out.println("Directorio creado: " + directoryName);
}

public void deleteDirectory(String directoryName) throws IOException {
    sendCommand("RMD " + directoryName);
    String response = readResponse();
    if (!response.startsWith("250")) {
        throw new IOException("Error al eliminar el directorio: " + response);
    }
    System.out.println("Directorio eliminado: " + directoryName);
}

public void rename(String currentName, String newName) throws IOException {
    sendCommand("RNFR " + currentName);
    String response = readResponse();
    if (!response.startsWith("350")) {
        throw new IOException("Error al renombrar: " + response);
    }

    sendCommand("RNTO " + newName);
    response = readResponse();
    if (!response.startsWith("250")) {
        throw new IOException("Error al completar el renombrado: " + response);
    }
    System.out.println("Archivo o carpeta renombrado de " + currentName + " a " + newName);
}

private Socket createDataSocket() throws IOException {
    sendCommand("PASV");
    String response = readResponse();
    if (!response.startsWith("227")) {
        throw new IOException("Error al iniciar conexión PASV: " + response);
    }

    // Parsear la respuesta PASV para obtener IP y puerto
    String[] data = extractPassiveModeData(response);
    String dataIP = data[0];
    int dataPort = Integer.parseInt(data[1]);

    return new Socket(dataIP, dataPort);
}

}
