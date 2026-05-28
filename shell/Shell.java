package shell;

import core.FileSession;
import util.Formatter;

import java.util.*;

/**
 * Núcleo del shell interactivo.
 * Registra los comandos disponibles, lee input del usuario y los despacha.
 *
 * Para añadir un nuevo comando en el futuro:
 *   1. Crear la clase en commands/ implementando Command
 *   2. Registrarla aquí con register(new MiComando())
 */
public class Shell {

    private final FileSession session;
    private final Map<String, Command> commandMap = new LinkedHashMap<>();
    private boolean running = true;

    public Shell(FileSession session) {
        this.session = session;
    }

    /** Registra un comando y todos sus alias. */
    public void register(Command cmd) {
        for (String name : cmd.names()) {
            commandMap.put(name.toLowerCase(), cmd);
        }
    }

    /** Inicia el bucle principal del REPL. */
    public void start() {
        printBanner();
        Scanner scanner = new Scanner(System.in);

        while (running) {
            System.out.print(buildPrompt());
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) continue;

            processLine(line);
        }

        scanner.close();
        System.out.println(Formatter.DIM + "\n  Hasta luego.\n" + Formatter.RESET);
    }

    private void processLine(String line) {
        // Separar comando y argumentos respetando comillas
        List<String> parts = tokenize(line);
        if (parts.isEmpty()) return;

        String cmdName = parts.get(0).toLowerCase();
        String[] args  = parts.subList(1, parts.size()).toArray(new String[0]);

        switch (cmdName) {
            case "exit":
            case "quit":
            case "salir":
                running = false;
                break;

            case "help":
            case "?":
            case "ayuda":
                printHelp();
                break;

            case "cls":
            case "clear":
                clearScreen();
                break;

            default:
                Command cmd = commandMap.get(cmdName);
                if (cmd != null) {
                    cmd.execute(args, session);
                } else {
                    Formatter.printError("Comando desconocido: \"" + cmdName + "\"");
                    Formatter.printInfo("Escribe 'help' para ver los comandos disponibles.");
                }
        }
    }

    /** Construye el prompt con el directorio actual. */
    private String buildPrompt() {
        String path = session.getCurrentPath();

        // Acortar el home a ~ igual que bash
        String home = System.getProperty("user.home");
        if (path.startsWith(home)) {
            path = "~" + path.substring(home.length());
        }

        // En Windows usar / en lugar de \ para uniformidad visual
        path = path.replace('\\', '/');

        return Formatter.CYAN + Formatter.BOLD + "fm" + Formatter.RESET
             + Formatter.DIM + ":" + Formatter.RESET
             + Formatter.YELLOW + path + Formatter.RESET
             + " › ";
    }

    private void printBanner() {
        System.out.println();
        System.out.println(Formatter.BOLD + Formatter.CYAN
                + "  ╔══════════════════════════════════╗" + Formatter.RESET);
        System.out.println(Formatter.BOLD + Formatter.CYAN
                + "  ║       FILE MANAGER  v1.0         ║" + Formatter.RESET);
        System.out.println(Formatter.BOLD + Formatter.CYAN
                + "  ╚══════════════════════════════════╝" + Formatter.RESET);
        System.out.println();
        System.out.println(Formatter.DIM + "  Escribe 'help' para ver los comandos disponibles." + Formatter.RESET);
        System.out.println(Formatter.DIM + "  Directorio inicial: " + session.getCurrentPath() + Formatter.RESET);
        System.out.println();
    }

    private void printHelp() {
        System.out.println();
        System.out.println(Formatter.BOLD + "  Comandos disponibles:" + Formatter.RESET);
        System.out.println(Formatter.DIM + "  " + "─".repeat(60) + Formatter.RESET);

        // Evitar duplicar comandos con alias
        Set<Command> seen = new LinkedHashSet<>(commandMap.values());
        for (Command cmd : seen) {
            System.out.println("  " + Formatter.GREEN + cmd.helpText() + Formatter.RESET);
        }

        System.out.println("  " + Formatter.GREEN + "help  →  Muestra este menú" + Formatter.RESET);
        System.out.println("  " + Formatter.GREEN + "cls   →  Limpia la pantalla" + Formatter.RESET);
        System.out.println("  " + Formatter.GREEN + "exit  →  Cierra el gestor" + Formatter.RESET);
        System.out.println(Formatter.DIM + "  " + "─".repeat(60) + Formatter.RESET);
        System.out.println();
    }

    private void clearScreen() {
        // Secuencia ANSI para limpiar pantalla + ir al inicio
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Tokenizador simple que respeta cadenas entre comillas.
     * Ejemplo: cd "Mi carpeta con espacios"  →  ["cd", "Mi carpeta con espacios"]
     */
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
