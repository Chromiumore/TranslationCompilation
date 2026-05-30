package me.chromiumore.mtnc.semantic;

import me.chromiumore.mtnc.analyzer.ast.Parameter;

import java.util.List;

// Информация о функции
class FunctionInfo {
    String name;
    List<Parameter> params;
    String returnType;   // null для Unit

    FunctionInfo(String name, List<Parameter> params, String returnType) {
        this.name = name;
        this.params = params;
        this.returnType = returnType;
    }
}
