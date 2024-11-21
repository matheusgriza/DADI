package dadi;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

public class FTPConnectionManager {
    private Socket controlSocket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private boolean isLoggedIn = false;

    public void connect(String server, int port) throws IOException {
    try {
        // Crear un socket sin cifrar para conectarse al servidor
        controlSocket = new Socket(server, port);
        reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));

        // Leer todo el mensaje de bienvenida
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("Mensaje de bienvenida: " + line);
            if (line.startsWith("220 ")) { // Fin del mensaje de bienvenida
                break;
            }
        }

        // Enviar comando AUTH TLS
        sendCommand("AUTH TLS");
        String authResponse = readResponse();
        System.out.println("Respuesta AUTH TLS: " + authResponse);

        // Verificar que el servidor aceptó TLS
        if (!authResponse.startsWith("234")) {
            throw new IOException("El servidor no aceptó AUTH TLS: " + authResponse);
        }

        // Establecer la conexión TLS confiando en todos los certificados
        trustAllCertificates();

    } catch (Exception e) {
        throw new IOException("Error al establecer la conexión segura: " + e.getMessage(), e);
    }
}


    private void trustAllCertificates() throws Exception {
        // Configurar un gestor de confianza que acepte todos los certificados
        TrustManager[] trustAllCerts = new TrustManager[] {
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                public void checkServerTrusted(X509Certificate[] certs, String authType) {}
            }
        };

        // Inicializar el contexto SSL
        SSLContext sc = SSLContext.getInstance("TLS");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        SSLSocketFactory sslSocketFactory = sc.getSocketFactory();

        // Crear un socket seguro
        SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(controlSocket, controlSocket.getInetAddress().getHostAddress(), controlSocket.getPort(), true);

        // Configurar el uso de TLS 1.3
        sslSocket.setEnabledProtocols(new String[]{"TLSv1.3"});

        // Actualizar el lector y escritor para el socket seguro
        controlSocket = sslSocket;
        reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
        writer = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));

        System.out.println("Conexión TLS establecida.");
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
    
    public List<String> listFiles() throws IOException {
    if (!isLoggedIn) {
        throw new IOException("Debes autenticarte antes de listar archivos.");
    }

    // Enviar comando PASV
    sendCommand("PASV");
    String pasvResponse = readResponse();
    System.out.println("Respuesta PASV: " + pasvResponse);

    // Crear el socket de datos a partir de la respuesta PASV
    Socket dataSocket = createDataSocket(pasvResponse);

    // Enviar comando LIST
    sendCommand("LIST");
    String listResponse = readResponse();
    System.out.println("Respuesta LIST: " + listResponse);

    // Leer la lista de archivos desde el socket de datos
    List<String> files = new ArrayList<>();
    try (BufferedReader dataReader = new BufferedReader(new InputStreamReader(dataSocket.getInputStream()))) {
        String line;
        while ((line = dataReader.readLine()) != null) {
            files.add(line);
        }
    }
    dataSocket.close();

    // Leer la respuesta final del servidor después de cerrar el socket de datos
    readResponse();
    return files;
}

    private Socket createDataSocket(String response) throws IOException {
    try {
        // Extraer los números entre paréntesis de la respuesta PASV
        int start = response.indexOf('(');
        int end = response.indexOf(')');
        if (start == -1 || end == -1) {
            throw new IOException("Respuesta PASV inválida: " + response);
        }

        // Dividir los números en partes
        String[] parts = response.substring(start + 1, end).split(",");
        if (parts.length != 6) {
            throw new IOException("Formato inesperado en respuesta PASV: " + response);
        }

        // Construir la dirección IP
        String host = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
        int port = Integer.parseInt(parts[4]) * 256 + Integer.parseInt(parts[5]);

        // Crear y devolver el socket
        return new Socket(host, port);
    } catch (Exception e) {
        throw new IOException("Error al analizar la respuesta PASV: " + e.getMessage(), e);
    }
}


}
