package dadi;
import java.util.Scanner;

public class MenuPrincipal {
    private final UserManager userManager;
    private final Scanner scanner;

    public MenuPrincipal() {
        this.userManager = new UserManager();
        this.scanner = new Scanner(System.in);
    }

    public void mostrarMenu() {
        System.out.println("=== Bienvenido a la aplicación FTP ===");
        while (true) {
            System.out.println("1. Iniciar sesión");
            System.out.println("2. Salir");
            System.out.print("Seleccione una opción: ");
            int opcion = scanner.nextInt();
            scanner.nextLine(); // Consumir la línea nueva.

            switch (opcion) {
                case 1:
                    iniciarSesion();
                    break;
                case 2:
                    System.out.println("¡Gracias por usar la aplicación!");
                    return;
                default:
                    System.out.println("Opción no válida. Intente de nuevo.");
            }
        }
    }

    private void iniciarSesion() {
        System.out.print("Ingrese su nombre de usuario: ");
        String username = scanner.nextLine();
        System.out.print("Ingrese su contraseña: ");
        String password = scanner.nextLine();

        if (userManager.authenticate(username, password)) {
            System.out.println("Inicio de sesión exitoso.");
            MenuUsuario menuUsuario = new MenuUsuario(userManager, username);
            if (userManager.isAdmin(username)) {
                menuUsuario.mostrarMenuAdministrador();
            } else {
                menuUsuario.mostrarMenuUsuario();
            }
        } else {
            System.out.println("Credenciales incorrectas.");
        }
    }
}
