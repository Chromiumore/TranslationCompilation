package me.chromiumore.mtnc.analyzer.ast.statement;

import me.chromiumore.mtnc.analyzer.ast.expression.Expression;

public class VariableDecl extends Statement {
    public boolean isVal;  // true = val, false = var
    public String name;
    public String type;    // может быть null (вывод типа)
    public Expression initializer;

    public VariableDecl(boolean isVal, String name, String type, Expression init) {
        this.isVal = isVal; this.name = name; this.type = type; this.initializer = init;
    }
}
