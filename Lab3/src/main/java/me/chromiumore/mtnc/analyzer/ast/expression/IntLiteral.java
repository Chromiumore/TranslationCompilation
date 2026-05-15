package me.chromiumore.mtnc.analyzer.ast.expression;

public class IntLiteral extends Expression {
    public int value;

    public IntLiteral(int v) {
        value = v;
    }
}
