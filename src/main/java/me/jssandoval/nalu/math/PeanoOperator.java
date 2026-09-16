package me.jssandoval.nalu.math;

import me.jssandoval.nalu.base.Alphabet;

public abstract class PeanoOperator {
    protected final Alphabet alphabet;

    public PeanoOperator(Alphabet alphabet) {
        this.alphabet = alphabet;
    }

    public int compare(String a, String b) {
        a = alphabet.clearZeros(a);
        b = alphabet.clearZeros(b);
        if (a.length() != b.length()) {
            return a.length() > b.length() ? 1 : -1;
        }
        return a.compareTo(b);
    }

    public String increment(String num) {
        num = alphabet.clearZeros(num);
        char zero = alphabet.getZero();
        char maxChar = alphabet.getSymbol(alphabet.getBase() - 1);

        // Caso base 1: Si es cero, devolvemos el símbolo 1 (o cero si la base es 1)
        if (num.equals(String.valueOf(zero))) {
            return String.valueOf(alphabet.getBase() > 1 ? alphabet.getSymbol(1) : zero);
        }

        char lastChar = num.charAt(num.length() - 1);
        String prefix = num.substring(0, num.length() - 1);

        // Caso recursivo: Si el último dígito es el máximo (ej. 9), se vuelve 0 y sumamos 1 al prefijo
        if (lastChar == maxChar) {
            return alphabet.clearZeros(increment(prefix) + zero);
        } else {
            // Caso base 2: Solo avanzamos el último dígito
            return prefix + alphabet.nextSymbol(lastChar);
        }
    }

    public String decrement(String num) {
        num = alphabet.clearZeros(num);
        char zero = alphabet.getZero();
        char maxChar = alphabet.getSymbol(alphabet.getBase() - 1);

        if (num.equals(String.valueOf(zero))) {
            throw new ArithmeticException("Underflow estructural.");
        }

        char lastChar = num.charAt(num.length() - 1);
        String prefix = num.substring(0, num.length() - 1);

        if (lastChar == zero) {
            return alphabet.clearZeros(decrement(prefix) + maxChar);
        } else {
            return alphabet.clearZeros(prefix + alphabet.prevSymbol(lastChar));
        }
    }

    // ============ SOPORTE DE SIGNOS ============
    // Estos helpers permiten que cada operación trabaje internamente solo con
    // magnitudes (strings sin '-'), y decida el signo del resultado al final.

    /** Indica si el valor trae signo negativo explícito (evita el caso "-0"). */
    protected boolean isNegative(String value) {
        return value.startsWith("-") && !value.equals("-" + alphabet.getZero());
    }

    /** Devuelve la magnitud del valor, sin el símbolo '-'. */
    protected String abs(String value) {
        return value.startsWith("-") ? value.substring(1) : value;
    }

    /** Antepone '-' a un resultado, salvo que sea cero (nunca "-0"). */
    protected String applyNegativeSign(String unsignedResult) {
        if (unsignedResult.equals(String.valueOf(alphabet.getZero()))) {
            return unsignedResult;
        }
        return "-" + unsignedResult;
    }
}