package me.chromiumore.mtnc.semantic;

import me.chromiumore.mtnc.analyzer.ast.FunctionDecl;
import me.chromiumore.mtnc.analyzer.ast.Parameter;
import me.chromiumore.mtnc.analyzer.ast.Program;
import me.chromiumore.mtnc.analyzer.ast.Block;
import me.chromiumore.mtnc.analyzer.ast.expression.*;
import me.chromiumore.mtnc.analyzer.ast.statement.*;

import java.util.*;

public class SemanticAnalyzer {
    private final Program ast;
    private final List<String> errors;

    // === Таблица символов ===
    private final Deque<Map<String, SymbolInfo>> symbolTable;   // стек областей
    private int scopeCounter = 0;
    private String currentScopeName = "global";

    // Для красивого вывода таблицы
    private static class SymbolTableEntry {
        String name;
        String type;
        boolean declared;
        boolean initialized;
        String scope;
    }
    private final List<SymbolTableEntry> tableEntries = new ArrayList<>();

    // === Таблица функций ===
    private final Map<String, FunctionInfo> functionTable = new HashMap<>();

    // === Триады ===
    private final List<Triad> triads = new ArrayList<>();
    private FunctionInfo currentFunction;

    public SemanticAnalyzer(Program ast) {
        this.ast = ast;
        this.errors = new ArrayList<>();
        this.symbolTable = new ArrayDeque<>();
        enterScope("global");   // глобальная область
    }

    // ---------- Управление областями видимости ----------
    private void enterScope(String scopeName) {
        symbolTable.push(new HashMap<>());
        scopeCounter++;
        if (scopeName != null) {
            currentScopeName = scopeName;
        }
    }

    private void enterScope() {
        enterScope(null);   // имя не меняется, только новая пустая область
    }

    private void exitScope() {
        if (!symbolTable.isEmpty()) {
            symbolTable.pop();
            scopeCounter--;
            // Восстанавливаем имя области (упрощённо: при выходе из блока имя сбрасываем)
            if (symbolTable.isEmpty()) {
                currentScopeName = "global";
            } else {
                // Можно было бы хранить стек имён, но для простоты оставим как есть
                currentScopeName = currentScopeName.contains(".")
                        ? currentScopeName.substring(0, currentScopeName.lastIndexOf('.'))
                        : currentScopeName;
            }
        }
    }

    private SymbolInfo lookup(String name) {
        for (Map<String, SymbolInfo> scope : symbolTable) {
            if (scope.containsKey(name)) return scope.get(name);
        }
        return null;
    }

    private boolean addSymbol(String name, String type, boolean isVal, boolean initialized) {
        Map<String, SymbolInfo> current = symbolTable.peek();   // текущая область
        if (current == null) return false;
        if (current.containsKey(name)) {
            errors.add("Повторное объявление переменной '" + name + "' в одной области видимости.");
            return false;
        }
        SymbolInfo si = new SymbolInfo(name, type, isVal, scopeCounter);
        si.initialized = initialized;
        current.put(name, si);
        // Запись для таблицы
        SymbolTableEntry entry = new SymbolTableEntry();
        entry.name = name;
        entry.type = type != null ? type : "?";
        entry.declared = true;
        entry.initialized = initialized;
        entry.scope = currentScopeName;
        tableEntries.add(entry);
        return true;
    }

    // Упрощённый вызов (initialized = false)
    private boolean addSymbol(String name, String type, boolean isVal) {
        return addSymbol(name, type, isVal, false);
    }

    // ---------- Главный метод анализа ----------
    public void analyze() {
        // Регистрируем функции (включая встроенные)
        for (FunctionDecl f : ast.functions) {
            if (functionTable.containsKey(f.name)) {
                errors.add("Повторное объявление функции '" + f.name + "'.");
            } else {
                functionTable.put(f.name, new FunctionInfo(f.name, f.parameters, f.returnType));
            }
        }
        // Встроенная println
        functionTable.put("println", new FunctionInfo("println",
                Collections.singletonList(new Parameter("arg", "String")), null));

        // Анализируем каждую функцию
        for (FunctionDecl f : ast.functions) {
            analyzeFunction(f);
        }
    }

