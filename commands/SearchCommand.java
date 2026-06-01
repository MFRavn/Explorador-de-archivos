package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Comando: search <término> [opciones]
 *
 * Busca archivos y carpetas de forma recursiva desde el directorio actual.
 *
 * Opciones:
 *   -d <carpeta>   Buscar desde una carpeta específica en lugar del directorio actual
 *   -ext <ext>     Filtrar solo por extensión:   search -ext pdf
 *   -type f        Solo archivos
 *   -type d        Solo directorios
 *
 * Ejemplos:
 *   search informe               → busca todo lo que contenga "informe"
 *   search -ext pdf              → busca todos los PDFs
 *   search factura -type f       → busca archivos cuyo nombre contenga "factura"
 *   search logs -d C:\Proyectos  → busca "logs" dentro de C:\Proyectos
 */
public class SearchCommand implements Command {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private static final int MAX_RESULTS = 200;

    @Override
    public void execute(String[] args, FileSession session) {
        if (args.length == 0) {
            Formatter.printError("Uso: search <término> [-ext <extensión>] [-type f|d] [-d <ruta>]");
            Formatter.printInfo("Ejemplo: search informe -ext pdf");
            return;
        }

        // --- Parsear argumentos ---
        String term     = null;
        String ext      = null;
        String typeFilter = null; // "f" = files, "d" = dirs, null = ambos
        File   searchRoot = session.getCurrentDirectory();

        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "-ext":
                    if (i + 1 < args.length) ext = args[++i].toLowerCase().replace(".", "");
                    break;
                case "-type":
                    if (i + 1 < args.length) typeFilter = args[++i].toLowerCase();
                    break;
                case "-d":
                    if (i + 1 < args.length) {
                        File customRoot = new File(args[++i]);
                        if (!customRoot.isAbsolute()) customRoot = new File(session.getCurrentDirectory(), args[i]);
                        if (!customRoot.isDirectory()) {
                            Formatter.printError("La ruta de búsqueda no es válida: " + args[i]);
                            return;
                        }
                        searchRoot = customRoot;
                    }
                    break;
                default:
                    if (term == null) term = args[i]; // el primer arg sin flag es el término
            }
        }

        // Puede buscarse solo por extensión sin término de texto
        if (term == null && ext == null) {
            Formatter.printError("Debes indicar un término o una extensión (-ext).");
            return;
        }

        // --- Ejecutar búsqueda ---
        final String finalTerm       = term   != null ? term.toLowerCase()   : null;
        final String finalExt        = ext;
        final String finalTypeFilter = typeFilter;
        final List<File> results     = new ArrayList<>();
        final File finalSearchRoot   = searchRoot;

        System.out.println();
        System.out.printf(Formatter.DIM + "  Buscando en: %s%s%n%n", searchRoot.getAbsolutePath(), Formatter.RESET);

        try {
            Files.walkFileTree(searchRoot.toPath(), new SimpleFileVisitor<>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (results.size() >= MAX_RESULTS) return FileVisitResult.TERMINATE;

                    File f = file.toFile();

                    // Filtro de tipo
                    if ("d".equals(finalTypeFilter)) return FileVisitResult.CONTINUE; // solo dirs

                    if (matches(f.getName(), finalTerm, finalExt)) {
                        results.add(f);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (results.size() >= MAX_RESULTS) return FileVisitResult.TERMINATE;

                    File d = dir.toFile();
                    if (d.equals(finalSearchRoot)) return FileVisitResult.CONTINUE;

                    // Filtro de tipo
                    if (!"f".equals(finalTypeFilter)) {
                        if (matches(d.getName(), finalTerm, finalExt)) {
                            results.add(d);
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, java.io.IOException exc) {
                    // Ignorar rutas sin permisos
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (Exception e) {
            Formatter.printError("Error durante la búsqueda: " + e.getMessage());
            return;
        }

        // --- Mostrar resultados ---
        if (results.isEmpty()) {
            Formatter.printInfo("No se encontraron resultados.");
            System.out.println();
            return;
        }

        printResultsHeader();
        for (File f : results) {
            printResultRow(f, searchRoot);
        }

        System.out.println(Formatter.DIM + "  " + "─".repeat(72) + Formatter.RESET);
        String limitNote = results.size() >= MAX_RESULTS ? " (límite alcanzado)" : "";
        System.out.printf(Formatter.DIM + "  %d resultado(s)%s%s%n%n", results.size(), limitNote, Formatter.RESET);
    }

    private boolean matches(String fileName, String term, String ext) {
        String lower = fileName.toLowerCase();

        boolean termOk = term == null || lower.contains(term);
        boolean extOk  = ext  == null || lower.endsWith("." + ext);

        return termOk && extOk;
    }

    private void printResultsHeader() {
        System.out.printf("%s%-6s  %-16s  %-14s  %s%s%n",
                Formatter.BOLD + Formatter.DIM, "TIPO", "MODIFICADO", "TAMAÑO", "RUTA RELATIVA", Formatter.RESET);
        System.out.println(Formatter.DIM + "  " + "─".repeat(72) + Formatter.RESET);
    }

    private void printResultRow(File f, File root) {
        boolean isDir  = f.isDirectory();
        String  type   = isDir ? "[DIR]" : "[   ]";
        String  size   = isDir ? "—" : Formatter.formatSize(f.length());
        String  date   = DATE_FORMAT.format(new Date(f.lastModified()));
        String  relPath = getRelativePath(f, root);
        String  color  = isDir ? Formatter.CYAN + Formatter.BOLD : "";

        System.out.printf("  %s%-6s%s  %-16s  %s%-14s%s  %s%s%s%n",
                color, type, Formatter.RESET,
                date,
                Formatter.DIM, size, Formatter.RESET,
                color, relPath, Formatter.RESET);
    }

    private String getRelativePath(File f, File root) {
        try {
            String rel = root.toPath().relativize(f.toPath()).toString();
            return rel.isEmpty() ? f.getName() : rel;
        } catch (Exception e) {
            return f.getAbsolutePath();
        }
    }

    @Override
    public String[] names() {
        return new String[]{"search", "find", "buscar"};
    }

    @Override
    public String helpText() {
        return "search <término> [-ext <ext>] [-type f|d] [-d <ruta>]  →  Busca archivos";
    }
}
