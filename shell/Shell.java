package shell;

import core.FileSession;
import util.Formatter;

import java.util.*;

/**
 * Núcleo del shell interactivo.
 * Registra los comandos disponibles, lee input del usuario y los despacha.
 */
public class Shell {

    private final FileSession session;
    private final Map<String, Command> commandMap = new LinkedHashMap<>();
    private final LineReader lineReader = new LineReader();
    private boolean running = true;

    public Shell(FileSession session) {
        this.session = session;
    }

    public void register(Command cmd) {
        for (String name : cmd.names()) {
            commandMap.put(name.toLowerCase(), cmd);
        }
    }

    public void start() {
        printBanner();

        // Pasar un Scanner de fallback a la sesión para los comandos que
        // necesiten confirmación (delete, etc.) — sigue funcionando aunque
        // LineReader tome el control del input principal.
        session.setScanner(new Scanner(System.in));

        while (running) {
            String line = lineReader.readLine(buildPrompt());

            if (line == null) {
                // Ctrl+C o stdin cerrado → salir limpiamente
                running = false;
                break;
            }

            line = line.trim();
            if (line.isEmpty()) continue;

            lineReader.addToHistory(line);
            processLine(line);
        }

        System.out.println(Formatter.DIM + "\n  Hasta luego.\n" + Formatter.RESET);
    }

    private void processLine(String line) {
        List<String> parts = tokenize(line);
        if (parts.isEmpty()) return;

        String cmdName = parts.get(0).toLowerCase();
        String[] args  = parts.subList(1, parts.size()).toArray(new String[0]);

        switch (cmdName) {
            case "exit", "quit", "salir" -> running = false;
            case "help", "?", "ayuda"    -> printHelp();
            case "cls", "clear"          -> clearScreen();
            case "history", "hist"       -> printHistory();
            default -> {
                Command cmd = commandMap.get(cmdName);
                if (cmd != null) {
                    cmd.execute(args, session);
                } else {
                    Formatter.printError("Comando desconocido: \"" + cmdName + "\"");
                    Formatter.printInfo("Escribe 'help' para ver los comandos disponibles.");
                }
            }
        }
    }

    // ── Prompt ───────────────────────────────────────────────────────

    private String buildPrompt() {
        String path = session.getCurrentPath();
        String home = System.getProperty("user.home");
        if (path.startsWith(home)) path = "~" + path.substring(home.length());
        path = path.replace('\\', '/');

        return Formatter.CYAN + Formatter.BOLD + "fm" + Formatter.RESET
             + Formatter.DIM + ":" + Formatter.RESET
             + Formatter.YELLOW + path + Formatter.RESET
             + " › ";
    }

    // ── Comandos built-in ────────────────────────────────────────────

    private void printBanner() {
        System.out.println();
        System.out.println(Formatter.BOLD + Formatter.CYAN + "  ╔══════════════════════════════════╗" + Formatter.RESET);
        System.out.println(Formatter.BOLD + Formatter.CYAN + "  ║       FILE MANAGER  v1.0         ║" + Formatter.RESET);
        System.out.println(Formatter.BOLD + Formatter.CYAN + "  ╚══════════════════════════════════╝" + Formatter.RESET);
        System.out.println();
        System.out.println(Formatter.DIM + "  Escribe 'help' para ver los comandos disponibles." + Formatter.RESET);
        System.out.println(Formatter.DIM + "  Directorio inicial: " + session.getCurrentPath() + Formatter.RESET);
        System.out.println();
    }

    private void printHelp() {
        System.out.println();
        System.out.println(Formatter.BOLD + "  Comandos disponibles:" + Formatter.RESET);
        System.out.println(Formatter.DIM + "  " + "─".repeat(60) + Formatter.RESET);

        Set<Command> seen = new LinkedHashSet<>(commandMap.values());
        for (Command cmd : seen) {
            System.out.println("  " + Formatter.GREEN + cmd.helpText() + Formatter.RESET);
        }

        System.out.println("  " + Formatter.GREEN + "history  →  Muestra el historial de comandos" + Formatter.RESET);
        System.out.println("  " + Formatter.GREEN + "help     →  Muestra este menú" + Formatter.RESET);
        System.out.println("  " + Formatter.GREEN + "cls      →  Limpia la pantalla" + Formatter.RESET);
        System.out.println("  " + Formatter.GREEN + "exit     →  Cierra el gestor" + Formatter.RESET);
        System.out.println(Formatter.DIM + "  " + "─".repeat(60) + Formatter.RESET);
        System.out.println();
    }

    private void printHistory() {
        List<String> hist = lineReader.getHistory();
        if (hist.isEmpty()) {
            Formatter.printInfo("El historial está vacío.");
            return;
        }
        System.out.println();
        int start = Math.max(0, hist.size() - 50); // mostrar últimas 50 entradas
        for (int i = start; i < hist.size(); i++) {
            System.out.printf("  %s%3d%s  %s%n",
                    Formatter.DIM, i + 1, Formatter.RESET, hist.get(i));
        }
        System.out.println();
    }

    private void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    // ── Tokenizador ──────────────────────────────────────────────────

    private List<String> tokenize(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) tokens.add(current.toString());
        return tokens;
    }
}
