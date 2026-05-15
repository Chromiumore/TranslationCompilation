package me.chromiumore.mtnc.analyzer.ast.expression;

public class StringLiteral extends Expression {
    public String value;

    public StringLiteral(String v) {
        value = v;
    }
}
