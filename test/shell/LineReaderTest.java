package shell;

import org.junit.jupiter.api.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests del historial de LineReader (lo que se puede testear sin terminal real).
 */
@DisplayName("LineReader - historial")
class LineReaderTest {

    private LineReader reader;

    @BeforeEach
    void setUp() {
        reader = new LineReader();
    }

    @Test
    @DisplayName("historial vacío al crear")
    void history_emptyOnCreation() {
        assertTrue(reader.getHistory().isEmpty());
    }

    @Test
    @DisplayName("addToHistory añade entradas")
    void addToHistory_addsEntries() {
        reader.addToHistory("ls");
        reader.addToHistory("cd Documentos");
        assertEquals(2, reader.getHistory().size());
    }

    @Test
    @DisplayName("no se añaden entradas vacías ni en blanco")
    void addToHistory_ignoresBlankLines() {
        reader.addToHistory("");
        reader.addToHistory("   ");
        reader.addToHistory(null);
        assertTrue(reader.getHistory().isEmpty());
    }

    @Test
    @DisplayName("no se duplica la última entrada consecutiva")
    void addToHistory_noDuplicateConsecutive() {
        reader.addToHistory("ls");
        reader.addToHistory("ls");
        assertEquals(1, reader.getHistory().size());
    }

    @Test
    @DisplayName("sí se guarda el mismo comando si no es consecutivo")
    void addToHistory_allowsNonConsecutiveDuplicate() {
        reader.addToHistory("ls");
        reader.addToHistory("cd ..");
        reader.addToHistory("ls");
        assertEquals(3, reader.getHistory().size());
    }

    @Test
    @DisplayName("historial se limita a 200 entradas")
    void addToHistory_limitedTo200() {
        for (int i = 0; i < 250; i++) {
            reader.addToHistory("comando_" + i);
        }
        assertTrue(reader.getHistory().size() <= 200);
    }

    @Test
    @DisplayName("las entradas más recientes se conservan al alcanzar el límite")
    void addToHistory_keepsRecentEntries() {
        for (int i = 0; i < 210; i++) {
            reader.addToHistory("cmd_" + i);
        }
        List<String> hist = reader.getHistory();
        // La última entrada debe ser la más reciente
        assertEquals("cmd_209", hist.get(hist.size() - 1));
    }
}
