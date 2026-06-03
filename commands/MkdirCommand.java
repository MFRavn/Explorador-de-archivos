package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

import java.io.File;

/**
 * Comando: mkdir <nombre> [nombre2 ...]
 *
 * Crea uno o varios directorios. Soporta rutas anidadas (equivalente a mkdir -p).
 * Si el directorio ya existe, lo indica sin fallar.
 *
 * Ejemplos:
 *   mkdir Proyectos
 *   mkdir Proyectos/Java/src          ← crea toda la cadena si no existe
 *   mkdir docs tests resources        ← varios a la vez
 */
public class MkdirCommand implements Command {

    @Override
    public void execute(String[] args, FileSession session) {
        if (args.length == 0) {
            Formatter.printError("Uso: mkdir <nombre> [nombre2 ...]");
            Formatter.printInfo("Ejemplo: mkdir Proyectos/Java/src");
            return;
        }

        System.out.println();
        int created = 0;

        for (String arg : args) {
            File dir = new File(arg);
            if (!dir.isAbsolute()) {
                dir = new File(session.getCurrentDirectory(), arg);
            }

            if (dir.exists()) {
                if (dir.isDirectory()) {
                    Formatter.printInfo("Ya existe: " + arg);
                } else {
                    Formatter.printError("Existe un archivo con ese nombre: " + arg);
                }
                continue;
            }

            // mkdirs() crea toda la cadena de directorios intermedios
            if (dir.mkdirs()) {
                Formatter.printSuccess("Creado: " + dir.getAbsolutePath());
                created++;
            } else {
                Formatter.printError("No se pudo crear: " + arg
                        + " (comprueba permisos o la ruta)");
            }
        }

        if (args.length > 1) {
            System.out.println();
            Formatter.printInfo(created + " de " + args.length + " directorio(s) creado(s).");
        }

        System.out.println();
    }

    @Override
    public String[] names() {
        return new String[]{"mkdir", "md", "newdir"};
    }

    @Override
    public String helpText() {
        return "mkdir <nombre> [...]  →  Crea uno o varios directorios (soporta rutas anidadas)";
    }
}
