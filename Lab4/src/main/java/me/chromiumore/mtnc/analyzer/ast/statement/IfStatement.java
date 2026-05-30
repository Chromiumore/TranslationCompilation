package me.chromiumore.mtnc.analyzer.ast.statement;

import me.chromiumore.mtnc.analyzer.ast.expression.Expression;

public class IfStatement extends Statement {
    public Expression condition;
    public Statement thenBranch;
    public Statement elseBranch;  // может быть null

    public IfStatement(Expression cond, Statement thenBr, Statement elseBr) {
        this.condition = cond; this.thenBranch = thenBr; this.elseBranch = elseBr;
    }
}
