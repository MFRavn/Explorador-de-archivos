package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Comando: tree [ruta] [-d] [-L <profundidad>]
 *
 * Muestra la estructura de directorios en forma de árbol visual,
 * similar al comando 'tree' de Windows y Linux.
 *
 * Opciones:
 *   -d          → mostrar solo directorios, sin archivos
 *   -L <n>      → limitar la profundidad máxima (por defecto: 4)
 *
 * Ejemplos:
 *   tree
 *   tree Documentos/
 *   tree -d
 *   tree Proyectos/ -L 2
 */
public class TreeCommand implements Command {

    private static final int DEFAULT_MAX_DEPTH = 4;
    private static final int MAX_ENTRIES       = 500; // evitar colgarse en árboles enormes

    // Caracteres del árbol (estilo Unicode, igual que el tree de Linux)
    private static final String BRANCH     = "├── ";
    private static final String LAST       = "└── ";
    private static final String VERTICAL   = "│   ";
    private static final String EMPTY      = "    ";

    @Override
    public void execute(String[] args, FileSession session) {
        File   root     = session.getCurrentDirectory();
        int    maxDepth = DEFAULT_MAX_DEPTH;
        boolean dirsOnly = false;

        // Parsear argumentos
        for (int i = 0; i < args.length; i++) {
            switch (args[i].toLowerCase()) {
                case "-d" -> dirsOnly = true;
                case "-l" -> {
                    if (i + 1 < args.length) {
                        try {
                            maxDepth = Integer.parseInt(args[++i]);
                            if (maxDepth < 1) maxDepth = 1;
                            if (maxDepth > 10) maxDepth = 10;
                        } catch (NumberFormatException e) {
                            Formatter.printError("Profundidad inválida: " + args[i]);
                            return;
                        }
                    }
                }
                default -> {
                    File customRoot = new File(args[i]);
                    if (!customRoot.isAbsolute()) {
                        customRoot = new File(session.getCurrentDirectory(), args[i]);
                    }
                    if (!customRoot.exists() || !customRoot.isDirectory()) {
                        Formatter.printError("Ruta no válida: " + args[i]);
                        return;
                    }
                    root = customRoot;
                }
            }
        }

        System.out.println();
        System.out.println(Formatter.BOLD + "  " + root.getAbsolutePath() + Formatter.RESET);

        int[] counters = {0, 0, 0}; // [dirs, files, total entries printed]
        boolean[] limitReached = {false};

        printTree(root, "", maxDepth, 0, dirsOnly, counters, limitReached);

        System.out.println();
        if (limitReached[0]) {
            System.out.println(Formatter.YELLOW + "  ⚠ Límite de " + MAX_ENTRIES
                    + " entradas alcanzado. Usa -L para reducir la profundidad." + Formatter.RESET);
        }
        System.out.printf(Formatter.DIM + "  %d directorio(s)%s%s%n%n",
                counters[0],
                dirsOnly ? "" : ", " + counters[1] + " archivo(s)",
                Formatter.RESET);
    }

    private void printTree(File dir, String prefix, int maxDepth, int depth,
                           boolean dirsOnly, int[] counters, boolean[] limitReached) {

        if (limitReached[0]) return;

        File[] entries = dir.listFiles();
        if (entries == null) return; // sin permisos

        // Ordenar: carpetas primero, luego archivos, ambos alfabéticamente
        Arrays.sort(entries, Comparator
                .comparing((File f) -> !f.isDirectory())
                .thenComparing(f -> f.getName().toLowerCase()));

        // Filtrar ocultos y, si -d, solo directorios
        entries = Arrays.stream(entries)
                .filter(f -> !f.isHidden())
                .filter(f -> !dirsOnly || f.isDirectory())
                .toArray(File[]::new);

        for (int i = 0; i < entries.length; i++) {
            if (limitReached[0]) return;
            if (counters[2]++ >= MAX_ENTRIES) {
                limitReached[0] = true;
                return;
            }

            File    entry  = entries[i];
            boolean isLast = (i == entries.length - 1);
            boolean isDir  = entry.isDirectory();

            String connector = isLast ? LAST : BRANCH;
            String name      = isDir
                    ? Formatter.CYAN + Formatter.BOLD + entry.getName() + Formatter.RESET
                    : entry.getName();
            String sizeHint  = isDir ? "" : Formatter.DIM + "  " + Formatter.formatSize(entry.length()) + Formatter.RESET;

            System.out.println("  " + prefix + connector + name + sizeHint);

            if (isDir) {
                counters[0]++;
                if (depth + 1 < maxDepth) {
                    String newPrefix = prefix + (isLast ? EMPTY : VERTICAL);
                    printTree(entry, newPrefix, maxDepth, depth + 1, dirsOnly, counters, limitReached);
                }
            } else {
                counters[1]++;
            }
        }
    }

    @Override
    public String[] names() {
        return new String[]{"tree"};
    }

    @Override
    public String helpText() {
        return "tree [ruta] [-d] [-L <n>]  →  Muestra estructura de carpetas en árbol";
    }
}
