package commands;

import core.FileSession;
import shell.Command;
import util.Formatter;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.*;

/**
 * Comando: zip <acción> [argumentos]
 *
 * Acciones:
 *   zip create <archivo.zip> <origen...>   → comprime uno o varios archivos/carpetas
 *   zip extract <archivo.zip> [destino]    → descomprime (destino opcional, por defecto carpeta actual)
 *   zip list <archivo.zip>                 → lista el contenido sin extraer
 *
 * Alias: zip c / zip x / zip l
 *
 * Ejemplos:
 *   zip create backup.zip Documentos/ fotos/
 *   zip extract backup.zip
 *   zip extract backup.zip C:\Destino
 *   zip list backup.zip
 */
public class ZipCommand implements Command {

    @Override
    public void execute(String[] args, FileSession session) {
        if (args.length < 2) {
            printUsage();
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "create", "c" -> actionCreate(args, session);
            case "extract", "x" -> actionExtract(args, session);
            case "list", "l" -> actionList(args, session);
            default -> {
                Formatter.printError("Acción desconocida: " + action);
                printUsage();
            }
        }
    }

    // ─────────────────────────────────────────
    //  CREATE
    // ─────────────────────────────────────────

    private void actionCreate(String[] args, FileSession session) {
        // zip create <archivo.zip> <origen1> [origen2] ...
        if (args.length < 3) {
            Formatter.printError("Uso: zip create <archivo.zip> <origen...>");
            return;
        }

        File zipFile = resolveFile(args[1], session);
        if (!zipFile.getName().toLowerCase().endsWith(".zip")) {
            zipFile = new File(zipFile.getAbsolutePath() + ".zip");
        }

        // Recoger todos los orígenes
        File[] sources = new File[args.length - 2];
        for (int i = 2; i < args.length; i++) {
            sources[i - 2] = resolveFile(args[i], session);
            if (!sources[i - 2].exists()) {
                Formatter.printError("No existe: " + args[i]);
                return;
            }
        }

        System.out.println();
        System.out.printf(Formatter.DIM + "  Comprimiendo → %s ...%s%n%n", zipFile.getName(), Formatter.RESET);

        int[] count = {0};

        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(zipFile)))) {

            zos.setLevel(Deflater.BEST_COMPRESSION);

            for (File source : sources) {
                if (source.isDirectory()) {
                    addDirectoryToZip(source, source.getName(), zos, count);
                } else {
                    addFileToZip(source, source.getName(), zos);
                    count[0]++;
                    System.out.println("  " + Formatter.DIM + "+ " + source.getName() + Formatter.RESET);
                }
            }

        } catch (IOException e) {
            Formatter.printError("Error al comprimir: " + e.getMessage());
            return;
        }

        System.out.println();
        Formatter.printSuccess(count[0] + " elemento(s) comprimido(s) → " + zipFile.getAbsolutePath());
        Formatter.printInfo("Tamaño final: " + Formatter.formatSize(zipFile.length()));
        System.out.println();
    }

    private void addDirectoryToZip(File dir, String baseName, ZipOutputStream zos, int[] count) throws IOException {
        Files.walkFileTree(dir.toPath(), new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String entryName = baseName + "/" + dir.toPath().relativize(file).toString()
                        .replace(File.separatorChar, '/');
                addFileToZip(file.toFile(), entryName, zos);
                count[0]++;
                System.out.println("  " + Formatter.DIM + "+ " + entryName + Formatter.RESET);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path subDir, BasicFileAttributes attrs) throws IOException {
                if (!subDir.equals(dir.toPath())) {
                    String entryName = baseName + "/" + dir.toPath().relativize(subDir).toString()
                            .replace(File.separatorChar, '/') + "/";
                    zos.putNextEntry(new ZipEntry(entryName));
                    zos.closeEntry();
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void addFileToZip(File file, String entryName, ZipOutputStream zos) throws IOException {
        zos.putNextEntry(new ZipEntry(entryName));
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = bis.read(buffer)) != -1) {
                zos.write(buffer, 0, read);
            }
        }
        zos.closeEntry();
    }

    // ─────────────────────────────────────────
    //  EXTRACT
    // ─────────────────────────────────────────

    private void actionExtract(String[] args, FileSession session) {
        // zip extract <archivo.zip> [destino]
        if (args.length < 2) {
            Formatter.printError("Uso: zip extract <archivo.zip> [carpeta_destino]");
            return;
        }

        File zipFile = resolveFile(args[1], session);
        if (!zipFile.exists()) {
            Formatter.printError("No existe: " + args[1]);
            return;
        }

        // Destino: argumento o carpeta actual
        File destDir;
        if (args.length >= 3) {
            destDir = resolveFile(args[2], session);
        } else {
            // Por defecto, carpeta con el nombre del ZIP en el directorio actual
            String zipName = zipFile.getName().replaceFirst("(?i)\\.zip$", "");
            destDir = new File(session.getCurrentDirectory(), zipName);
        }

        if (!destDir.exists() && !destDir.mkdirs()) {
            Formatter.printError("No se pudo crear el directorio destino: " + destDir.getAbsolutePath());
            return;
        }

        System.out.println();
        System.out.printf(Formatter.DIM + "  Extrayendo en: %s ...%s%n%n", destDir.getAbsolutePath(), Formatter.RESET);

        int count = 0;

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)))) {

            ZipEntry entry;
            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                // Protección contra Zip Slip (path traversal)
                File outFile = new File(destDir, entry.getName()).getCanonicalFile();
                if (!outFile.getAbsolutePath().startsWith(destDir.getCanonicalPath() + File.separator)
                        && !outFile.equals(destDir.getCanonicalFile())) {
                    Formatter.printError("Entrada ZIP insegura ignorada: " + entry.getName());
                    zis.closeEntry();
                    continue;
                }

                if (entry.isDirectory()) {
                    outFile.mkdirs();
                } else {
                    outFile.getParentFile().mkdirs();
                    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFile))) {
                        int read;
                        while ((read = zis.read(buffer)) != -1) bos.write(buffer, 0, read);
                    }
                    System.out.println("  " + Formatter.DIM + "← " + entry.getName() + Formatter.RESET);
                    count++;
                }
                zis.closeEntry();
            }

        } catch (IOException e) {
            Formatter.printError("Error al extraer: " + e.getMessage());
            return;
        }

        System.out.println();
        Formatter.printSuccess(count + " archivo(s) extraído(s) → " + destDir.getAbsolutePath());
        System.out.println();
    }

    // ─────────────────────────────────────────
    //  LIST
    // ─────────────────────────────────────────

    private void actionList(String[] args, FileSession session) {
        if (args.length < 2) {
            Formatter.printError("Uso: zip list <archivo.zip>");
            return;
        }

        File zipFile = resolveFile(args[1], session);
        if (!zipFile.exists()) {
            Formatter.printError("No existe: " + args[1]);
            return;
        }

        System.out.println();
        System.out.printf(Formatter.BOLD + "  Contenido de: %s%s%n%n", zipFile.getName(), Formatter.RESET);
        System.out.printf("%s%-8s  %-14s  %s%s%n",
                Formatter.BOLD + Formatter.DIM, "TIPO", "TAMAÑO", "ENTRADA", Formatter.RESET);
        System.out.println(Formatter.DIM + "  " + "─".repeat(60) + Formatter.RESET);

        int fileCount = 0, dirCount = 0;
        long totalUncompressed = 0;

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(zipFile)))) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                boolean isDir = entry.isDirectory();
                String type   = isDir ? "[DIR]" : "[   ]";
                String size   = isDir ? "—" : (entry.getSize() >= 0
                        ? Formatter.formatSize(entry.getSize()) : "?");
                String color  = isDir ? Formatter.CYAN : "";

                System.out.printf("  %s%-8s%s  %s%-14s%s  %s%s%s%n",
                        color, type, Formatter.RESET,
                        Formatter.DIM, size, Formatter.RESET,
                        color, entry.getName(), Formatter.RESET);

                if (isDir) dirCount++;
                else {
                    fileCount++;
                    if (entry.getSize() >= 0) totalUncompressed += entry.getSize();
                }
                zis.closeEntry();
            }

        } catch (IOException e) {
            Formatter.printError("Error al leer el ZIP: " + e.getMessage());
            return;
        }

        System.out.println(Formatter.DIM + "  " + "─".repeat(60) + Formatter.RESET);
        System.out.printf(Formatter.DIM + "  %d carpeta(s)  %d archivo(s)  ~%s sin comprimir%s%n%n",
                dirCount, fileCount, Formatter.formatSize(totalUncompressed), Formatter.RESET);
    }

    // ─────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────

    private File resolveFile(String path, FileSession session) {
        File f = new File(path);
        return f.isAbsolute() ? f : new File(session.getCurrentDirectory(), path);
    }

    private void printUsage() {
        System.out.println();
        Formatter.printInfo("Uso del comando zip:");
        System.out.println("  " + Formatter.GREEN + "zip create backup.zip carpeta/ archivo.txt" + Formatter.RESET);
        System.out.println("  " + Formatter.GREEN + "zip extract backup.zip [destino]" + Formatter.RESET);
        System.out.println("  " + Formatter.GREEN + "zip list backup.zip" + Formatter.RESET);
        System.out.println();
    }

    @Override
    public String[] names() {
        return new String[]{"zip"};
    }

    @Override
    public String helpText() {
        return "zip create|extract|list <archivo.zip> [...]  →  Comprimir/descomprimir ZIP";
    }
}
