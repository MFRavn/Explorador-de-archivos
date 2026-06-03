package commands;

import core.FileSession;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DeleteCommand")
class DeleteCommandTest {

    private DeleteCommand command;
    private FileSession   session;

    @TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        command = new DeleteCommand();
        session = new FileSession();
        session.changeDirectory(tempDir.toString());

        // Simular scanner que responde "s" (confirmar) a toda pregunta
        session.setScanner(new java.util.Scanner("s\n"));
    }

    @Test
    @DisplayName("delete archivo con confirmación afirmativa → archivo eliminado")
    void delete_fileWithConfirmation_deletesFile() throws IOException {
        File file = createFile("basura.txt");
        session.setScanner(new java.util.Scanner("s\n"));

        command.execute(new String[]{"basura.txt"}, session);

        assertFalse(file.exists());
    }

    @Test
    @DisplayName("delete archivo con confirmación negativa → archivo conservado")
    void delete_fileWithDenial_keepsFile() throws IOException {
        File file = createFile("importante.txt");
        session.setScanner(new java.util.Scanner("n\n"));

        command.execute(new String[]{"importante.txt"}, session);

        assertTrue(file.exists());
    }

    @Test
    @DisplayName("delete con -f → elimina sin pedir confirmación")
    void delete_withForceFlag_deletesWithoutPrompt() throws IOException {
        File file = createFile("borrar.txt");
        // scanner vacío — si lo leyera, fallaría
        session.setScanner(new java.util.Scanner(""));

        command.execute(new String[]{"borrar.txt", "-f"}, session);

        assertFalse(file.exists());
    }

    @Test
    @DisplayName("delete carpeta recursivamente con -f → carpeta y contenido eliminados")
    void delete_directoryWithForce_deletesRecursively() throws IOException {
        File dir = tempDir.resolve("carpeta").toFile(); dir.mkdir();
        Files.writeString(new File(dir, "archivo.txt").toPath(), "x");

        command.execute(new String[]{"carpeta", "-f"}, session);

        assertFalse(dir.exists());
    }

    @Test
    @DisplayName("delete archivo inexistente → no lanza excepción")
    void delete_missingFile_handlesGracefully() {
        assertDoesNotThrow(() ->
            command.execute(new String[]{"no_existe.txt", "-f"}, session));
    }

    @Test
    @DisplayName("delete sin argumentos → no lanza excepción")
    void delete_noArgs_handlesGracefully() {
        assertDoesNotThrow(() ->
            command.execute(new String[]{}, session));
    }

    @Test
    @DisplayName("delete el directorio de trabajo actual → rechazado")
    void delete_currentWorkingDir_isRejected() {
        // El directorio de trabajo es tempDir, intentar borrarlo
        session.setScanner(new java.util.Scanner("s\n"));

        assertDoesNotThrow(() ->
            command.execute(new String[]{tempDir.toString(), "-f"}, session));

        // El directorio debe seguir existiendo
        assertTrue(tempDir.toFile().exists());
    }

    private File createFile(String name) throws IOException {
        File f = tempDir.resolve(name).toFile();
        f.createNewFile();
        return f;
    }
}
