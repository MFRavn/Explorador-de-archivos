package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

/**
 * Comando: pwd
 * Imprime la ruta absoluta del directorio de trabajo actual.
 */
public class PwdCommand implements Command {

    @Override
    public void execute(String[] args, FileSession session) {
        System.out.println();
        System.out.println(Formatter.BOLD + "  " + session.getCurrentPath() + Formatter.RESET);
        System.out.println();
    }

    @Override
    public String[] names() {
        return new String[]{"pwd", "where", "cwd"};
    }

    @Override
    public String helpText() {
        return "pwd  →  Muestra la ruta del directorio actual";
    }
}
