package me.chromiumore.mtnc.analyzer.ast.statement;

import me.chromiumore.mtnc.analyzer.ast.expression.Expression;

// Выражение-оператор (например, вызов функции как отдельный оператор)
public class ExpressionStatement extends Statement {
    public Expression expr;

    public ExpressionStatement(Expression expr) { this.expr = expr; }
}
