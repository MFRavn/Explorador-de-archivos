import commands.*;
import core.FileSession;
import shell.Shell;

/**
 * Punto de entrada del gestor de archivos.
 *
 * Para compilar y ejecutar (desde la carpeta FileManager/):
 *
 *   Windows:
 *     javac -d out -sourcepath . Main.java shell\*.java commands\*.java core\*.java util\*.java
 *     java -cp out Main
 *
 *   Linux/macOS:
 *     javac -d out -sourcepath . Main.java shell/*.java commands/*.java core/*.java util/*.java
 *     java -cp out Main
 */
public class Main {

    public static void main(String[] args) {
        FileSession session = new FileSession();
        Shell shell = new Shell(session);

        // ── Navegación ──────────────────────────────
        shell.register(new ListCommand());
        shell.register(new ChangeDirCommand());
        shell.register(new PwdCommand());
        shell.register(new InfoCommand());

        // ── Operaciones de archivos ──────────────────
        shell.register(new CopyCommand());
        shell.register(new MoveCommand());

        // ── Búsqueda ────────────────────────────────
        shell.register(new SearchCommand());

        // ── Compresión ──────────────────────────────
        shell.register(new ZipCommand());

        // Arrancar el REPL
        shell.start();
    }
}
