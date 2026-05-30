package me.chromiumore.mtnc.analyzer.ast.expression;

import java.util.ArrayList;
import java.util.List;

public class FunctionCall extends Expression {
    public String functionName;
    public List<Expression> arguments = new ArrayList<>();

    public FunctionCall(String name) { this.functionName = name; }
}
