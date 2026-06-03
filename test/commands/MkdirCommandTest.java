package commands;

import core.FileSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MkdirCommand")
class MkdirCommandTest {

    private MkdirCommand command;
    private FileSession  session;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        command = new MkdirCommand();
        session = new FileSession();
        session.changeDirectory(tempDir.toString());
    }

    @Test
    @DisplayName("crear directorio simple → existe después del comando")
    void mkdir_simple_createsDirectory() {
        command.execute(new String[]{"nueva_carpeta"}, session);
        assertTrue(tempDir.resolve("nueva_carpeta").toFile().isDirectory());
    }

    @Test
    @DisplayName("crear ruta anidada → toda la cadena se crea")
    void mkdir_nestedPath_createsFullChain() {
        command.execute(new String[]{"a/b/c"}, session);
        assertTrue(tempDir.resolve("a/b/c").toFile().isDirectory());
    }

    @Test
    @DisplayName("crear varios directorios en un comando → todos se crean")
    void mkdir_multipleArgs_createsAll() {
        command.execute(new String[]{"docs", "tests", "resources"}, session);
        assertTrue(tempDir.resolve("docs").toFile().isDirectory());
        assertTrue(tempDir.resolve("tests").toFile().isDirectory());
        assertTrue(tempDir.resolve("resources").toFile().isDirectory());
    }

    @Test
    @DisplayName("crear directorio que ya existe → no falla")
    void mkdir_alreadyExists_doesNotFail() {
        tempDir.resolve("existente").toFile().mkdir();
        assertDoesNotThrow(() ->
            command.execute(new String[]{"existente"}, session));
    }

    @Test
    @DisplayName("crear directorio con nombre de archivo existente → lo indica sin fallar")
    void mkdir_nameClashesWithFile_handlesGracefully() throws IOException {
        Files.writeString(tempDir.resolve("archivo.txt"), "x");
        assertDoesNotThrow(() ->
            command.execute(new String[]{"archivo.txt"}, session));
        // El archivo no se convirtió en directorio
        assertFalse(tempDir.resolve("archivo.txt").toFile().isDirectory());
    }

    @Test
    @DisplayName("sin argumentos → no lanza excepción")
    void mkdir_noArgs_handlesGracefully() {
        assertDoesNotThrow(() ->
            command.execute(new String[]{}, session));
    }

    @Test
    @DisplayName("nombres incluyen 'mkdir' y 'md'")
    void names_includeExpectedAliases() {
        assertTrue(java.util.Arrays.asList(command.names()).contains("mkdir"));
        assertTrue(java.util.Arrays.asList(command.names()).contains("md"));
    }
}
