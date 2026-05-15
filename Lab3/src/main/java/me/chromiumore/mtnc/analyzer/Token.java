package me.chromiumore.mtnc.analyzer;

public class Token {
    public TokenType type;
    public String lexeme;
    public int position;

    public Token(TokenType type, String lexeme, int position) {
        this.type = type;
        this.lexeme = lexeme;
        this.position = position;
    }

    @Override
    public String toString() {
        return "(" + type + ", " + lexeme + ")";
    }
}
