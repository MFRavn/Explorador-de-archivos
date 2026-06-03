package core;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para FileSession: navegación, historial y confirmaciones.
 */
@DisplayName("FileSession")
class FileSessionTest {

    private FileSession session;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        session = new FileSession();
    }

    // ── changeDirectory ──────────────────────────────────────────────

    @Test
    @DisplayName("cd a ruta absoluta existente → éxito")
    void changeDirectory_absoluteExisting_returnsTrue() {
        assertTrue(session.changeDirectory(tempDir.toString()));
        assertEquals(tempDir.toFile().getAbsolutePath(),
                     session.getCurrentDirectory().getAbsolutePath());
    }

    @Test
    @DisplayName("cd a ruta que no existe → false")
    void changeDirectory_nonExistent_returnsFalse() {
        assertFalse(session.changeDirectory("/ruta/que/no/existe/jamas"));
    }

    @Test
    @DisplayName("cd .. desde directorio con padre → sube un nivel")
    void changeDirectory_dotDot_goesUp() {
        session.changeDirectory(tempDir.toString());
        File parent = tempDir.toFile().getParentFile();
        assertTrue(session.changeDirectory(".."));
        assertEquals(parent.getAbsolutePath(),
                     session.getCurrentDirectory().getAbsolutePath());
    }

    @Test
    @DisplayName("cd .. desde raíz del sistema → false (no hay padre)")
    void changeDirectory_dotDotFromRoot_returnsFalse() {
        // Encontrar la raíz real del sistema
        File root = tempDir.toFile();
        while (root.getParentFile() != null) root = root.getParentFile();
        session.changeDirectory(root.getAbsolutePath());

        assertFalse(session.changeDirectory(".."));
    }

    @Test
    @DisplayName("cd ~ → directorio home del usuario")
    void changeDirectory_tilde_goesToHome() {
        session.changeDirectory(tempDir.toString()); // salir del home primero
        assertTrue(session.changeDirectory("~"));
        assertEquals(System.getProperty("user.home"),
                     session.getCurrentDirectory().getAbsolutePath());
    }

    @Test
    @DisplayName("cd - → vuelve al directorio anterior")
    void changeDirectory_dash_returnsToLastDir() {
        File original = session.getCurrentDirectory();
        session.changeDirectory(tempDir.toString());

        assertTrue(session.changeDirectory("-"));
        assertEquals(original.getAbsolutePath(),
                     session.getCurrentDirectory().getAbsolutePath());
    }

    @Test
    @DisplayName("cd - sin historial → false")
    void changeDirectory_dashWithEmptyHistory_returnsFalse() {
        assertFalse(session.changeDirectory("-"));
    }

    @Test
    @DisplayName("cd a un archivo (no directorio) → false")
    void changeDirectory_toFile_returnsFalse() throws Exception {
        File file = tempDir.resolve("archivo.txt").toFile();
        file.createNewFile();
        assertFalse(session.changeDirectory(file.getAbsolutePath()));
    }

    // ── Historial ────────────────────────────────────────────────────

    @Test
    @DisplayName("historial se puebla al navegar")
    void history_populatesOnNavigation() {
        File initial = session.getCurrentDirectory();
        session.changeDirectory(tempDir.toString());

        assertFalse(session.getHistory().isEmpty());
        assertEquals(initial.getAbsolutePath(),
                     session.getHistory().peek().getAbsolutePath());
    }

    @Test
    @DisplayName("historial se limita a 50 entradas")
    void history_limitedTo50Entries() throws Exception {
        // Crear 60 subdirectorios y navegar por ellos
        File base = tempDir.toFile();
        File[] dirs = new File[60];
        for (int i = 0; i < 60; i++) {
            dirs[i] = new File(base, "dir" + i);
            dirs[i].mkdir();
        }
        for (File dir : dirs) {
            session.changeDirectory(dir.getAbsolutePath());
        }
        assertTrue(session.getHistory().size() <= 50);
    }

    // ── confirm ──────────────────────────────────────────────────────

    @Test
    @DisplayName("confirm con scanner null → false (no falla)")
    void confirm_nullScanner_returnsFalse() {
        // session sin setScanner → scanner es null
        assertFalse(session.confirm("¿Continuar?"));
    }
}
