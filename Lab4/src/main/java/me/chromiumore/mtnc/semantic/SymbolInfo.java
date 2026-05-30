package me.chromiumore.mtnc.semantic;

// Запись в таблице символов
class SymbolInfo {
    String name;
    String type;
    boolean isVal;
    boolean initialized;
    int scopeLevel;
    SymbolTableEntry tableEntry;

    SymbolInfo(String name, String type, boolean isVal, int scopeLevel, SymbolTableEntry entry) {
        this.name = name;
        this.type = type;
        this.isVal = isVal;
        this.initialized = false;
        this.scopeLevel = scopeLevel;
        this.tableEntry = entry;
    }
}
