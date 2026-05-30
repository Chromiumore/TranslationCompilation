package me.chromiumore.mtnc.analyzer.ast.expression;

public class StringTemplateLiteral extends Expression {
    public String value;

    public StringTemplateLiteral(String v) {
        value = v;
    }
}
