package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Comando: move <origen> <destino>
 *
 * Casos soportados:
 *   move archivo.txt Documentos/           → mover a otra carpeta
 *   move archivo.txt nuevo_nombre.txt      → renombrar en el mismo lugar
 *   move archivo.txt Documentos/nuevo.txt  → mover y renombrar a la vez
 *   move carpeta/ OtraCarpeta/             → mover directorio completo
 *
 * Flags:
 *   -f  → sobreescribir destino si ya existe
 */
public class MoveCommand implements Command {

    @Override
    public void execute(String[] args, FileSession session) {
        if (args.length < 2) {
            Formatter.printError("Uso: move <origen> <destino> [-f]");
            Formatter.printInfo("Ejemplo: move informe.txt Documentos/");
            Formatter.printInfo("Ejemplo: move viejo.txt nuevo.txt   (renombrar)");
            return;
        }

        boolean force = false;
        if (args[args.length - 1].equals("-f")) {
            force = true;
            args = java.util.Arrays.copyOf(args, args.length - 1);
            if (args.length < 2) {
                Formatter.printError("Uso: move <origen> <destino> [-f]");
                return;
            }
        }

        File origin = resolveFile(args[0], session);
        File destination = resolveFile(args[1], session);

        if (!origin.exists()) {
            Formatter.printError("El origen no existe: " + args[0]);
            return;
        }

        // Si el destino es directorio existente, mover dentro de él
        if (destination.isDirectory()) {
            destination = new File(destination, origin.getName());
        }

        if (destination.exists() && !force) {
            Formatter.printError("Ya existe: " + destination.getAbsolutePath());
            Formatter.printInfo("Usa -f para sobreescribir: move " + args[0] + " " + args[1] + " -f");
            return;
        }

        // Asegurarse de que el directorio padre del destino existe
        File parentDir = destination.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            Formatter.printError("El directorio destino no existe: " + parentDir.getAbsolutePath());
            return;
        }

        try {
            boolean isRename = origin.getParentFile().getCanonicalPath()
                    .equals(destination.getParentFile() != null
                            ? destination.getParentFile().getCanonicalPath()
                            : session.getCurrentPath());

            CopyOption[] options = force
                    ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING}
                    : new CopyOption[]{};

            // Intentar mover atómicamente (mismo volumen) o con copia+borrado (distintos volúmenes)
            try {
                Files.move(origin.toPath(), destination.toPath(), options);
            } catch (AtomicMoveNotSupportedException e) {
                // Fallback: copiar y borrar
                copyAndDelete(origin, destination, force);
            }

            if (isRename) {
                Formatter.printSuccess("Renombrado → " + destination.getName());
            } else {
                Formatter.printSuccess("Movido → " + destination.getAbsolutePath());
            }

        } catch (IOException e) {
            Formatter.printError("No se pudo mover: " + e.getMessage());
        }
    }

    /** Fallback para mover entre volúmenes distintos: copia recursiva + borrado del origen. */
    private void copyAndDelete(File origin, File destination, boolean force) throws IOException {
        if (origin.isDirectory()) {
            copyDirectoryRecursive(origin.toPath(), destination.toPath(), force);
            deleteRecursive(origin.toPath());
        } else {
            CopyOption[] opts = force
                    ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES}
                    : new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES};
            Files.copy(origin.toPath(), destination.toPath(), opts);
            Files.delete(origin.toPath());
        }
    }

    private void copyDirectoryRecursive(Path origin, Path destination, boolean force) throws IOException {
        Files.walkFileTree(origin, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(destination.resolve(origin.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                CopyOption[] opts = force
                        ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES}
                        : new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES};
                Files.copy(file, destination.resolve(origin.relativize(file)), opts);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void deleteRecursive(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private File resolveFile(String path, FileSession session) {
        File f = new File(path);
        return f.isAbsolute() ? f : new File(session.getCurrentDirectory(), path);
    }

    @Override
    public String[] names() {
        return new String[]{"move", "mv"};
    }

    @Override
    public String helpText() {
        return "move <origen> <destino> [-f]  →  Mueve o renombra archivo/carpeta";
    }
}
