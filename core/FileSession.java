package core;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Scanner;

/**
 * Mantiene el estado de la sesión activa del gestor de archivos.
 * Esta clase es la que la futura GUI también usará como "modelo".
 */
public class FileSession {

    private File currentDirectory;
    private final Deque<File> history = new ArrayDeque<>();

    /**
     * Scanner compartido para toda la sesión.
     * Los comandos que necesiten leer input del usuario (ej: confirmaciones)
     * usan este scanner en lugar de crear uno propio, evitando conflictos con stdin.
     */
    private Scanner scanner;

    public FileSession() {
        this.currentDirectory = new File(System.getProperty("user.home"));
    }

    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
    }

    public Scanner getScanner() {
        return scanner;
    }

    /**
     * Muestra un prompt de confirmación y devuelve true si el usuario responde s/S/y/Y.
     * Usado por DeleteCommand y cualquier operación destructiva futura.
     */
    public boolean confirm(String question) {
        System.out.print(question + " [s/N]: ");
        if (scanner == null || !scanner.hasNextLine()) return false;
        String answer = scanner.nextLine().trim().toLowerCase();
        return answer.equals("s") || answer.equals("y") || answer.equals("si") || answer.equals("yes");
    }

    /** Cambia el directorio actual y guarda el anterior en el historial. */
    public boolean changeDirectory(String path) {
        File target;

        if (path.equals("..")) {
            File parent = currentDirectory.getParentFile();
            if (parent == null) return false;
            target = parent;
        } else if (path.equals("~")) {
            target = new File(System.getProperty("user.home"));
        } else if (path.equals("-")) {
            if (history.isEmpty()) return false;
            target = history.peek();
        } else {
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
        if (history.size() > 50) history.pollLast();
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
