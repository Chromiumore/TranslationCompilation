package me.chromiumore.mtnc.analyzer;

import me.chromiumore.mtnc.analyzer.ast.*;
import me.chromiumore.mtnc.analyzer.ast.expression.*;
import me.chromiumore.mtnc.analyzer.ast.statement.*;

import java.util.ArrayList;
import java.util.List;

public class AstPrinter {
    public static void print(ASTNode node) {
        System.out.println(format(node));
    }

    public static String format(ASTNode node) {
        StringBuilder sb = new StringBuilder();
        format(node, "", true, sb);
        return sb.toString();
    }

    private static void format(ASTNode node, String prefix, boolean isLast, StringBuilder sb) {
        if (node == null) return;
        sb.append(prefix);
        sb.append(isLast ? "└── " : "├── ");
        String nodeDesc = nodeToString(node);
        sb.append(nodeDesc).append("\n");
        List<ASTNode> children = getChildren(node);
        for (int i = 0; i < children.size(); i++) {
            boolean childLast = (i == children.size() - 1);
            String newPrefix = prefix + (isLast ? "    " : "│   ");
            format(children.get(i), newPrefix, childLast, sb);
        }
    }

    private static String nodeToString(ASTNode node) {
        if (node instanceof Program) return "Program";
        if (node instanceof FunctionDecl) {
            FunctionDecl f = (FunctionDecl) node;
            String params = f.parameters.stream()
                    .map(p -> p.name + ": " + p.type)
                    .reduce((a, b) -> a + ", " + b).orElse("");
            return "function " + f.name + "(" + params + ")" +
                    (f.returnType != null ? ": " + f.returnType : "");
        }
        if (node instanceof Parameter) {
            Parameter p = (Parameter) node;
            return "param " + p.name + ": " + p.type;
        }
        if (node instanceof Block) return "block";
        if (node instanceof VariableDecl) {
            VariableDecl v = (VariableDecl) node;
            String desc = (v.isVal ? "val " : "var ") + v.name +
                    (v.type != null ? ": " + v.type : "");
            if (v.initializer != null) desc += " =";
            // если инициализатора нет, то без "="
            return desc;
        }
        if (node instanceof Assignment) {
            Assignment a = (Assignment) node;
            return a.variableName + " " + a.operator;
        }
        if (node instanceof IfStatement) return "if";
        if (node instanceof ForStatement) {
            ForStatement f = (ForStatement) node;
            return "for " + f.loopVariable + " in";
        }
        if (node instanceof WhileStatement) return "while";
        if (node instanceof ReturnStatement) return "return";
        if (node instanceof ExpressionStatement) return "expr";
        if (node instanceof FunctionCall) {
            FunctionCall c = (FunctionCall) node;
            return "call " + c.functionName + "(...)";
        }
        if (node instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) node;
            return "binary " + b.operator;
        }
        if (node instanceof IntLiteral) return "int " + ((IntLiteral) node).value;
        if (node instanceof RealLiteral) return "real " + ((RealLiteral)node).value;
        if (node instanceof StringLiteral) return "string \"" + ((StringLiteral) node).value + "\"";
        if (node instanceof StringTemplateLiteral)
            return "template \"" + ((StringTemplateLiteral) node).value + "\"";
        if (node instanceof IdentifierExpr) return "id " + ((IdentifierExpr) node).name;
        return node.getClass().getSimpleName();
    }

    private static List<ASTNode> getChildren(ASTNode node) {
        List<ASTNode> list = new ArrayList<>();
        if (node instanceof Program) {
            list.addAll(((Program) node).functions);
        } else if (node instanceof FunctionDecl) {
            FunctionDecl f = (FunctionDecl) node;
            list.addAll(f.parameters);
            if (f.body != null) list.add(f.body);
        } else if (node instanceof Block) {
            list.addAll(((Block) node).statements);
        } else if (node instanceof VariableDecl) {
            VariableDecl v = (VariableDecl) node;
            if (v.initializer != null)
                list.add(v.initializer);
        } else if (node instanceof Assignment) {
            if (((Assignment) node).value != null)
                list.add(((Assignment) node).value);
        } else if (node instanceof IfStatement) {
            IfStatement i = (IfStatement) node;
            if (i.condition != null) list.add(i.condition);
            if (i.thenBranch != null) list.add(i.thenBranch);
            if (i.elseBranch != null) list.add(i.elseBranch);
        } else if (node instanceof ForStatement) {
            ForStatement f = (ForStatement) node;
            if (f.rangeStart != null) list.add(f.rangeStart);
            if (f.rangeEnd != null) list.add(f.rangeEnd);
            if (f.body != null) list.add(f.body);
        } else if (node instanceof WhileStatement) {
            WhileStatement w = (WhileStatement) node;
            if (w.condition != null) list.add(w.condition);
            if (w.body != null) list.add(w.body);
        } else if (node instanceof ReturnStatement) {
            if (((ReturnStatement) node).value != null)
                list.add(((ReturnStatement) node).value);
        } else if (node instanceof ExpressionStatement) {
            if (((ExpressionStatement) node).expr != null)
                list.add(((ExpressionStatement) node).expr);
        } else if (node instanceof FunctionCall) {
            list.addAll(((FunctionCall) node).arguments);
        } else if (node instanceof BinaryExpr) {
            BinaryExpr b = (BinaryExpr) node;
            if (b.left != null) list.add(b.left);
            if (b.right != null) list.add(b.right);
        }
        return list;
    }
}