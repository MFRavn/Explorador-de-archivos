package shell;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Lector de línea interactivo con historial y edición básica.
 *
 * Funcionalidades:
 *   ↑ / ↓          → navegar historial de comandos
 *   ← / →          → mover cursor dentro de la línea
 *   Home / End      → ir al inicio / final de la línea
 *   Backspace       → borrar carácter a la izquierda del cursor
 *   Delete (Supr)   → borrar carácter a la derecha del cursor
 *   Ctrl+C          → cancelar línea actual (devuelve null)
 *   Ctrl+L          → limpiar pantalla
 *
 * Nota sobre modo raw:
 *   En Windows Terminal y en Linux/macOS se detecta automáticamente
 *   el modo raw a través de stty (Unix) o conhost/VT (Windows).
 *   Si la terminal no soporta raw mode (ej: IDE embebido, pipe),
 *   cae back silenciosamente al Scanner estándar.
 */
public class LineReader {

    // ── Constantes de teclas ─────────────────────────────────────────
    private static final int BACKSPACE  = 127;
    private static final int DELETE_WIN = 8;   // Backspace en algunas configs Windows
    private static final int CTRL_C     = 3;
    private static final int CTRL_L     = 12;
    private static final int ENTER      = 13;
    private static final int ENTER_UNIX = 10;
    private static final int ESCAPE     = 27;

    private final List<String> history   = new ArrayList<>();
    private final int          maxHistory = 200;
    private final boolean      rawSupported;

    // Estado de la terminal para restaurar al salir
    private String originalSttyState = null;

    public LineReader() {
        this.rawSupported = detectRawSupport();
        if (rawSupported) {
            Runtime.getRuntime().addShutdownHook(new Thread(this::restoreTerminal));
        }
    }

    // ── API pública ──────────────────────────────────────────────────

    /**
     * Lee una línea del usuario mostrando el prompt dado.
     * Devuelve null si el usuario presiona Ctrl+C o si stdin se cierra.
     */
    public String readLine(String prompt) {
        if (!rawSupported) {
            return readLineFallback(prompt);
        }
        try {
            return readLineRaw(prompt);
        } catch (IOException e) {
            return null;
        }
    }

    /** Añade una entrada al historial (se llama desde Shell tras ejecutar un comando). */
    public void addToHistory(String line) {
        if (line == null || line.isBlank()) return;
        // No duplicar la última entrada
        if (!history.isEmpty() && history.get(history.size() - 1).equals(line)) return;
        history.add(line);
        if (history.size() > maxHistory) history.remove(0);
    }

    public List<String> getHistory() {
        return history;
    }

    // ── Lectura en modo raw ──────────────────────────────────────────

