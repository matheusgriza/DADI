package dadi;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
public class FilePersistence {
    public static void saveConfigurations(Map<String, List<FTPConfiguration>> userConfigs, String fileName) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(userConfigs);
        } catch (IOException e) {
            System.out.println("Error al guardar configuraciones: " + e.getMessage());
        }
    }

    public static Map<String, List<FTPConfiguration>> loadConfigurations(String fileName) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            return (Map<String, List<FTPConfiguration>>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Error al cargar configuraciones: " + e.getMessage());
            return new HashMap<>();
        }
    }
}
