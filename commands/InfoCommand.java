package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Comando: info <nombre>
 * Muestra información detallada de un archivo o directorio:
 * tipo, tamaño, fechas de creación/modificación, permisos, ruta completa.
 */
public class InfoCommand implements Command {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy  HH:mm:ss");

    @Override
    public void execute(String[] args, FileSession session) {
        if (args.length == 0) {
            Formatter.printError("Uso: info <nombre_archivo_o_carpeta>");
            return;
        }

        String name = String.join(" ", args);
        File file = new File(name);
        if (!file.isAbsolute()) {
            file = new File(session.getCurrentDirectory(), name);
        }

        if (!file.exists()) {
            Formatter.printError("No existe: " + name);
            return;
        }

        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);

            System.out.println();
            printRow("Nombre",     file.getName());
            printRow("Tipo",       file.isDirectory() ? "Directorio" : "Archivo");
            printRow("Ruta",       file.getAbsolutePath());
            printRow("Tamaño",     file.isDirectory()
                                       ? calcDirSize(file) + " (directorio)"
                                       : Formatter.formatSize(file.length())
                                         + "  (" + file.length() + " bytes)");
            printRow("Creado",     DATE_FORMAT.format(new Date(attrs.creationTime().toMillis())));
            printRow("Modificado", DATE_FORMAT.format(new Date(attrs.lastModifiedTime().toMillis())));
            printRow("Permisos",   buildPermString(file));
            printRow("Oculto",     file.isHidden() ? "Sí" : "No");

            if (file.isDirectory()) {
                File[] children = file.listFiles();
                int count = children != null ? children.length : 0;
                printRow("Contenido", count + " elemento(s) en primer nivel");
            }

            System.out.println();

        } catch (Exception e) {
            Formatter.printError("No se pudo leer la información: " + e.getMessage());
        }
    }

    private void printRow(String label, String value) {
        System.out.printf("  %s%-14s%s  %s%n",
                Formatter.DIM + Formatter.BOLD, label + ":", Formatter.RESET, value);
    }

    private String buildPermString(File f) {
        return (f.canRead()    ? "Lectura "  : "") +
               (f.canWrite()   ? "Escritura " : "") +
               (f.canExecute() ? "Ejecución" : "");
    }

    /** Calcula el tamaño total de un directorio de forma recursiva. */
    private String calcDirSize(File dir) {
        long total = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                total += f.isDirectory() ? 0 : f.length();
            }
        }
        return Formatter.formatSize(total) + " (nivel raíz)";
    }

    @Override
    public String[] names() {
        return new String[]{"info", "stat"};
    }

    @Override
    public String helpText() {
        return "info <nombre>  →  Información detallada de un archivo o carpeta";
    }
}
