package me.chromiumore.mtnc.analyzer.ast;

import me.chromiumore.mtnc.analyzer.ast.statement.Statement;

import java.util.ArrayList;
import java.util.List;

    public class Block extends Statement {
    public List<Statement> statements = new ArrayList<>();
}
