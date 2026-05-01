package me.chromiumore.mtnc.analyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LexicalAnalyzer {
    // Таблицы лексем
    private static final Set<String> KEYWORDS = Set.of(
            "fun", "val", "var", "Int", "for", "in", "if", "else", "return", "while"
    );

    private static final Set<String> OPERATORS = Set.of(
            "=", "+", "+=", "%", "==", "*", "<", ".."
    );

    private static final Set<Character> DELIMITERS = Set.of(
            '(', ')', '{', '}', ';', ':'
    );

    public enum TokenType {
        KEYWORD, IDENTIFIER, CONSTANT_INT, CONSTANT_REAL, CONSTANT_STRING, OPERATOR, DELIMITER, ERROR
    }

    public static class Token {
        public TokenType type;
        public String lexeme;

        public Token(TokenType type, String lexeme) {
            this.type = type;
            this.lexeme = lexeme;
        }

        @Override
        public String toString() {
            return "(" + type + ", " + lexeme + ")";
        }
    }

    private final String input;
    private int position;
    private final List<Token> tokens;
    private final List<String> errors;

    public LexicalAnalyzer(String input) {
        this.input = input.trim();
        this.position = 0;
        this.tokens = new ArrayList<>();
        this.errors = new ArrayList<>();
    }

    public void analyze() {
        while (position < input.length()) {
            char current = input.charAt(position);

            if (Character.isWhitespace(current)) {
                // Пропуск пробелов
                position++;
            } else if (Character.isLetter(current) || current == '_') {
                parseIdentifierOrKeyword();
            } else if (Character.isDigit(current)) {
                parseNumber();
            } else if (current == '"') {
                parseString();
            } else if (isOperatorStart(current)) {
                parseOperator();
            } else if (DELIMITERS.contains(current)) {
                tokens.add(new Token(TokenType.DELIMITER, String.valueOf(current)));
                position++;
            } else {
                String errorMsg = "Недопустимый символ '" + current + "' на позиции " + position;
                errors.add(errorMsg);
                tokens.add(new Token(TokenType.ERROR, String.valueOf(current)));
                position++;
            }
        }
    }

    private void parseIdentifierOrKeyword() {
        int start = position;
        while (position < input.length() &&
                (Character.isLetterOrDigit(input.charAt(position)) || input.charAt(position) == '_')) {
            position++;
        }
        String lexeme = input.substring(start, position);

        if (KEYWORDS.contains(lexeme)) {
            tokens.add(new Token(TokenType.KEYWORD, lexeme));
        } else if (Character.isDigit(lexeme.charAt(0))) {
            String errorMsg = "Идентификатор не может начинаться с цифры: '" + lexeme + "'";
            errors.add(errorMsg);
            tokens.add(new Token(TokenType.ERROR, lexeme));
        } else {
            tokens.add(new Token(TokenType.IDENTIFIER, lexeme));
        }
    }

    private void parseNumber() {
        int start = position;
        boolean isReal = false;
        int dotCount = 0;

        // Целая часть
        while (position < input.length() && Character.isDigit(input.charAt(position))) {
            position++;
        }

        // Дробная часть
        if (position < input.length() && input.charAt(position) == '.') {
            dotCount++;
            position++;

            // Проверка на две точки подряд
            if (position < input.length() && input.charAt(position) == '.') {
                // Это оператор диапазона "..", откатываем
                position--;
                dotCount = 0;
                isReal = false;
            } else {
                isReal = true;
                // Дробная часть должна содержать цифры
                if (position < input.length() && Character.isDigit(input.charAt(position))) {
                    while (position < input.length() && Character.isDigit(input.charAt(position))) {
                        position++;
                    }
                } else {
                    String errorMsg = "Некорректное вещественное число: ожидается цифра после точки";
                    errors.add(errorMsg);
                    tokens.add(new Token(TokenType.ERROR, input.substring(start, position)));
                    return;
                }
            }
        }

        String lexeme = input.substring(start, position);

        // Проверка на буквы в числе
        if (position < input.length() && Character.isLetter(input.charAt(position))) {
            String errorMsg = "Некорректное число: недопустимые символы после числа '" + lexeme + "'";
            errors.add(errorMsg);
            tokens.add(new Token(TokenType.ERROR, lexeme + input.charAt(position)));
            position++;
            return;
        }

        if (isReal) {
            tokens.add(new Token(TokenType.CONSTANT_REAL, lexeme));
        } else {
            tokens.add(new Token(TokenType.CONSTANT_INT, lexeme));
        }
    }

    private void parseString() {
        position++;
        int start = position;
        boolean closed = false;

        while (position < input.length()) {
            char c = input.charAt(position);
            if (c == '"') {
                closed = true;
                break;
            }
            position++;
        }

        if (!closed) {
            String errorMsg = "Незакрытый строковый литерал, начинающийся с позиции " + (start - 1);
            errors.add(errorMsg);
            tokens.add(new Token(TokenType.ERROR, input.substring(start - 1)));
            return;
        }

        String lexeme = input.substring(start - 1, position + 1);
        tokens.add(new Token(TokenType.CONSTANT_STRING, lexeme));
        position++;
    }

    private void parseOperator() {
        String possibleOp = null;
        for (int len = 2; len >= 1; len--) {
            if (position + len <= input.length()) {
                String sub = input.substring(position, position + len);
                if (OPERATORS.contains(sub)) {
                    possibleOp = sub;
                    position += len;
                    break;
                }
            }
        }

        if (possibleOp != null) {
            tokens.add(new Token(TokenType.OPERATOR, possibleOp));
        } else {
            char c = input.charAt(position);
            String errorMsg = "Неизвестный оператор '" + c + "' на позиции " + position;
            errors.add(errorMsg);
            tokens.add(new Token(TokenType.ERROR, String.valueOf(c)));
            position++;
        }
    }

    private boolean isOperatorStart(char c) {
        return OPERATORS.stream().anyMatch(op -> op.charAt(0) == c);
    }

    public void printResults() {
        System.out.printf("%-15s | %s%n", "Лексема", "Тип");
        System.out.println("----------------+-----------------------");
        for (Token token : tokens) {
            System.out.printf("%-15s | %s%n", token.lexeme, token.type);
        }

        System.out.println("\n" + tokens);
        System.out.printf("Лексический анализ завершён. Обнаружено %d токенов.%n", tokens.size());

        if (errors.isEmpty()) {
            System.out.println("Ошибок не найдено.");
        } else {
            System.out.println("Обнаружены лексические ошибки:");
            for (String error : errors) {
                System.out.println(" - " + error);
            }
        }
    }

    public List<Token> getTokens() {
        return tokens;
    }
}
