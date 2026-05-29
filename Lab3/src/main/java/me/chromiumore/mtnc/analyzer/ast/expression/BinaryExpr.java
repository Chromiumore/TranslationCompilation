package me.chromiumore.mtnc.analyzer.ast.expression;

public class BinaryExpr extends Expression {
    public Expression left;
    public String operator;  // "+", "*", "%", "<", "=="
    public Expression right;

    public BinaryExpr(Expression left, String op, Expression right) {
        this.left = left; this.operator = op; this.right = right;
    }
}
