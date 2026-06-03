package commands;

import core.FileSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests para CopyCommand.
 * Se usan @TempDir para que JUnit limpie todo al acabar cada test.
 */
@DisplayName("CopyCommand")
class CopyCommandTest {

    private CopyCommand  command;
    private FileSession  session;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        command = new CopyCommand();
        session = new FileSession();
        session.changeDirectory(tempDir.toString());
    }

    // ── Copiar archivo ───────────────────────────────────────────────

    @Test
    @DisplayName("copiar archivo existente → se crea en destino")
    void copy_existingFile_createsDestination() throws IOException {
        File src  = createFile("origen.txt", "contenido");
        File dest = tempDir.resolve("destino.txt").toFile();

        command.execute(new String[]{"origen.txt", "destino.txt"}, session);

        assertTrue(dest.exists());
        assertEquals("contenido", Files.readString(dest.toPath()));
    }

    @Test
    @DisplayName("copiar archivo a carpeta existente → entra dentro con el mismo nombre")
    void copy_fileToExistingDir_createsInsideDir() throws IOException {
        File src    = createFile("data.txt", "abc");
        File subDir = tempDir.resolve("subdir").toFile();
        subDir.mkdir();

        command.execute(new String[]{"data.txt", "subdir"}, session);

        assertTrue(new File(subDir, "data.txt").exists());
    }

    @Test
    @DisplayName("copiar archivo que no existe → no falla la JVM")
    void copy_missingSource_handlesGracefully() {
        // Solo comprobamos que no lanza excepción (el comando imprime el error)
        assertDoesNotThrow(() ->
            command.execute(new String[]{"no_existe.txt", "destino.txt"}, session));
    }

    @Test
    @DisplayName("copiar sin -f cuando destino ya existe → no sobreescribe")
    void copy_destinationExists_noForce_doesNotOverwrite() throws IOException {
        createFile("origen.txt", "nuevo");
        File dest = createFile("destino.txt", "original");

        command.execute(new String[]{"origen.txt", "destino.txt"}, session);

        assertEquals("original", Files.readString(dest.toPath()));
    }

    @Test
    @DisplayName("copiar con -f cuando destino ya existe → sobreescribe")
    void copy_destinationExists_withForce_overwrites() throws IOException {
        createFile("origen.txt", "nuevo");
        File dest = createFile("destino.txt", "original");

        command.execute(new String[]{"origen.txt", "destino.txt", "-f"}, session);

        assertEquals("nuevo", Files.readString(dest.toPath()));
    }

    // ── Copiar directorio ────────────────────────────────────────────

    @Test
    @DisplayName("copiar directorio con archivos → copia recursiva completa")
    void copy_directory_recursiveCopy() throws IOException {
        File src = tempDir.resolve("carpeta_src").toFile();
        src.mkdir();
        Files.writeString(new File(src, "a.txt").toPath(), "a");
        Files.writeString(new File(src, "b.txt").toPath(), "b");

        command.execute(new String[]{"carpeta_src", "carpeta_dest"}, session);

        File dest = tempDir.resolve("carpeta_dest").toFile();
        assertTrue(dest.isDirectory());
        assertTrue(new File(dest, "a.txt").exists());
        assertTrue(new File(dest, "b.txt").exists());
    }

    // ── Nombres y alias ──────────────────────────────────────────────

    @Test
    @DisplayName("nombres del comando incluyen 'copy' y 'cp'")
    void names_includeExpectedAliases() {
        assertArrayEquals(new String[]{"copy", "cp"}, command.names());
    }

    // ── Helper ───────────────────────────────────────────────────────

    private File createFile(String name, String content) throws IOException {
        File f = tempDir.resolve(name).toFile();
        Files.writeString(f.toPath(), content);
        return f;
    }
}
