package me.chromiumore.mtnc;

import me.chromiumore.mtnc.analyzer.LexicalAnalyzer;
import me.chromiumore.mtnc.preprocessor.PreprocessResult;
import me.chromiumore.mtnc.preprocessor.Preprocessor;
import me.chromiumore.mtnc.preprocessor.ValidationResult;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            throw new RuntimeException("Usage: java Preprocessor <inputFile>");
        }

        PreprocessResult preprocessResult = Preprocessor.preprocessFile(args[0]);
        ValidationResult validationResult = preprocessResult.validationResult();

        System.out.println(preprocessResult.program());
        System.out.println("\n" + validationResult.getMessage());

        LexicalAnalyzer analyzer = new LexicalAnalyzer(preprocessResult.program());
        analyzer.analyze();

        analyzer.printResults();
    }
}
