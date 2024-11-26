package dadi;
import java.util.*;

public class MenuUsuario {
    private final UserManager userManager;
    private final ConfigurationManager configManager;
    private final String username;
    private final Scanner scanner;

    public MenuUsuario(UserManager userManager, String username) {
        this.userManager = userManager;
        this.configManager = new ConfigurationManager();
        this.username = username;
        this.scanner = new Scanner(System.in);
    }

    public void mostrarMenuAdministrador() {
        while (true) {
            System.out.println("=== Menú Administrador ===");
            System.out.println("1. Crear un nuevo usuario");
            System.out.println("2. Configurar servidor FTP");
            System.out.println("3. Conectar y listar archivos FTP");
            System.out.println("4. Cerrar sesión");
            System.out.print("Seleccione una opción: ");
            int opcion = scanner.nextInt();
            scanner.nextLine();

            switch (opcion) {
                case 1:
                    crearUsuario();
                    break;
                case 2:
                    configurarFTP();
                    break;
                case 3:
                    conectarYListarFTP();
                    break;
                case 4:
                    System.out.println("Cerrando sesión...");
                    return;
                default:
                    System.out.println("Opción no válida. Intente de nuevo.");
            }
        }
    }

    public void mostrarMenuUsuario() {
        while (true) {
            System.out.println("=== Menú Usuario ===");
            System.out.println("1. Configurar servidor FTP");
            System.out.println("2. Conectar y listar archivos FTP");
            System.out.println("3. Cerrar sesión");
            System.out.print("Seleccione una opción: ");
            int opcion = scanner.nextInt();
            scanner.nextLine();

            switch (opcion) {
                case 1:
                    configurarFTP();
                    break;
                case 2:
                    conectarYListarFTP();
                    break;
                case 3:
                    System.out.println("Cerrando sesión...");
                    return;
                default:
                    System.out.println("Opción no válida. Intente de nuevo.");
            }
        }
    }

    private void crearUsuario() {
        try {
            System.out.print("Ingrese el nombre del nuevo usuario: ");
            String newUsername = scanner.nextLine();
            System.out.print("Ingrese la contraseña del nuevo usuario: ");
            String newPassword = scanner.nextLine();
            System.out.println("Seleccione el rol del usuario: (1. Administrador, 2. Cliente)");
            int roleOption = scanner.nextInt();
            scanner.nextLine();

            Role role = (roleOption == 1) ? Role.ADMIN : Role.CLIENT;
            userManager.createUser(username, newUsername, newPassword, role);
            System.out.println("Usuario creado exitosamente.");
        } catch (Exception e) {
            System.out.println("Error al crear usuario: " + e.getMessage());
        }
    }

    private void configurarFTP() {
        System.out.print("Ingrese la dirección del servidor FTP: ");
        String serverAddress = scanner.nextLine();
        System.out.print("Ingrese el puerto del servidor FTP: ");
        int port = scanner.nextInt();
        scanner.nextLine();
        System.out.print("Ingrese el nombre de usuario del servidor FTP: ");
        String ftpUsername = scanner.nextLine();
        System.out.print("Ingrese la contraseña del servidor FTP: ");
        String ftpPassword = scanner.nextLine();

        FTPConfiguration config = new FTPConfiguration(serverAddress, port, ftpUsername, ftpPassword);
        configManager.addConfiguration(username, config);
        System.out.println("Configuración de servidor FTP guardada exitosamente.");
    }
    
