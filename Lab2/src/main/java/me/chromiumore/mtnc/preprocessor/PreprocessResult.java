package me.chromiumore.mtnc.preprocessor;

public record PreprocessResult (
        String program,
        ValidationResult validationResult
) {}
