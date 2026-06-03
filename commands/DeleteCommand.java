package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Comando: delete <nombre> [-f]
 *
 * Elimina un archivo o carpeta completa.
 * Siempre pide confirmación antes de borrar, a menos que se pase -f (force).
 *
 * Si el objetivo es una carpeta no vacía, lo avisa explícitamente antes de confirmar.
 *
 * Ejemplos:
 *   delete viejo.txt
 *   delete carpeta_temporal/
 *   delete logs/ -f        ← sin confirmación
 */
public class DeleteCommand implements Command {

    @Override
    public void execute(String[] args, FileSession session) {
        if (args.length == 0) {
            Formatter.printError("Uso: delete <archivo_o_carpeta> [-f]");
            return;
        }

        boolean force = false;
        String targetArg = args[0];

        // Detectar -f en cualquier posición
        for (String arg : args) {
            if (arg.equals("-f")) force = true;
        }
        // Si el único arg sin flag es -f, falta el objetivo
        if (targetArg.equals("-f")) {
            Formatter.printError("Uso: delete <archivo_o_carpeta> [-f]");
            return;
        }

        File target = resolveFile(targetArg, session);

        if (!target.exists()) {
            Formatter.printError("No existe: " + targetArg);
            return;
        }

        // Protección: no permitir borrar el directorio de trabajo actual
        try {
            if (target.getCanonicalPath().equals(session.getCurrentDirectory().getCanonicalPath())) {
                Formatter.printError("No puedes eliminar el directorio de trabajo actual.");
                Formatter.printInfo("Navega fuera con 'cd ..' primero.");
                return;
            }
        } catch (IOException e) {
            Formatter.printError("No se pudo verificar la ruta: " + e.getMessage());
            return;
        }

        // Calcular info del objetivo para mostrar en confirmación
        boolean isDir    = target.isDirectory();
        String  typeDesc = isDir ? "carpeta" : "archivo";

        System.out.println();

        if (isDir) {
            int count = countFiles(target);
            if (count > 0) {
                System.out.printf("  %s⚠ La carpeta contiene %d archivo(s) y se eliminará por completo.%s%n",
                        Formatter.YELLOW, count, Formatter.RESET);
            }
        }

        if (!force) {
            boolean confirmed = session.confirm(
                    "  " + Formatter.RED + "¿Eliminar " + typeDesc + " \"" + target.getName() + "\"?" + Formatter.RESET
            );
            if (!confirmed) {
                Formatter.printInfo("Operación cancelada.");
                System.out.println();
                return;
            }
        }

        try {
            if (isDir) {
                deleteRecursive(target.toPath());
            } else {
                Files.delete(target.toPath());
            }
            Formatter.printSuccess("Eliminado: " + target.getAbsolutePath());
        } catch (IOException e) {
            Formatter.printError("No se pudo eliminar: " + e.getMessage());
        }

        System.out.println();
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
                if (exc != null) throw exc;
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                Formatter.printError("No se pudo borrar: " + file.getFileName() + " — " + exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Cuenta archivos (no directorios) de forma recursiva. */
    private int countFiles(File dir) {
        int count = 0;
        File[] entries = dir.listFiles();
        if (entries == null) return 0;
        for (File f : entries) {
            if (f.isDirectory()) count += countFiles(f);
            else count++;
        }
        return count;
    }

    private File resolveFile(String path, FileSession session) {
        File f = new File(path);
        return f.isAbsolute() ? f : new File(session.getCurrentDirectory(), path);
    }

    @Override
    public String[] names() {
        return new String[]{"delete", "del", "rm", "remove"};
    }

    @Override
    public String helpText() {
        return "delete <nombre> [-f]  →  Elimina archivo o carpeta (pide confirmación)";
    }
}
