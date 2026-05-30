package me.chromiumore.mtnc.analyzer.ast.statement;

import me.chromiumore.mtnc.analyzer.ast.expression.Expression;

public class ForStatement extends Statement {
    public String loopVariable;
    public Expression rangeStart;
    public Expression rangeEnd;
    public Statement body;

    public ForStatement(String var, Expression start, Expression end, Statement body) {
        this.loopVariable = var; this.rangeStart = start; this.rangeEnd = end; this.body = body;
    }
}
