package me.chromiumore.mtnc.analyzer;

import me.chromiumore.mtnc.analyzer.ast.*;
import me.chromiumore.mtnc.analyzer.ast.expression.*;
import me.chromiumore.mtnc.analyzer.ast.statement.*;

import java.util.*;

public class Parser {
    private final List<Token> tokens;
    private int pos = 0;
    private final List<String> errors = new ArrayList<>();
    private Deque<Set<String>> scopes = new ArrayDeque<>();
    private Set<String> importedFunctions = new HashSet<>(Arrays.asList(new String[]{"print", "println"}));

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token peek() {
        return pos < tokens.size() ? tokens.get(pos) : null;
    }

    private Token advance() {
        if (pos >= tokens.size()) throw new ParseException("Unexpected end of input");
        return tokens.get(pos++);
    }

    private boolean matchKeyword(String keyword) {
        Token t = peek();
        return t != null && t.type == TokenType.KEYWORD && t.lexeme.equals(keyword);
    }

    private boolean matchDelim(String delim) {
        Token t = peek();
        return t != null && t.type == TokenType.DELIMITER && t.lexeme.equals(delim);
    }

    private boolean matchOp(String op) {
        Token t = peek();
        return t != null && t.type == TokenType.OPERATOR && t.lexeme.equals(op);
    }

    private Token expectKeyword(String keyword) {
        if (matchKeyword(keyword)) return advance();
        error("Ожидалось ключевое слово '" + keyword + "'");
        return null;
    }

    private Token expectDelim(String delim) {
        if (matchDelim(delim)) return advance();
        error("Ожидался разделитель '" + delim + "'");
        return null;
    }

    private Token expectIdentifier() {
        if (peek() != null && peek().type == TokenType.IDENTIFIER) return advance();
        error("Ожидался идентификатор");
        return null;
    }

    private Token expectOp(String op) {
        if (matchOp(op)) return advance();
        error("Ожидался оператор '" + op + "'");
        return null;
    }

    private void enterScope() {
        scopes.push(new HashSet<>());
    }

    private void exitScope() {
        if (!scopes.isEmpty()) scopes.pop();
    }

    private void declare(String name) {
        if (!scopes.isEmpty()) {
             if (scopes.peek().contains(name)) {
                 error("Попытка объявления переменной/функции, которая уже была объявлена ранее: " + name);
             }
            scopes.peek().add(name);
        }
    }

    private boolean isDeclared(String name) {
        for (Set<String> scope : scopes) {
            if (scope.contains(name)) return true;
        }
        return false;
    }

    private void error(String message) {
        Token tok = peek();
        String posInfo = tok == null ? "EOF" : String.valueOf(tok.position);
        String msg = "Синтаксическая ошибка на токене " + posInfo + ": " + message +
                (tok != null ? " (получено " + tok + ")" : "");
        errors.add(msg);
    }

    private void synchronize() {
        while (peek() != null) {
            Token t = peek();
            // остановка на начале оператора, объявления или закрывающей скобке
            if (t.type == TokenType.KEYWORD &&
                    (t.lexeme.equals("fun") || t.lexeme.equals("val") || t.lexeme.equals("var") ||
                            t.lexeme.equals("if") || t.lexeme.equals("for") || t.lexeme.equals("while") ||
                            t.lexeme.equals("return") || t.lexeme.equals("else"))) return;
            if (t.type == TokenType.DELIMITER && (t.lexeme.equals("}") || t.lexeme.equals(";"))) return;
            advance();
        }
    }

    public Program parse() {
        Program program = new Program();
        while (peek() != null) {
            if (matchKeyword("fun")) {
                FunctionDecl f = functionDecl();
                if (f != null) program.functions.add(f);
            } else {
                error("Ожидалось объявление функции");
                advance();
                synchronize();
            }
        }

        validate(program);
        return program;
    }

    private void validate(Program program) {
        Set<String> declaredFunctions = importedFunctions;
        for (FunctionDecl f : program.functions) {
            if (f.name != null) declaredFunctions.add(f.name);
        }

        validateNode(program, declaredFunctions);
    }

