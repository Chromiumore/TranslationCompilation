package me.chromiumore.mtnc.preprocessor;

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

        PreprocessResult preprocessResult = preprocessFile(args[0]);
        ValidationResult validationResult = preprocessResult.validationResult();

        System.out.println(preprocessResult.program());
        System.out.println("\n" + validationResult.getMessage());
    }

    public static PreprocessResult preprocessFile(String path) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(path)));

            String cleaned = cleanContent(content);
            ValidationResult validationResult = validateAfterCleaning(cleaned);

            return new PreprocessResult(cleaned, validationResult);
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

    public static ValidationResult validateAfterCleaning(String cleanedContent) {
        Pattern openCommentsPattern = Pattern.compile("/\\*|\\*/");
        Matcher commentsMatcher = openCommentsPattern.matcher(cleanedContent);

        if (commentsMatcher.find()) {
            return ValidationResult.error("Обнаружен незакрытый многострочный комментарий!");
        }

        Pattern invalidCharactersPattern = Pattern.compile("\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F");
        Matcher charactersMatcher = invalidCharactersPattern.matcher(cleanedContent);

        if (charactersMatcher.find()) {
            return ValidationResult.error("Обнаружены недопустимые символы!");
        }

        return ValidationResult.ok();
    }
}