    private void conectarYListarFTP() {
            List<FTPConfiguration> configs = configManager.getConfigurations(username);
            if (configs.isEmpty()) {
                System.out.println("No tiene configuraciones de servidor FTP. Por favor configure una primero.");
                return;
            }

            System.out.println("Seleccione un servidor para conectar:");
            for (int i = 0; i < configs.size(); i++) {
                FTPConfiguration config = configs.get(i);
                System.out.printf("%d. %s:%d (%s)%n", i + 1, config.getServerAddress(), config.getPort(), config.getUsername());
            }
            int opcion = scanner.nextInt();
            scanner.nextLine();

            if (opcion < 1 || opcion > configs.size()) {
                System.out.println("Opción no válida.");
                return;
            }

            FTPConfiguration selectedConfig = configs.get(opcion - 1);
            try {
                FTPConnectionManager ftpManager = new FTPConnectionManager();
                ftpManager.connect(selectedConfig.getServerAddress(), selectedConfig.getPort());
                ftpManager.login(selectedConfig.getUsername(), selectedConfig.getPassword());

                System.out.println("Archivos en el directorio raíz:");
                ftpManager.listFiles();

                mostrarMenuFTP(ftpManager);
                ftpManager.disconnect();
            } catch (Exception e) {
                System.out.println("Error al conectar al servidor FTP: " + e.getMessage());
            }
    }

    private void mostrarMenuFTP(FTPConnectionManager ftpManager) {
        while (true) {
            System.out.println("=== Menú FTP ===");
            System.out.println("1. Cambiar de directorio");
            System.out.println("2. Listar archivos en el directorio actual");
            System.out.println("3. Descargar archivo");
            System.out.println("4. Subir archivo");
            System.out.println("5. Crear directorio");
            System.out.println("6. Eliminar directorio");
            System.out.println("7. Renombrar archivo o carpeta");
            System.out.println("8. Salir del menú FTP");
            System.out.print("Seleccione una opción: ");

            int opcion = scanner.nextInt();
            scanner.nextLine();

            try {
                switch (opcion) {
                    case 1: // Cambiar de directorio
                        System.out.print("Ingrese el nombre del directorio: ");
                        String directory = scanner.nextLine();
                        ftpManager.changeDirectory(directory);
                        break;

                    case 2: // Listar archivos
                        
                        try {
                                ftpManager.listFiles();
                            } catch (Exception e) {
                                System.out.println("Error al listar archivos: " + e.getMessage());
                            }
                            break;

                    case 3: // Descargar archivo
                        System.out.print("Ingrese la ruta del archivo remoto: ");
                        String remoteFilePath = scanner.nextLine();
                        System.out.print("Ingrese la ruta local donde se guardará: ");
                        String localFilePath = scanner.nextLine();
                        ftpManager.downloadFile(remoteFilePath, localFilePath);
                        break;

                    case 4: // Subir archivo
                        System.out.print("Ingrese la ruta del archivo local: ");
                        localFilePath = scanner.nextLine();
                        System.out.print("Ingrese la ruta remota donde se subirá: ");
                        remoteFilePath = scanner.nextLine();
                        ftpManager.uploadFile(localFilePath, remoteFilePath);
                        break;

                    case 5: // Crear directorio
                         System.out.print("Ingrese el nombre del nuevo directorio: ");
                         String newDirectory = scanner.nextLine();
                         try {
                             ftpManager.createDirectory(newDirectory);
                             ftpManager.changeDirectory(newDirectory); // Cambia al nuevo directorio
                         } catch (Exception e) {
                             System.out.println("Error al crear el directorio: " + e.getMessage());
                         }
                         break;

                    case 6: // Eliminar directorio
                        System.out.print("Ingrese el nombre del directorio a eliminar: ");
                        String directoryToDelete = scanner.nextLine();
                        ftpManager.deleteDirectory(directoryToDelete);
                        break;

                    case 7: // Renombrar archivo o carpeta
                        System.out.print("Ingrese el nombre actual del archivo/carpeta: ");
                        String currentName = scanner.nextLine();
                        System.out.print("Ingrese el nuevo nombre: ");
                        String newName = scanner.nextLine();
                        ftpManager.rename(currentName, newName);
                        break;

                    case 8: // Salir del menú FTP
                        System.out.println("Saliendo del menú FTP...");
                        return;

                    default:
                        System.out.println("Opción no válida. Intente de nuevo.");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    
}
