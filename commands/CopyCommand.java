package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Comando: copy <origen> <destino>
 *
 * Casos soportados:
 *   copy archivo.txt carpeta/          → copia el archivo dentro de la carpeta
 *   copy archivo.txt carpeta/nuevo.txt → copia con nuevo nombre
 *   copy carpeta/ destino/             → copia recursiva de directorio completo
 *
 * Flags opcionales (van después del destino):
 *   -f  → sobreescribir sin preguntar (force)
 */
public class CopyCommand implements Command {

    @Override
    public void execute(String[] args, FileSession session) {
        if (args.length < 2) {
            Formatter.printError("Uso: copy <origen> <destino> [-f]");
            Formatter.printInfo("Ejemplo: copy informe.txt Documentos/");
            return;
        }

        boolean force = false;
        // Detectar flag -f al final
        String lastArg = args[args.length - 1];
        if (lastArg.equals("-f")) {
            force = true;
            args = java.util.Arrays.copyOf(args, args.length - 1);
            if (args.length < 2) {
                Formatter.printError("Uso: copy <origen> <destino> [-f]");
                return;
            }
        }

        File origin = resolveFile(args[0], session);
        File destination = resolveFile(args[1], session);

        if (!origin.exists()) {
            Formatter.printError("El origen no existe: " + args[0]);
            return;
        }

        // Si el destino es un directorio existente, copiar dentro de él
        if (destination.isDirectory()) {
            destination = new File(destination, origin.getName());
        }

        // Comprobar sobreescritura
        if (destination.exists() && !force) {
            Formatter.printError("Ya existe: " + destination.getAbsolutePath());
            Formatter.printInfo("Usa -f para sobreescribir: copy " + args[0] + " " + args[1] + " -f");
            return;
        }

        try {
            if (origin.isDirectory()) {
                copyDirectory(origin.toPath(), destination.toPath(), force);
                Formatter.printSuccess("Carpeta copiada → " + destination.getAbsolutePath());
            } else {
                copyFile(origin.toPath(), destination.toPath(), force);
                Formatter.printSuccess("Copiado → " + destination.getAbsolutePath());
            }
        } catch (IOException e) {
            Formatter.printError("No se pudo copiar: " + e.getMessage());
        }
    }

    private void copyFile(Path origin, Path destination, boolean force) throws IOException {
        CopyOption[] options = force
                ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES}
                : new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES};
        Files.copy(origin, destination, options);
    }

    private void copyDirectory(Path origin, Path destination, boolean force) throws IOException {
        Files.walkFileTree(origin, new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path target = destination.resolve(origin.relativize(dir));
                if (!Files.exists(target)) {
                    Files.createDirectories(target);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path target = destination.resolve(origin.relativize(file));
                CopyOption[] options = force
                        ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES}
                        : new CopyOption[]{StandardCopyOption.COPY_ATTRIBUTES};
                Files.copy(file, target, options);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                Formatter.printError("No se pudo copiar: " + file + " — " + exc.getMessage());
                return FileVisitResult.CONTINUE; // seguir con el resto
            }
        });
    }

    private File resolveFile(String path, FileSession session) {
        File f = new File(path);
        return f.isAbsolute() ? f : new File(session.getCurrentDirectory(), path);
    }

    @Override
    public String[] names() {
        return new String[]{"copy", "cp"};
    }

    @Override
    public String helpText() {
        return "copy <origen> <destino> [-f]  →  Copia archivo o carpeta (-f sobreescribe)";
    }
}
