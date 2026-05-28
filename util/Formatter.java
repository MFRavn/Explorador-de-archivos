package util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utilidades de formato para la salida en terminal.
 * Centralizar aquí el formateo facilita el futuro rediseño para GUI.
 */
public class Formatter {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy  HH:mm");

    // Colores ANSI (solo en terminales que los soporten)
    public static final String RESET  = "\u001B[0m";
    public static final String BOLD   = "\u001B[1m";
    public static final String CYAN   = "\u001B[36m";
    public static final String YELLOW = "\u001B[33m";
    public static final String GREEN  = "\u001B[32m";
    public static final String DIM    = "\u001B[2m";
    public static final String RED    = "\u001B[31m";

    /**
     * Imprime la cabecera de la tabla de listado.
     */
    public static void printListHeader() {
        System.out.printf("%s%-6s  %-20s  %-14s  %s%s%n",
                BOLD + DIM, "TIPO", "FECHA MOD.", "TAMAÑO", "NOMBRE", RESET);
        System.out.println(DIM + "─".repeat(70) + RESET);
    }

    /**
     * Imprime una fila de la tabla para un archivo o directorio.
     */
    public static void printFileRow(File file) {
        boolean isDir = file.isDirectory();
        String type   = isDir ? "[DIR]" : "[   ]";
        String name   = isDir ? CYAN + BOLD + file.getName() + RESET
                               : file.getName();
        String size   = isDir ? "—" : formatSize(file.length());
        String date   = getModifiedDate(file);
        String color  = isDir ? CYAN : "";

        System.out.printf("%s%-6s%s  %-20s  %s%-14s%s  %s%n",
                color, type, RESET,
                date,
                DIM, size, RESET,
                name);
    }

    /**
     * Pie de tabla con resumen.
     */
    public static void printListFooter(int dirs, int files, long totalBytes) {
        System.out.println(DIM + "─".repeat(70) + RESET);
        System.out.printf(DIM + "  %d carpeta(s)   %d archivo(s)   %s total%s%n",
                dirs, files, formatSize(totalBytes), RESET);
    }

    /** Convierte bytes a formato legible (KB, MB, GB). */
    public static String formatSize(long bytes) {
        if (bytes < 1024)       return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private static String getModifiedDate(File file) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(
                    file.toPath(), BasicFileAttributes.class);
            return DATE_FORMAT.format(new Date(attrs.lastModifiedTime().toMillis()));
        } catch (Exception e) {
            return "—";
        }
    }

    /** Imprime un mensaje de error formateado. */
    public static void printError(String msg) {
        System.out.println(RED + "  ✗ Error: " + msg + RESET);
    }

    /** Imprime un mensaje de éxito. */
    public static void printSuccess(String msg) {
        System.out.println(GREEN + "  ✓ " + msg + RESET);
    }

    /** Imprime un mensaje informativo. */
    public static void printInfo(String msg) {
        System.out.println(DIM + "  " + msg + RESET);
    }
}
