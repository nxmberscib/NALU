package me.jssandoval.nalu.base;

public class Alphabet {
    private static final String MASTER_ALPHABET = "0123456789ABCDEFGHIJKLMNÑOPQRSTUVWXYZabcdefghijklmnñopqrstuvwxyz";
    private final int base;
    private final String currentAlphabet;

    public Alphabet(int base) {
        if (base < 2 || base > MASTER_ALPHABET.length()) {
            throw new IllegalArgumentException("Base fuera de rango (2-" + MASTER_ALPHABET.length() + ")");
        }
        this.base = base;
        this.currentAlphabet = MASTER_ALPHABET.substring(0, base);
    }

    public int getBase() {
        return base;
    }

    public char getZero() {
        return currentAlphabet.charAt(0);
    }

    public char getSymbol(int index) {
        return currentAlphabet.charAt(index);
    }

    public int getIndex(char symbol) {
        int idx = currentAlphabet.indexOf(symbol);
        if (idx == -1) throw new IllegalArgumentException("Símbolo '" + symbol + "' no pertenece al alfabeto.");
        return idx;
    }

    public char nextSymbol(char symbol) {
        return currentAlphabet.charAt((getIndex(symbol) + 1) % base);
    }

    public char prevSymbol(char symbol) {
        return currentAlphabet.charAt((getIndex(symbol) - 1 + base) % base);
    }

    public String clearZeros(String num) {
        char zero = getZero();
        while (num.length() > 1 && num.charAt(0) == zero) {
            num = num.substring(1);
        }
        return num.isEmpty() ? String.valueOf(zero) : num;
    }
}