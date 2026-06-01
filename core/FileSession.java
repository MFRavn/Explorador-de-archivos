package core;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Mantiene el estado de la sesión activa del gestor de archivos.
 * Esta clase es la que la futura GUI también usará como "modelo".
 */
public class FileSession {

    private File currentDirectory;
    private final Deque<File> history = new ArrayDeque<>();

    public FileSession() {
        // Arrancamos en el directorio home del usuario
        this.currentDirectory = new File(System.getProperty("user.home"));
    }

    /** Cambia el directorio actual y guarda el anterior en el historial. */
    public boolean changeDirectory(String path) {
        File target;

        if (path.equals("..")) {
            File parent = currentDirectory.getParentFile();
            if (parent == null) return false; // ya estamos en raíz
            target = parent;
        } else if (path.equals("~")) {
            target = new File(System.getProperty("user.home"));
        } else if (path.equals("-")) {
            // Volver al directorio anterior (como bash)
            if (history.isEmpty()) return false;
            target = history.peek();
        } else {
            // Ruta absoluta o relativa
            target = new File(path);
            if (!target.isAbsolute()) {
                target = new File(currentDirectory, path);
            }
        }

        try {
            target = target.getCanonicalFile();
        } catch (Exception e) {
            return false;
        }

        if (!target.exists() || !target.isDirectory()) return false;

        history.push(currentDirectory);
        if (history.size() > 50) history.pollLast(); // limitar historial
        currentDirectory = target;
        return true;
    }

    public File getCurrentDirectory() {
        return currentDirectory;
    }

    public String getCurrentPath() {
        return currentDirectory.getAbsolutePath();
    }

    public Deque<File> getHistory() {
        return history;
    }
}
