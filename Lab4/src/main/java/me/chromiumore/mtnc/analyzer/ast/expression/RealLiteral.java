package me.chromiumore.mtnc.analyzer.ast.expression;

public class RealLiteral extends Expression {
    public double value;

    public RealLiteral(double value) {
        this.value = value;
    }
}
