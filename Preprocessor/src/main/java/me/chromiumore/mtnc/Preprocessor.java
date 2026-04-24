package me.chromiumore.mtnc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Preprocessor {
    public static void main(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("Usage: java Preprocessor <inputFile>");
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(args[0])));

            // Очистка от комментариев
            String noBlockComments = content.replaceAll("/\\*[\\s\\S]*?\\*/", "");
            String noComments = noBlockComments.replaceAll("//.*", "");

            // Удаление лишних пробельных символов
            String cleaned = noComments
                    .replaceAll("[ \\t]+", " ")
                    .replaceAll("\\r?\\n[ \\t]+", "\n")
                    .replaceAll("[ \\t]+\\r?\\n", "\n")
                    .replaceAll("(\\r?\\n)+", "\n");

            cleaned = cleaned.trim();

            System.out.println(cleaned);
            System.out.println("\nОшибок не выявлено");
        } catch (IOException e) {
            throw new RuntimeException("Failed to open a file");
        }
    }
}