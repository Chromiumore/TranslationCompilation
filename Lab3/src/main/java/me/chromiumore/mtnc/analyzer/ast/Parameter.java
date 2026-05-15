package me.chromiumore.mtnc.analyzer.ast;

public class Parameter extends ASTNode {
    public String name;
    public String type;

    public Parameter(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