    private void analyzeFunction(FunctionDecl func) {
        currentFunction = functionTable.get(func.name);
        // Новая область для параметров и тела
        enterScope(func.name);              // например, "main"
        // Параметры добавляются в эту область
        for (Parameter p : func.parameters) {
            addSymbol(p.name, p.type, true, true);   // инициализированы
        }
        // Тело функции
        if (func.body != null) {
            analyzeBlock(func.body);
        }
        exitScope();   // покидаем функцию
        currentFunction = null;
    }

    private void analyzeBlock(Block block) {
        if (block == null) return;
        // Создаём вложенную область для блока
        String parentScope = currentScopeName;
        enterScope(parentScope + ".block");   // "main.block"
        for (Statement stmt : block.statements) {
            analyzeStatement(stmt);
        }
        exitScope();
    }

    // ---------- Анализ операторов ----------
    private void analyzeStatement(Statement stmt) {
        if (stmt instanceof VariableDecl) {
            analyzeVariableDecl((VariableDecl) stmt);
        } else if (stmt instanceof Assignment) {
            analyzeAssignment((Assignment) stmt);
        } else if (stmt instanceof IfStatement) {
            analyzeIf((IfStatement) stmt);
        } else if (stmt instanceof ForStatement) {
            analyzeFor((ForStatement) stmt);
        } else if (stmt instanceof WhileStatement) {
            analyzeWhile((WhileStatement) stmt);
        } else if (stmt instanceof ReturnStatement) {
            analyzeReturn((ReturnStatement) stmt);
        } else if (stmt instanceof ExpressionStatement) {
            analyzeExpression(((ExpressionStatement) stmt).expr);
        } else if (stmt instanceof Block) {
            // вложенный блок (хотя в грамматике такого нет, но для безопасности)
            analyzeBlock((Block) stmt);
        }
    }

    private void analyzeVariableDecl(VariableDecl decl) {
        String initType = analyzeExpression(decl.initializer);
        String varType = decl.type != null ? decl.type : initType;
        if (!varType.equals(initType)) {
            errors.add("Несоответствие типов: переменной '" + decl.name + "' типа " + varType +
                    " присваивается значение типа " + initType + ".");
        }
        // Добавляем в текущую область (уже внутри блока)
        addSymbol(decl.name, varType, decl.isVal, true);
        // Триада присваивания
        int initTriadIdx = triads.size() - 1;
        triads.add(new Triad(":=", decl.name, "^" + (initTriadIdx + 1)));
    }

    private void analyzeAssignment(Assignment assign) {
        SymbolInfo var = lookup(assign.variableName);
        if (var == null) {
            errors.add("Присваивание необъявленной переменной '" + assign.variableName + "'.");
            return;
        }
        if (var.isVal) {
            errors.add("Нельзя присваивать значение неизменяемой переменной '" + assign.variableName + "'.");
        }
        String exprType = analyzeExpression(assign.value);
        if (!var.type.equals(exprType)) {
            errors.add("Несоответствие типов в присваивании: переменная '" + assign.variableName +
                    "' типа " + var.type + " не может принять значение типа " + exprType + ".");
        }
        if (assign.operator.equals("+=") && !var.type.equals("Int")) {
            errors.add("Оператор += допустим только для Int.");
        }
        int valTriadIdx = triads.size() - 1;
        triads.add(new Triad(":=", assign.variableName, "^" + (valTriadIdx + 1)));
    }

