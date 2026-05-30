package me.chromiumore.mtnc.semantic;

// Запись в таблице символов
class SymbolInfo {
    String name;
    String type;
    boolean isVal;
    boolean initialized;
    int scopeLevel;   // для отладки, можно не использовать

    SymbolInfo(String name, String type, boolean isVal, int scopeLevel) {
        this.name = name;
        this.type = type;
        this.isVal = isVal;
        this.initialized = false;
        this.scopeLevel = scopeLevel;
    }
}
