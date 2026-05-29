package me.chromiumore.mtnc.semantic;

// Триада
public class Triad {
    String op;
    String arg1, arg2; // строки; для ссылок на другие триады храним "^N"

    Triad(String op, String arg1, String arg2) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
    }

    @Override
    public String toString() {
        return "(" + op + ", " + arg1 +
                (arg2.isEmpty() ? "" : ", " + arg2) + ")";
    }
}
