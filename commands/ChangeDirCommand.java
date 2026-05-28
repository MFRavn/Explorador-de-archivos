package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

/**
 * Comando: cd <ruta>
 * Cambia el directorio de trabajo actual.
 *
 * Casos especiales:
 *   cd ..   → directorio padre
 *   cd ~    → directorio home del usuario
 *   cd -    → directorio anterior (como en bash)
 */
public class ChangeDirCommand implements Command {

    @Override
    public void execute(String[] args, FileSession session) {
        if (args.length == 0) {
            // Sin argumentos → ir al home (comportamiento Unix estándar)
            session.changeDirectory("~");
            Formatter.printInfo("→ " + session.getCurrentPath());
            return;
        }

        String path = String.join(" ", args);
        boolean ok = session.changeDirectory(path);

        if (ok) {
            Formatter.printInfo("→ " + session.getCurrentPath());
        } else {
            Formatter.printError("No se puede acceder a: \"" + path + "\"");
            Formatter.printInfo("Verifica que la ruta exista y tengas permisos.");
        }
    }

    @Override
    public String[] names() {
        return new String[]{"cd", "chdir"};
    }

    @Override
    public String helpText() {
        return "cd <ruta>  →  Cambia el directorio actual  |  cd .. / cd ~ / cd -";
    }
}
