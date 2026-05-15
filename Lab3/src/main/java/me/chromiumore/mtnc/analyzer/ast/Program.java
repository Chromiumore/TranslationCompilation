package me.chromiumore.mtnc.analyzer.ast;

import java.util.ArrayList;
import java.util.List;

public class Program extends ASTNode {
    public List<FunctionDecl> functions = new ArrayList<>();
}
