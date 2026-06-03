package commands;

import core.FileSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MoveCommand")
class MoveCommandTest {

    private MoveCommand command;
    private FileSession session;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        command = new MoveCommand();
        session = new FileSession();
        session.changeDirectory(tempDir.toString());
    }

    @Test
    @DisplayName("mover archivo a otro directorio → origen desaparece, destino existe")
    void move_fileToDirectory_sourceGoneDestinationExists() throws IOException {
        File src    = createFile("origen.txt", "datos");
        File subDir = tempDir.resolve("subdir").toFile(); subDir.mkdir();

        command.execute(new String[]{"origen.txt", "subdir"}, session);

        assertFalse(src.exists());
        assertTrue(new File(subDir, "origen.txt").exists());
    }

    @Test
    @DisplayName("renombrar archivo en el mismo directorio → nombre cambia")
    void move_renameInPlace_changesName() throws IOException {
        createFile("viejo.txt", "contenido");

        command.execute(new String[]{"viejo.txt", "nuevo.txt"}, session);

        assertFalse(tempDir.resolve("viejo.txt").toFile().exists());
        assertTrue(tempDir.resolve("nuevo.txt").toFile().exists());
    }

    @Test
    @DisplayName("mover sin -f cuando destino existe → no sobreescribe")
    void move_destinationExists_noForce_doesNotOverwrite() throws IOException {
        createFile("origen.txt", "nuevo");
        createFile("destino.txt", "original");

        command.execute(new String[]{"origen.txt", "destino.txt"}, session);

        // origen sigue ahí (no se movió)
        assertTrue(tempDir.resolve("origen.txt").toFile().exists());
        assertEquals("original", Files.readString(tempDir.resolve("destino.txt")));
    }

    @Test
    @DisplayName("mover con -f cuando destino existe → sobreescribe")
    void move_destinationExists_withForce_overwrites() throws IOException {
        createFile("origen.txt", "nuevo");
        createFile("destino.txt", "original");

        command.execute(new String[]{"origen.txt", "destino.txt", "-f"}, session);

        assertFalse(tempDir.resolve("origen.txt").toFile().exists());
        assertEquals("nuevo", Files.readString(tempDir.resolve("destino.txt")));
    }

    @Test
    @DisplayName("mover archivo inexistente → no lanza excepción")
    void move_missingSource_handlesGracefully() {
        assertDoesNotThrow(() ->
            command.execute(new String[]{"no_existe.txt", "destino.txt"}, session));
    }

    @Test
    @DisplayName("nombres incluyen 'move' y 'mv'")
    void names_includeExpectedAliases() {
        assertTrue(java.util.Arrays.asList(command.names()).contains("move"));
        assertTrue(java.util.Arrays.asList(command.names()).contains("mv"));
    }

    private File createFile(String name, String content) throws IOException {
        File f = tempDir.resolve(name).toFile();
        Files.writeString(f.toPath(), content);
        return f;
    }
}
