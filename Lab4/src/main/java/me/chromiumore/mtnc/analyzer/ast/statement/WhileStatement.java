package me.chromiumore.mtnc.analyzer.ast.statement;

import me.chromiumore.mtnc.analyzer.ast.expression.Expression;

public class WhileStatement extends Statement {
    public Expression condition;
    public Statement body;

    public WhileStatement(Expression cond, Statement body) { this.condition = cond; this.body = body; }
}
