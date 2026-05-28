import commands.ChangeDirCommand;
import commands.InfoCommand;
import commands.ListCommand;
import commands.PwdCommand;
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

        // Registrar todos los comandos disponibles
        shell.register(new ListCommand());
        shell.register(new ChangeDirCommand());
        shell.register(new PwdCommand());
        shell.register(new InfoCommand());

        // Arrancar el REPL
        shell.start();
    }
}
