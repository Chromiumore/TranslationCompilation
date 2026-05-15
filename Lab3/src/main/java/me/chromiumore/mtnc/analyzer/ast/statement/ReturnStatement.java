package me.chromiumore.mtnc.analyzer.ast.statement;

import me.chromiumore.mtnc.analyzer.ast.expression.Expression;

public class ReturnStatement extends Statement {
    public Expression value;  // может быть null

    public ReturnStatement(Expression value) { this.value = value; }
}
