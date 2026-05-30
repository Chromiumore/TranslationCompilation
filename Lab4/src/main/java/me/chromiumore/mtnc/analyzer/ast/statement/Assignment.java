package me.chromiumore.mtnc.analyzer.ast.statement;

import me.chromiumore.mtnc.analyzer.ast.expression.Expression;

public class Assignment extends Statement {
    public String variableName;
    public String operator;  // "=" или "+="
    public Expression value;

    public Assignment(String varName, String op, Expression value) {
        this.variableName = varName; this.operator = op; this.value = value;
    }
}
