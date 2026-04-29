package me.chromiumore.mtnc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Preprocessor {
public static void main(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("Usage: java Preprocessor <inputFile>");
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(args[0])));

            String cleaned = cleanContent(content);
            String message = validateAfterCleaning(cleaned);

            System.out.println(cleaned);
            System.out.println("\n" + message);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open a file");
        }
    }

    private static String cleanContent(String content) {
        // Очистка от комментариев
        String noComments = content.replaceAll("//.*", "");
        String noBlockComments = noComments.replaceAll("/\\*[\\s\\S]*?\\*/", "");

        // Удаление лишних пробельных символов
        String cleaned = noBlockComments
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\r?\\n[ \\t]+", "\n")
                .replaceAll("[ \\t]+\\r?\\n", "\n")
                .replaceAll("(\\r?\\n)+", "\n");

        cleaned = cleaned.trim();

        return cleaned;
    }

    private static String validateAfterCleaning(String cleanedContent) {
        Pattern openCommentsPattern = Pattern.compile("/\\*|\\*/");
        Matcher commentsMatcher = openCommentsPattern.matcher(cleanedContent);

        if (commentsMatcher.find()) {
            return "Обнаружен незакрытый многострочный комментарий!";
        }

        Pattern invalidCharactersPattern = Pattern.compile("\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F");
        Matcher charactersMatcher = invalidCharactersPattern.matcher(cleanedContent);

        if (charactersMatcher.find()) {
            return "Обнаружены недопустимые символы!";
        }

        return "Ошибок не выявлено";
    }
}