    private String readLineRaw(String prompt) throws IOException {
        enableRawMode();

        StringBuilder buffer   = new StringBuilder();
        int           cursor   = 0;   // posición del cursor dentro del buffer
        int           histIdx  = history.size(); // apunta "más allá del final" = línea actual
        String        savedLine = "";  // guarda la línea en edición cuando navegamos historial

        printPrompt(prompt);

        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(System.in, 1))) {

            while (true) {
                int b = in.read();
                if (b == -1) return null; // stdin cerrado

                // ── Enter ───────────────────────────────────────────
                if (b == ENTER || b == ENTER_UNIX) {
                    System.out.println();
                    return buffer.toString();
                }

                // ── Ctrl+C ──────────────────────────────────────────
                if (b == CTRL_C) {
                    System.out.println("^C");
                    return null;
                }

                // ── Ctrl+L (limpiar) ────────────────────────────────
                if (b == CTRL_L) {
                    System.out.print("\033[H\033[2J");
                    System.out.flush();
                    printPrompt(prompt);
                    printBuffer(buffer, cursor);
                    continue;
                }

                // ── Backspace ───────────────────────────────────────
                if (b == BACKSPACE || b == DELETE_WIN) {
                    if (cursor > 0) {
                        buffer.deleteCharAt(cursor - 1);
                        cursor--;
                        redrawLine(prompt, buffer, cursor);
                    }
                    continue;
                }

                // ── Secuencias de escape (flechas, Home, End, Delete) ─
                if (b == ESCAPE) {
                    int b2 = in.read();
                    if (b2 == '[') {
                        int b3 = in.read();

                        switch (b3) {
                            case 'A' -> { // Flecha ↑ — historial anterior
                                if (histIdx > 0) {
                                    if (histIdx == history.size()) savedLine = buffer.toString();
                                    histIdx--;
                                    buffer = new StringBuilder(history.get(histIdx));
                                    cursor = buffer.length();
                                    redrawLine(prompt, buffer, cursor);
                                }
                            }
                            case 'B' -> { // Flecha ↓ — historial siguiente
                                if (histIdx < history.size()) {
                                    histIdx++;
                                    String entry = histIdx == history.size()
                                            ? savedLine : history.get(histIdx);
                                    buffer = new StringBuilder(entry);
                                    cursor = buffer.length();
                                    redrawLine(prompt, buffer, cursor);
                                }
                            }
                            case 'C' -> { // Flecha →
                                if (cursor < buffer.length()) {
                                    cursor++;
                                    moveCursorRight(1);
                                }
                            }
                            case 'D' -> { // Flecha ←
                                if (cursor > 0) {
                                    cursor--;
                                    moveCursorLeft(1);
                                }
                            }
                            case 'H' -> { // Home
                                moveCursorLeft(cursor);
                                cursor = 0;
                            }
                            case 'F' -> { // End
                                moveCursorRight(buffer.length() - cursor);
                                cursor = buffer.length();
                            }
                            case '3' -> { // Delete (Supr) → ESC [ 3 ~
                                int b4 = in.read();
                                if (b4 == '~' && cursor < buffer.length()) {
                                    buffer.deleteCharAt(cursor);
                                    redrawLine(prompt, buffer, cursor);
                                }
                            }
                            case '1' -> { // Home alternativo → ESC [ 1 ~
                                int b4 = in.read();
                                if (b4 == '~') {
                                    moveCursorLeft(cursor);
                                    cursor = 0;
                                }
                            }
                            case '4' -> { // End alternativo → ESC [ 4 ~
                                int b4 = in.read();
                                if (b4 == '~') {
                                    moveCursorRight(buffer.length() - cursor);
                                    cursor = buffer.length();
                                }
                            }
                        }
                    }
                    continue;
                }

                // ── Carácter imprimible ─────────────────────────────
                if (b >= 32 && b < 127) {
                    buffer.insert(cursor, (char) b);
                    cursor++;
                    // Si el cursor está al final, simplemente imprimir
                    if (cursor == buffer.length()) {
                        System.out.print((char) b);
                        System.out.flush();
                    } else {
                        // Estamos en medio: redibujar desde el cursor
                        redrawLine(prompt, buffer, cursor);
                    }
                }
            }
        } finally {
            restoreTerminal();
        }
    }

    // ── Redibujo de línea ────────────────────────────────────────────

    private void printPrompt(String prompt) {
        System.out.print(prompt);
        System.out.flush();
    }

    private void printBuffer(StringBuilder buffer, int cursor) {
        System.out.print(buffer);
        // Mover cursor a su posición real (puede no estar al final)
        int back = buffer.length() - cursor;
        if (back > 0) moveCursorLeft(back);
        System.out.flush();
    }

    /**
     * Redibuja la línea completa desde la posición actual del cursor lógico.
     * Borra hasta el final, imprime el resto del buffer, y reposiciona el cursor.
     */
    private void redrawLine(String prompt, StringBuilder buffer, int cursor) {
        // Ir al inicio de la línea de entrada
        int promptLen = stripAnsi(prompt).length();
        System.out.print("\r");                     // inicio de línea física
        System.out.print(" ".repeat(promptLen + buffer.length() + 5)); // limpiar
        System.out.print("\r");
        System.out.print(prompt);
        System.out.print(buffer);
        // Reposicionar cursor
        int back = buffer.length() - cursor;
        if (back > 0) moveCursorLeft(back);
        System.out.flush();
    }

    private void moveCursorLeft(int n) {
        if (n > 0) { System.out.print("\033[" + n + "D"); System.out.flush(); }
    }

    private void moveCursorRight(int n) {
        if (n > 0) { System.out.print("\033[" + n + "C"); System.out.flush(); }
    }

    /** Elimina códigos ANSI de un string para calcular longitud visual real. */
    private String stripAnsi(String s) {
        return s.replaceAll("\033\\[[;\\d]*[A-Za-z]", "");
    }

    // ── Modo raw de terminal ─────────────────────────────────────────

    private boolean detectRawSupport() {
        // Solo activamos raw mode si stdin es una terminal real (no un pipe o IDE)
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            // En Windows comprobamos si hay consola activa
            return System.console() != null;
        }
        // En Unix comprobamos si stty está disponible
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"stty", "-a"});
            p.waitFor();
            return p.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private void enableRawMode() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return; // Windows Terminal maneja VT nativo

        try {
            // Guardar estado actual
            Process save = Runtime.getRuntime().exec(new String[]{"stty", "-g"});
            save.waitFor();
            originalSttyState = new String(save.getInputStream().readAllBytes()).trim();

            // Activar raw mode: -icanon (no buffering), -echo (no eco), min 1 char
            Runtime.getRuntime().exec(new String[]{"stty", "raw", "-echo"}).waitFor();
        } catch (Exception ignored) {}
    }

    private void restoreTerminal() {
        if (originalSttyState == null) return;
        try {
            Runtime.getRuntime().exec(new String[]{"stty", originalSttyState}).waitFor();
            originalSttyState = null;
        } catch (Exception ignored) {}
    }

    // ── Fallback (IDEs, pipes, terminals sin raw mode) ───────────────

    private String readLineFallback(String prompt) {
        System.out.print(prompt);
        System.out.flush();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }
}
