package hellfirepvp.modularmachinery.common.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FileUtils {
    public static final int KB = 1024;
    public static final int MB = 1024 * 1024;

    public static String readFile(File file) throws IOException {
        InputStreamReader reader = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
        char[] chars = new char[formatBufferSize(file.length())];
        StringBuilder sb = new StringBuilder((int) file.length());
        int length;

        try {
            while ((length = reader.read(chars)) != -1) {
                sb.append(chars, 0, length);
            }
        } catch (IOException e) {
            reader.close();
            throw e;
        } finally {
            reader.close();
        }

        return sb.toString();
    }

    public static void writeFile(String content, File file) throws IOException {
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("Could not create file " + file.getAbsolutePath());
            }
        }
        OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file.toPath()), StandardCharsets.UTF_8);
        try {
            writer.write(content);
        } catch (IOException e) {
            writer.close();
            throw e;
        } finally {
            writer.close();

        }
    }

    public static int formatBufferSize(long size) {
        if (size <= KB) {
            return (int) size;
        } else if (size <= MB) {
            return KB * 8;
        } else if (size <= MB * 128) {
            return KB * 32;
        } else {
            return KB * 128;
        }
    }
}
