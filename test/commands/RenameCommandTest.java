package commands;

import core.FileSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RenameCommand")
class RenameCommandTest {

    private RenameCommand command;
    private FileSession   session;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        command = new RenameCommand();
        session = new FileSession();
        session.changeDirectory(tempDir.toString());
    }

    @Test
    @DisplayName("renombrar archivo → nombre cambia, contenido intacto")
    void rename_file_changesName() throws IOException {
        createFile("viejo.txt", "datos");
        command.execute(new String[]{"viejo.txt", "nuevo.txt"}, session);

        assertFalse(tempDir.resolve("viejo.txt").toFile().exists());
        assertEquals("datos", Files.readString(tempDir.resolve("nuevo.txt")));
    }

    @Test
    @DisplayName("renombrar directorio → nombre cambia")
    void rename_directory_changesName() {
        tempDir.resolve("antigua").toFile().mkdir();
        command.execute(new String[]{"antigua", "nueva"}, session);

        assertFalse(tempDir.resolve("antigua").toFile().exists());
        assertTrue(tempDir.resolve("nueva").toFile().isDirectory());
    }

    @Test
    @DisplayName("nuevo nombre con separador de ruta → rechazado")
    void rename_newNameWithPathSeparator_isRejected() throws IOException {
        createFile("archivo.txt", "x");
        command.execute(new String[]{"archivo.txt", "carpeta/archivo.txt"}, session);
        // El archivo original sigue en su lugar
        assertTrue(tempDir.resolve("archivo.txt").toFile().exists());
    }

    @Test
    @DisplayName("nuevo nombre con caracteres inválidos → rechazado")
    void rename_invalidCharsInNewName_isRejected() throws IOException {
        createFile("archivo.txt", "x");
        command.execute(new String[]{"archivo.txt", "arch:ivo.txt"}, session);
        assertTrue(tempDir.resolve("archivo.txt").toFile().exists());
    }

    @Test
    @DisplayName("nombre nuevo ya existe sin -f → no sobreescribe")
    void rename_destinationExists_noForce_keepsOriginal() throws IOException {
        createFile("a.txt", "a_contenido");
        createFile("b.txt", "b_contenido");

        command.execute(new String[]{"a.txt", "b.txt"}, session);

        // b.txt conserva su contenido original
        assertEquals("b_contenido", Files.readString(tempDir.resolve("b.txt")));
        // a.txt sigue existiendo
        assertTrue(tempDir.resolve("a.txt").toFile().exists());
    }

    @Test
    @DisplayName("nombre nuevo ya existe con -f → sobreescribe")
    void rename_destinationExists_withForce_overwrites() throws IOException {
        createFile("a.txt", "a_contenido");
        createFile("b.txt", "b_contenido");

        command.execute(new String[]{"a.txt", "b.txt", "-f"}, session);

        assertFalse(tempDir.resolve("a.txt").toFile().exists());
        assertEquals("a_contenido", Files.readString(tempDir.resolve("b.txt")));
    }

    @Test
    @DisplayName("origen inexistente → no lanza excepción")
    void rename_missingSrc_handlesGracefully() {
        assertDoesNotThrow(() ->
            command.execute(new String[]{"no_existe.txt", "nuevo.txt"}, session));
    }

    @Test
    @DisplayName("menos de 2 argumentos → no lanza excepción")
    void rename_tooFewArgs_handlesGracefully() {
        assertDoesNotThrow(() ->
            command.execute(new String[]{"solo_uno"}, session));
    }

    private File createFile(String name, String content) throws IOException {
        File f = tempDir.resolve(name).toFile();
        Files.writeString(f.toPath(), content);
        return f;
    }
}