    private String analyzeExpression(Expression expr) {
        if (expr instanceof IntLiteral) {
            triads.add(new Triad("const", Integer.toString(((IntLiteral)expr).value), ""));
            return "Int";
        } else if (expr instanceof RealLiteral) {
            triads.add(new Triad("const", Double.toString(((RealLiteral)expr).value), ""));
            return "Real";
        } else if (expr instanceof StringLiteral) {
            triads.add(new Triad("const_str", ((StringLiteral)expr).value, ""));
            return "String";
        } else if (expr instanceof StringTemplateLiteral) {
            triads.add(new Triad("template", ((StringTemplateLiteral)expr).value, ""));
            return "String";
        } else if (expr instanceof IdentifierExpr) {
            String name = ((IdentifierExpr) expr).name;
            SymbolInfo si = lookup(name);
            if (si == null) {
                errors.add("Использование необъявленной переменной '" + name + "'.");
                triads.add(new Triad("error", name, ""));
                return "Int";   // заглушка
            }
            triads.add(new Triad("load", name, ""));
            return si.type;
        } else if (expr instanceof BinaryExpr) {
            BinaryExpr bin = (BinaryExpr) expr;
            String leftType = analyzeExpression(bin.left);
            String rightType = analyzeExpression(bin.right);
            if (!leftType.equals("Int") || !rightType.equals("Int")) {
                errors.add("Бинарная операция '" + bin.operator + "' требует операндов типа Int.");
            }
            int leftTriad = triads.size() - 2;
            int rightTriad = triads.size() - 1;
            triads.add(new Triad(bin.operator, "^" + (leftTriad + 1), "^" + (rightTriad + 1)));
            return "Int";
        } else if (expr instanceof FunctionCall) {
            FunctionCall call = (FunctionCall) expr;
            FunctionInfo func = functionTable.get(call.functionName);
            if (func == null) {
                errors.add("Вызов необъявленной функции '" + call.functionName + "'.");
                for (Expression arg : call.arguments) analyzeExpression(arg);
                triads.add(new Triad("CALL", call.functionName, ""));
                return "Int";   // заглушка
            }
            if (call.arguments.size() != func.params.size()) {
                errors.add("Неверное количество аргументов при вызове '" + call.functionName + "'.");
            } else {
                for (int i = 0; i < call.arguments.size(); i++) {
                    String argType = analyzeExpression(call.arguments.get(i));
                    if (!argType.equals(func.params.get(i).type)) {
                        errors.add("Несоответствие типа аргумента " + (i+1) + " при вызове '" +
                                call.functionName + "': ожидается " + func.params.get(i).type +
                                ", получено " + argType + ".");
                    }
                }
            }
            StringBuilder argsRef = new StringBuilder();
            for (int i = 0; i < call.arguments.size(); i++) {
                if (i > 0) argsRef.append(", ");
                argsRef.append("^").append(triads.size() - call.arguments.size() + i + 1);
            }
            triads.add(new Triad("CALL", call.functionName, argsRef.toString()));
            return func.returnType != null ? func.returnType : "Unit";
        }
        return "unknown";
    }

    // ---------- Управляющие конструкции ----------
    private void analyzeIf(IfStatement stmt) {
        String condType = analyzeExpression(stmt.condition);
        if (!condType.equals("Int")) {
            errors.add("Условие в if должно быть типа Int.");
        }
        int condTriad = triads.size() - 1;
        triads.add(new Triad("JZ", "^" + (condTriad + 1), "L_else"));
        int jzIndex = triads.size() - 1;
        // then
        if (stmt.thenBranch instanceof Block) analyzeBlock((Block)stmt.thenBranch);
        else analyzeStatement(stmt.thenBranch);
        triads.add(new Triad("JMP", "L_end", ""));
        int jmpEndIdx = triads.size() - 1;
        // else
        int elseLabel = triads.size() + 1;
        triads.get(jzIndex).arg2 = "L" + elseLabel;
        if (stmt.elseBranch != null) {
            if (stmt.elseBranch instanceof Block) analyzeBlock((Block)stmt.elseBranch);
            else analyzeStatement(stmt.elseBranch);
        }
        int endLabel = triads.size() + 1;
        triads.get(jmpEndIdx).arg2 = "L" + endLabel;
    }

