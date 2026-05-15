package me.chromiumore.mtnc.analyzer.ast;

import java.util.ArrayList;
import java.util.List;

public class FunctionDecl extends ASTNode {
    public String name;
    public List<Parameter> parameters = new ArrayList<>();
    public String returnType;
    public Block body;
}
