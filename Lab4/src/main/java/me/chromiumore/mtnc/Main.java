package me.chromiumore.mtnc;

import me.chromiumore.mtnc.analyzer.AstPrinter;
import me.chromiumore.mtnc.analyzer.LexicalAnalyzer;
import me.chromiumore.mtnc.analyzer.ParseException;
import me.chromiumore.mtnc.analyzer.Parser;
import me.chromiumore.mtnc.analyzer.ast.Program;
import me.chromiumore.mtnc.preprocessor.PreprocessResult;
import me.chromiumore.mtnc.preprocessor.Preprocessor;
import me.chromiumore.mtnc.preprocessor.ValidationResult;
import me.chromiumore.mtnc.semantic.SemanticAnalyzer;
import me.chromiumore.mtnc.semantic.Triad;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("Usage: java Main <inputFile>");
        }

        PreprocessResult preprocessResult = Preprocessor.preprocessFile(args[0]);
        ValidationResult validationResult = preprocessResult.validationResult();

        System.out.println(preprocessResult.program());
        System.out.println("\n" + validationResult.getMessage());

        LexicalAnalyzer analyzer = new LexicalAnalyzer(preprocessResult.program());
        analyzer.analyze();

        analyzer.printResults();

        if (analyzer.hasErrors()) { return; }

        Parser parser = new Parser(analyzer.getTokens());
        try {
            Program ast = parser.parse();
            if (parser.hasErrors()) {
                System.out.println("Разбор завершён с ошибками:");
                for (String err : parser.getErrors()) {
                    System.out.println(" - " + err);
                }
            } else {
                System.out.println("AST:");
                AstPrinter.print(ast);
                System.out.println("Синтаксический анализ завершён успешно. Ошибок не найдено.");
            }

            if (parser.hasErrors()) return;

            SemanticAnalyzer sem = new SemanticAnalyzer(ast);
            sem.analyze();
            if (sem.getErrors().isEmpty()) {
                System.out.println("Семантический анализ завершён успешно. Ошибок не найдено.");
                sem.printSymbolTable();
                // Печать триад
                int i = 1;
                for (Triad t : sem.getTriads()) {
                    System.out.println(i++ + ") " + t);
                }
            } else {
                for (String err : sem.getErrors()) {
                    System.out.println("Ошибка: " + err);
                }
            }
        } catch (ParseException e) {
            System.out.println("Критическая ошибка разбора: " + e.getMessage());
        }
    }
}
