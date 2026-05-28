package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Comando: ls [ruta]
 * Lista el contenido del directorio actual o de la ruta indicada.
 * Muestra primero las carpetas, luego los archivos, ambos en orden alfabético.
 */
public class ListCommand implements Command {

    @Override
    public void execute(String[] args, FileSession session) {
        File dir;

        if (args.length == 0) {
            dir = session.getCurrentDirectory();
        } else {
            String path = String.join(" ", args); // soporte para rutas con espacios
            dir = new File(path);
            if (!dir.isAbsolute()) {
                dir = new File(session.getCurrentDirectory(), path);
            }
        }

        if (!dir.exists()) {
            Formatter.printError("La ruta no existe: " + dir.getAbsolutePath());
            return;
        }
        if (!dir.isDirectory()) {
            Formatter.printError("No es un directorio: " + dir.getAbsolutePath());
            return;
        }

        File[] entries = dir.listFiles();
        if (entries == null) {
            Formatter.printError("No se puede leer el directorio (permisos insuficientes).");
            return;
        }

        // Ordenar: primero directorios, luego archivos; ambos alfabéticamente
        Arrays.sort(entries, Comparator
                .comparing((File f) -> !f.isDirectory()) // false < true → dirs primero
                .thenComparing(f -> f.getName().toLowerCase()));

        System.out.println();
        System.out.println(Formatter.BOLD + "  " + dir.getAbsolutePath() + Formatter.RESET);
        System.out.println();
        Formatter.printListHeader();

        int dirCount = 0, fileCount = 0;
        long totalBytes = 0;

        for (File entry : entries) {
            // Saltar archivos ocultos en Windows (empieza por '.' no se oculta igual,
            // pero sí los que tienen atributo hidden)
            if (entry.isHidden()) continue;

            Formatter.printFileRow(entry);

            if (entry.isDirectory()) dirCount++;
            else {
                fileCount++;
                totalBytes += entry.length();
            }
        }

        Formatter.printListFooter(dirCount, fileCount, totalBytes);
        System.out.println();
    }

    @Override
    public String[] names() {
        return new String[]{"ls", "dir", "list"};
    }

    @Override
    public String helpText() {
        return "ls [ruta]  →  Lista el contenido de un directorio";
    }
}
