package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * Comando: rename <nombre_actual> <nombre_nuevo>
 *
 * Renombra un archivo o carpeta dentro del mismo directorio.
 * A diferencia de 'move', aquí queda claro que la intención es cambiar el nombre,
 * no cambiar la ubicación. Si el nuevo nombre incluye separadores de ruta, lo rechaza.
 *
 * Flags:
 *   -f  → sobreescribir si el nombre nuevo ya existe
 *
 * Ejemplos:
 *   rename borrador.txt entrega_final.txt
 *   rename carpeta_vieja carpeta_nueva
 *   rename foto.jpg foto_portada.jpg -f
 */
public class RenameCommand implements Command {

    @Override
    public void execute(String[] args, FileSession session) {
        if (args.length < 2) {
            Formatter.printError("Uso: rename <nombre_actual> <nombre_nuevo> [-f]");
            Formatter.printInfo("Ejemplo: rename borrador.txt entrega_final.txt");
            return;
        }

        boolean force = false;
        if (args[args.length - 1].equals("-f")) {
            force = true;
            args = java.util.Arrays.copyOf(args, args.length - 1);
            if (args.length < 2) {
                Formatter.printError("Uso: rename <nombre_actual> <nombre_nuevo> [-f]");
                return;
            }
        }

        String currentName = args[0];
        String newName     = args[1];

        // Rechazar si el nuevo nombre contiene separadores de ruta
        if (newName.contains("/") || newName.contains("\\")) {
            Formatter.printError("El nuevo nombre no puede contener separadores de ruta.");
            Formatter.printInfo("Para mover a otra carpeta usa el comando 'move'.");
            return;
        }

        // Rechazar caracteres inválidos en Windows (y en general)
        if (newName.matches(".*[<>:\"|?*].*")) {
            Formatter.printError("El nombre contiene caracteres no permitidos: < > : \" | ? *");
            return;
        }

        File source = resolveFile(currentName, session);

        if (!source.exists()) {
            Formatter.printError("No existe: " + currentName);
            return;
        }

        File destination = new File(source.getParentFile(), newName);

        if (destination.exists()) {
            if (!force) {
                Formatter.printError("Ya existe un elemento llamado \"" + newName + "\" en este directorio.");
                Formatter.printInfo("Usa -f para sobreescribir: rename " + currentName + " " + newName + " -f");
                return;
            }
            // Si -f y el destino es directorio no vacío, rechazar igualmente
            if (destination.isDirectory()) {
                File[] contents = destination.listFiles();
                if (contents != null && contents.length > 0) {
                    Formatter.printError("No se puede sobreescribir una carpeta no vacía.");
                    return;
                }
            }
        }

        try {
            CopyOption[] options = force
                    ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING}
                    : new CopyOption[]{};
            Files.move(source.toPath(), destination.toPath(), options);

            System.out.println();
            Formatter.printSuccess("\"" + currentName + "\"  →  \"" + newName + "\"");
            System.out.println();

        } catch (IOException e) {
            Formatter.printError("No se pudo renombrar: " + e.getMessage());
        }
    }

    private File resolveFile(String path, FileSession session) {
        File f = new File(path);
        return f.isAbsolute() ? f : new File(session.getCurrentDirectory(), path);
    }

    @Override
    public String[] names() {
        return new String[]{"rename", "ren"};
    }

    @Override
    public String helpText() {
        return "rename <actual> <nuevo> [-f]  →  Renombra archivo o carpeta en el lugar";
    }
}
