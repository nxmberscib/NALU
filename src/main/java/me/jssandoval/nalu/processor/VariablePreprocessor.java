package me.jssandoval.nalu.processor;

import me.jssandoval.nalu.base.Alphabet;

public class VariablePreprocessor {

    private static final String STANDARD_SYMBOLS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String normalizeOperand(String input, Alphabet targetAlphabet) {
        if (input == null || input.isEmpty()) return input;

        boolean isNegative = input.startsWith("-");
        String cleanInput = input.replace("-", "").toUpperCase();

        int targetBase = targetAlphabet.getBase();
        int maxSymbolIndex = -1;

        // 1. Detectar el símbolo más alto presente en la variable
        for (char c : cleanInput.toCharArray()) {
            if (c == '.') continue;
            int index = STANDARD_SYMBOLS.indexOf(c);
            if (index > maxSymbolIndex) maxSymbolIndex = index;
        }

        int detectedBase = Math.max(maxSymbolIndex + 1, 2);

        // 2. Si encaja en la base N actual, no requiere conversión
        if (detectedBase <= targetBase) {
            return input;
        }

        // 3. Convertir de detectedBase a la base N activa
        return convertBase(cleanInput, isNegative, detectedBase, targetAlphabet);
    }

    private static String convertBase(String cleanInput, boolean isNegative, int sourceBase, Alphabet targetAlphabet) {
        String[] parts = cleanInput.split("\\.");

        // Conversión de parte entera
        long decimalInt = Long.parseLong(parts[0], sourceBase);
        String convertedInt = toAlphabetBase(decimalInt, targetAlphabet);

        // Conversión de parte fraccionaria (si existe)
        String convertedFrac = "";
        if (parts.length > 1) {
            double decimalFrac = 0;
            for (int i = 0; i < parts[1].length(); i++) {
                int digitVal = STANDARD_SYMBOLS.indexOf(parts[1].charAt(i));
                decimalFrac += digitVal / Math.pow(sourceBase, i + 1);
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6 && decimalFrac > 0; i++) {
                decimalFrac *= targetAlphabet.getBase();
                int digit = (int) decimalFrac;
                sb.append(targetAlphabet.getSymbol(digit));
                decimalFrac -= digit;
            }
            convertedFrac = sb.toString();
        }

        String result = convertedFrac.isEmpty() ? convertedInt : convertedInt + "." + convertedFrac;
        return isNegative ? "-" + result : result;
    }

    private static String toAlphabetBase(long value, Alphabet targetAlphabet) {
        if (value == 0) return String.valueOf(targetAlphabet.getZero());

        int base = targetAlphabet.getBase();
        StringBuilder sb = new StringBuilder();
        while (value > 0) {
            int remainder = (int) (value % base);
            sb.append(targetAlphabet.getSymbol(remainder));
            value /= base;
        }
        return sb.reverse().toString();
    }
}