    private void analyzeFor(ForStatement stmt) {
        String startType = analyzeExpression(stmt.rangeStart);
        if (!startType.equals("Int")) errors.add("Начало диапазона for должно быть Int.");
        int startTriad = triads.size() - 1;
        String endType = analyzeExpression(stmt.rangeEnd);
        if (!endType.equals("Int")) errors.add("Конец диапазона for должен быть Int.");
        int endTriad = triads.size() - 1;

        // Создаём область для переменной цикла
        enterScope(currentScopeName + ".for");
        addSymbol(stmt.loopVariable, "Int", true, true);

        // i = start
        triads.add(new Triad(":=", stmt.loopVariable, "^" + (startTriad + 1)));

        int loopStart = triads.size();
        // условие i <= end
        triads.add(new Triad("<=", stmt.loopVariable, "^" + (endTriad + 1)));
        int condTriad = triads.size() - 1;
        triads.add(new Triad("JZ", "^" + (condTriad + 1), "L_exit"));
        int jzIdx = triads.size() - 1;

        // тело
        analyzeStatement(stmt.body);   // обычно это блок

        // i = i + 1
        triads.add(new Triad("+", stmt.loopVariable, "1"));
        int incTriad = triads.size() - 1;
        triads.add(new Triad(":=", stmt.loopVariable, "^" + (incTriad + 1)));
        triads.add(new Triad("JMP", "L" + (loopStart + 1), ""));

        int exitLabel = triads.size() + 1;
        triads.get(jzIdx).arg2 = "L" + exitLabel;
        exitScope();
    }

    private void analyzeWhile(WhileStatement stmt) {
        int loopStart = triads.size() + 1;
        String condType = analyzeExpression(stmt.condition);
        if (!condType.equals("Int")) {
            errors.add("Условие while должно быть типа Int.");
        }
        int condTriad = triads.size() - 1;
        triads.add(new Triad("JZ", "^" + (condTriad + 1), "L_exit"));
        int jzIdx = triads.size() - 1;
        analyzeStatement(stmt.body);   // блок
        triads.add(new Triad("JMP", "L" + loopStart, ""));
        int exitLabel = triads.size() + 1;
        triads.get(jzIdx).arg2 = "L" + exitLabel;
    }

    private void analyzeReturn(ReturnStatement stmt) {
        if (currentFunction == null) {
            errors.add("return вне функции.");
            return;
        }
        if (stmt.value != null) {
            String retType = analyzeExpression(stmt.value);
            if (!retType.equals(currentFunction.returnType)) {
                errors.add("Несоответствие типа возвращаемого значения: функция '" +
                        currentFunction.name + "' ожидает " + currentFunction.returnType +
                        ", но возвращается " + retType + ".");
            }
            int valTriad = triads.size() - 1;
            triads.add(new Triad("RETURN", "^" + (valTriad + 1), ""));
        } else {
            if (currentFunction.returnType != null) {
                errors.add("Функция '" + currentFunction.name + "' должна возвращать значение типа " +
                        currentFunction.returnType + ".");
            } else {
                triads.add(new Triad("RETURN", "", ""));
            }
        }
    }

    // ---------- Вывод результатов ----------
    public List<String> getErrors() { return errors; }
    public List<Triad> getTriads() { return triads; }

    public void printSymbolTable() {
        System.out.println("Name | Type    | Declared  | Initialized | Scope");
        System.out.println("-----+---------+-----------+-------------+------");
        for (SymbolTableEntry e : tableEntries) {
            System.out.printf("%-5s | %-7s | %-9s | %-11s | %s%n",
                    e.name, e.type, e.declared, e.initialized, e.scope);
        }
    }
}