package me.chromiumore.mtnc.analyzer.ast.expression;

public class IdentifierExpr extends Expression {
    public String name;

    public IdentifierExpr(String n) {
        name = n;
    }
}
