package ee.rmit.xrd.utils;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public final class FileUtils {

    private FileUtils() {
    }

    public static byte[] readFromFile(Path absolutePath) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int i;
        try (FileInputStream fis = new FileInputStream(absolutePath.toString())) {
            while ((i = fis.read(buffer)) != -1) {
                baos.write(buffer, 0, i);
            }
        }
        return baos.toByteArray();
    }

    public static byte[] readFromUrl(URL url) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int i;
        try (InputStream is = url.openStream()) {
            while ((i = is.read(buffer)) != -1) {
                baos.write(buffer, 0, i);
            }
        }
        return baos.toByteArray();
    }

    public static void writeToFile(Path absolutePath, byte[] content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(absolutePath.toString())) {
            fos.write(content);
        }
    }

    /**
     * <pre>
     * Glob matcher is explained {@link java.nio.file.FileSystem#getPathMatcher(String) getPatchMatcher}.
     *
     * Examples:
     *  "*.{c,h,cpp,hpp,java}"
     *  "*.java"
     * </pre>
     */
    public static List<Path> listFiles(Path directory, String glob) throws IOException {
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, glob)) {
            stream.forEach(result::add);
        } catch (DirectoryIteratorException ex) {
            throw ex.getCause();
        }
        return result;
    }

    public static void deleteDirectory(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
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

    public static void closeInputStream(InputStream is) {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (Exception ignore) {
            //silent failure
        }
    }

    public static void closeOutputStream(OutputStream os) {
        if (os == null) {
            return;
        }
        try {
            os.close();
        } catch (Exception ignore) {
            //silent failure
        }
    }
}