    private void validateNode(ASTNode node, Set<String> declaredFunctions) {
        if (node == null) return;
        if (node instanceof Program) {
            for (FunctionDecl f : ((Program) node).functions) {
                validateNode(f, declaredFunctions);
            }
        } else if (node instanceof FunctionDecl) {
            validateNode(((FunctionDecl) node).body, declaredFunctions);
        } else if (node instanceof Block) {
            for (Statement stmt : ((Block) node).statements) {
                validateNode(stmt, declaredFunctions);
            }
        } else if (node instanceof IfStatement) {
            IfStatement ifStmt = (IfStatement) node;
            validateNode(ifStmt.condition, declaredFunctions);
            validateNode(ifStmt.thenBranch, declaredFunctions);
            validateNode(ifStmt.elseBranch, declaredFunctions);
        } else if (node instanceof ForStatement) {
            ForStatement forStmt = (ForStatement) node;
            validateNode(forStmt.rangeStart, declaredFunctions);
            validateNode(forStmt.rangeEnd, declaredFunctions);
            validateNode(forStmt.body, declaredFunctions);
        } else if (node instanceof WhileStatement) {
            WhileStatement whileStmt = (WhileStatement) node;
            validateNode(whileStmt.condition, declaredFunctions);
            validateNode(whileStmt.body, declaredFunctions);
        } else if (node instanceof ReturnStatement) {
            validateNode(((ReturnStatement) node).value, declaredFunctions);
        } else if (node instanceof ExpressionStatement) {
            validateNode(((ExpressionStatement) node).expr, declaredFunctions);
        } else if (node instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) node;
            if (!declaredFunctions.contains(call.functionName)) {
                errors.add("Синтаксическая ошибка: вызов необъявленной функции '" + call.functionName + "'");
            }
            for (Expression arg : call.arguments) {
                validateNode(arg, declaredFunctions);
            }
        } else if (node instanceof BinaryExpr) {
            BinaryExpr bin = (BinaryExpr) node;
            validateNode(bin.left, declaredFunctions);
            validateNode(bin.right, declaredFunctions);
        }
    }

    private FunctionDecl functionDecl() {
        Deque<Set<String>> savedScopes = scopes;
        scopes = new ArrayDeque<>();
        enterScope();

        expectKeyword("fun");
        Token nameTok = expectIdentifier();
        if (nameTok == null) {
            scopes = savedScopes;
            return null;
        }
        String name = nameTok.lexeme;

        expectDelim("(");
        List<Parameter> params = parameters();
        expectDelim(")");

        String returnType = null;
        if (matchDelim(":")) {
            advance();
            returnType = parseType();
        }

        Block body = block();
        if (body == null) return null;

        FunctionDecl f = new FunctionDecl();
        f.name = name;
        f.parameters = params;
        f.returnType = returnType;
        f.body = body;

        scopes = savedScopes;
        return f;
    }

    private List<Parameter> parameters() {
        List<Parameter> params = new ArrayList<>();
        if (peek() != null && peek().type == TokenType.IDENTIFIER) {
            params.add(parameter());
            while (matchDelim(",")) {
                advance();
                params.add(parameter());
            }
        }
        return params;
    }

    private Parameter parameter() {
        Token id = expectIdentifier();
        expectDelim(":");
        String type = parseType();
        declare(id.lexeme);
        return new Parameter(id != null ? id.lexeme : null, type);
    }

    private String parseType() {
        Token t = peek();
        if (t != null && (t.type == TokenType.IDENTIFIER || t.type == TokenType.KEYWORD)) {
            advance();
            return t.lexeme;
        }
        error("Ожидалось имя типа");
        return "?";
    }

    private Block block() {
        expectDelim("{");
        enterScope();
        Block block = new Block();
        while (peek() != null && !matchDelim("}")) {
            Statement stmt = statement();
            if (stmt != null) block.statements.add(stmt);
            // необязательная точка с запятой после оператора
            if (matchDelim(";")) {
                advance();
            } else if (!matchDelim("}")) {
                // пропущенная точка с запятой допускается только перед следующим оператором
                if (!isStartOfStatement()) {
                    error("Ожидалась ';' или конец блока '}'");
                    synchronize();
                    if (matchDelim("}")) break;
                }
            }
        }
        expectDelim("}");
        exitScope();
        return block;
    }

    private boolean isStartOfStatement() {
        Token t = peek();
        if (t == null) return false;
        if (t.type == TokenType.KEYWORD) {
            return t.lexeme.equals("val") || t.lexeme.equals("var") || t.lexeme.equals("if") ||
                    t.lexeme.equals("for") || t.lexeme.equals("while") || t.lexeme.equals("return");
        }
        return t.type == TokenType.IDENTIFIER; // присваивание или выражение-оператор
    }

    private Statement statement() {
        Token t = peek();
        if (t == null) return null;
        if (t.type == TokenType.KEYWORD) {
            switch (t.lexeme) {
                case "val":
                case "var": return variableDecl();
                case "if": return ifStatement();
                case "for": return forStatement();
                case "while": return whileStatement();
                case "return": return returnStatement();
                default:
                    error("Неожиданное ключевое слово '" + t.lexeme + "' в начале оператора");
                    advance();
                    synchronize();
                    return null;
            }
        } else if (t.type == TokenType.IDENTIFIER) {
            // Предпросмотр для различения присваивания и выражения-оператора
            if (isNextAssignmentOp()) {
                return assignment();
            } else {
                Expression expr = expression();
                return new ExpressionStatement(expr);
            }
        } else {
            error("Неожиданный токен " + t + " в начале оператора");
            advance();
            synchronize();
            return null;
        }
    }

    private boolean isNextAssignmentOp() {
        if (pos + 1 < tokens.size()) {
            Token next = tokens.get(pos + 1);
            return next.type == TokenType.OPERATOR &&
                    (next.lexeme.equals("=") || next.lexeme.equals("+="));
        }
        return false;
    }

    private VariableDecl variableDecl() {
        boolean isVal = matchKeyword("val");
        Token kw = advance(); // val или var
        if (isVal != (kw.lexeme.equals("val"))) isVal = kw.lexeme.equals("val");

        Token id = expectIdentifier();
        String type = null;
        if (matchDelim(":")) {
            advance();
            type = parseType();
        }
        Expression init = null;
        if (matchOp("=")) {
            advance();
            init = expression();
        } else if (type == null) {
            // Если тип не указан, инициализатор обязателен для вывода типа
            error("Объявление переменной без типа требует инициализатор ' = <выражение>'");
        }

        declare(id.lexeme);
        return new VariableDecl(isVal, id.lexeme, type, init);
    }

    private Assignment assignment() {
        Token id = expectIdentifier();
        String varName = id.lexeme;
        // Проверка существования переменной
        if (!isDeclared(varName)) {
            error("Необъявленная переменная: " + varName);
        }
        String op;
        if (matchOp("=")) {
            advance();
            op = "=";
        } else if (matchOp("+=")) {
            advance();
            op = "+=";
        } else {
            error("Ожидался оператор '=' или '+=' в присваивании");
            return null;
        }
        Expression value = expression();
        return new Assignment(id.lexeme, op, value);
    }

    private IfStatement ifStatement() {
        expectKeyword("if");
        expectDelim("(");
        Expression cond = expression();
        expectDelim(")");
        Statement thenBr = block();
        Statement elseBr = null;
        if (matchKeyword("else")) {
            advance();
            if (matchKeyword("if")) {
                elseBr = ifStatement();
            } else {
                elseBr = block();
            }
        }
        return new IfStatement(cond, thenBr, elseBr);
    }

    private ForStatement forStatement() {
        expectKeyword("for");
        expectDelim("(");
        Token id = expectIdentifier();
        String loopVar = id.lexeme;
        expectKeyword("in");
        Expression start = expression();
        expectOp("..");
        Expression end = expression();
        expectDelim(")");

        enterScope();
        declare(loopVar);
        Statement body = block();   // тело цикла – блок
        exitScope();

        return new ForStatement(loopVar, start, end, body);
    }

    private WhileStatement whileStatement() {
        expectKeyword("while");
        expectDelim("(");
        Expression cond = expression();
        expectDelim(")");
        Statement body = block();   // тело цикла – блок
        return new WhileStatement(cond, body);
    }

    private ReturnStatement returnStatement() {
        expectKeyword("return");
        Expression expr = null;
        if (peek() != null && !matchDelim("}") && !matchDelim(";")) {
            expr = expression();
        }
        return new ReturnStatement(expr);
    }

    private Expression expression() {
        return expression(0);
    }

    private Expression expression(int minPrec) {
        Expression left = primary();
        while (peek() != null) {
            Token t = peek();
            if (t.type == TokenType.OPERATOR && isBinaryOp(t.lexeme)) {
                int prec = precedence(t.lexeme);
                if (prec < minPrec) break;
                advance();
                Expression right = expression(prec + 1);
                left = new BinaryExpr(left, t.lexeme, right);
            } else {
                break;
            }
        }
        return left;
    }

    private boolean isBinaryOp(String op) {
        return op.equals("+") || op.equals("*") || op.equals("%") ||
                 op.equals("<") || op.equals("==");
    }

    private int precedence(String op) {
        switch (op) {
            case "<": case "==": return 2;
            case "+": return 3;
            case "*": case "%": return 4;
            default: return 0;
        }
    }

    private Expression primary() {
        Token t = peek();
        if (t == null) {
            error("Неожиданный конец выражения");
            throw new ParseException("Unexpected end of expression");
        }
        if (t.type == TokenType.CONSTANT_INT) {
            advance();
            return new IntLiteral(Integer.parseInt(t.lexeme));
        } else if (t.type == TokenType.CONSTANT_REAL) {
            advance();
            return new RealLiteral(Double.parseDouble(t.lexeme));
        }
        else if (t.type == TokenType.CONSTANT_STRING) {
            advance();
            String s = t.lexeme;
            if (s.contains("${")) {
                return new StringTemplateLiteral(s);
            } else {
                return new StringLiteral(s);
            }
        } else if (t.type == TokenType.IDENTIFIER) {
            advance();
            if (matchDelim("(")) {
                return finishCall(t.lexeme);
            } else {
                if (!isDeclared(t.lexeme)) {
                    error("Необъявленная переменная: " + t.lexeme);
                }
                return new IdentifierExpr(t.lexeme);
            }
        } else if (matchDelim("(")) {
            advance();
            Expression expr = expression();
            expectDelim(")");
            return expr;
        } else {
            error("Ожидалось выражение, получено " + t);
            advance();
            synchronize();
            return new IntLiteral(0); // заглушка
        }
    }

    private FunctionCall finishCall(String name) {
        expectDelim("(");
        FunctionCall call = new FunctionCall(name);
        if (!matchDelim(")")) {
            call.arguments.add(expression());
            while (matchDelim(",")) {
                advance();
                call.arguments.add(expression());
            }
        }
        expectDelim(")");
        return call;
    }

    public List<String> getErrors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }
}
