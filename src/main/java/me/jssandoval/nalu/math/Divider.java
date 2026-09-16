package me.jssandoval.nalu.math;

import me.jssandoval.nalu.base.Alphabet;
import me.jssandoval.nalu.processor.CalculationResult;
import me.jssandoval.nalu.processor.DecimalAligner;

import java.util.ArrayList;
import java.util.List;

public class Divider extends PeanoOperator {

    private final Subtracter subtracter;
    private final Multiplier multiplier;

    public Divider(Alphabet alphabet) {
        super(alphabet);
        this.subtracter = new Subtracter(alphabet);
        // Instanciamos el multiplicador para hallar el sustraendo rápidamente
        this.multiplier = new Multiplier(alphabet);
    }

    // --- Herramientas de dibujo visual ---
    private String padRight(String text, int desiredLength) {
        if (text.length() >= desiredLength) return text;
        return text + " ".repeat(desiredLength - text.length());
    }

    private String alignRight(String text, int dividendEndIndex) {
        // El dividendo original siempre empieza con 2 espacios de margen ("  ")
        int targetStringIndex = 2 + dividendEndIndex;
        int spacesNeeded = targetStringIndex - text.length() + 1;
        if (spacesNeeded < 0) spacesNeeded = 0;
        return " ".repeat(spacesNeeded) + text;
    }

    public CalculationResult[] divide(String dividend, String divisor) {
        boolean negDividend = isNegative(dividend);
        boolean negDivisor = isNegative(divisor);

        CalculationResult[] magnitudes = divideMagnitudes(abs(dividend), abs(divisor));
        String zeroString = String.valueOf(alphabet.getZero());

        boolean negQuotient = negDividend != negDivisor;
        boolean negRemainder = negDividend; // El residuo conserva el signo del dividendo

        String quotient = magnitudes[0].getResult();
        String remainder = magnitudes[1].getResult();

        if (negQuotient && !quotient.equals(zeroString)) {
            quotient = applyNegativeSign(quotient);
        }
        if (negRemainder && !remainder.equals(zeroString)) {
            remainder = applyNegativeSign(remainder);
        }

        return new CalculationResult[]{
                new CalculationResult(quotient, magnitudes[0].getVisualProcess()),
                new CalculationResult(remainder, magnitudes[1].getVisualProcess())
        };
    }

    private CalculationResult[] divideMagnitudes(String dividend, String divisor) {
        // --- 1. ALINEACIÓN DECIMAL ---
        DecimalAligner aligner = new DecimalAligner(alphabet, dividend, divisor);
        int maximumFractionalLength = aligner.getMaximumFractionalLength();

        String alignedDividend = alphabet.clearZeros(aligner.getLeftOperand());
        String alignedDivisor = alphabet.clearZeros(aligner.getRightOperand());
        char zeroSymbol = alphabet.getZero();
        String zeroString = String.valueOf(zeroSymbol);

        if (alignedDivisor.equals(zeroString)) {
            throw new ArithmeticException("Error Matemático: División por cero no está definida.");
        }

        List<String> leftLines = new ArrayList<>();
        leftLines.add("  " + alignedDividend);

        StringBuilder integerQuotientBuilder = new StringBuilder();
        StringBuilder fractionalQuotientBuilder = new StringBuilder();
        String currentBlock = "";
        boolean firstSubtractionDone = false;

        // --- 2. PARTE ENTERA DE LA DIVISIÓN ---
        for (int i = 0; i < alignedDividend.length(); i++) {
            char nextDigit = alignedDividend.charAt(i);

            if (currentBlock.equals(zeroString)) {
                currentBlock = String.valueOf(nextDigit);
            } else {
                currentBlock += nextDigit;
            }

            if (compare(currentBlock, alignedDivisor) < 0) {
                integerQuotientBuilder.append(zeroSymbol);
            } else {
                String tempRemainder = currentBlock;
                String partialQuotientDigit = zeroString; // Usar String en lugar de char

                while (compare(tempRemainder, alignedDivisor) >= 0) {
                    tempRemainder = subtracter.subtract(tempRemainder, alignedDivisor).getResult();
                    partialQuotientDigit = increment(partialQuotientDigit);
                }

                integerQuotientBuilder.append(partialQuotientDigit);

                String subtractionAmount = multiplier.multiply(partialQuotientDigit, alignedDivisor).getResult();

                if (firstSubtractionDone) {
                    String blockStr = alphabet.clearZeros(currentBlock);
                    if (blockStr.isEmpty()) blockStr = zeroString;
                    leftLines.add(alignRight(blockStr, i));
                }

                leftLines.add(alignRight("- " + subtractionAmount, i));
                leftLines.add(alignRight("-".repeat(subtractionAmount.length() + 2), i));

                currentBlock = tempRemainder;
                firstSubtractionDone = true;
            }
        }

        String integerQuotient = alphabet.clearZeros(integerQuotientBuilder.toString());
        if (integerQuotient.isEmpty()) integerQuotient = zeroString;

        // --- 3. PARTE FRACCIONARIA (CÁLCULO DE DECIMALES) ---
        int maxPrecision = 10; // Límite de cifras decimales calculadas
        int fractionalDigitsCount = 0;

        // Limpiamos el residuo entero antes de entrar a calcular decimales
        currentBlock = alphabet.clearZeros(currentBlock);
        if (currentBlock.isEmpty()) currentBlock = zeroString;

        if (!currentBlock.equals(zeroString)) {
            // CORRECCIÓN 1: Usamos zeroString y evitamos el bucle infinito
            while (!currentBlock.equals(zeroString) && fractionalDigitsCount < maxPrecision) {
                currentBlock += zeroSymbol; // Bajamos un cero decimal
                int virtualIndex = alignedDividend.length() + fractionalDigitsCount;

                if (compare(currentBlock, alignedDivisor) < 0) {
                    fractionalQuotientBuilder.append(zeroSymbol);
                } else {
                    String tempRemainder = currentBlock;
                    String partialQuotientDigit = zeroString;

                    while (compare(tempRemainder, alignedDivisor) >= 0) {
                        tempRemainder = subtracter.subtract(tempRemainder, alignedDivisor).getResult();
                        partialQuotientDigit = increment(partialQuotientDigit);
                    }

                    fractionalQuotientBuilder.append(partialQuotientDigit);

                    String subtractionAmount = multiplier.multiply(partialQuotientDigit, alignedDivisor).getResult();

                    String blockStr = alphabet.clearZeros(currentBlock);
                    if (blockStr.isEmpty()) blockStr = zeroString;
                    leftLines.add(alignRight(blockStr, virtualIndex));

                    leftLines.add(alignRight("- " + subtractionAmount, virtualIndex));
                    leftLines.add(alignRight("-".repeat(subtractionAmount.length() + 2), virtualIndex));

                    currentBlock = tempRemainder;
                    firstSubtractionDone = true;
                }

                // CORRECCIÓN 2: Limpiamos ceros acumulados al final de cada ciclo fraccionario
                currentBlock = alphabet.clearZeros(currentBlock);
                if (currentBlock.isEmpty()) currentBlock = zeroString;

                fractionalDigitsCount++;
            }
        }

        // Armamos el cociente completo uniendo entero y decimal
        String fullQuotient = integerQuotient;
        String fractionalPart = alphabet.clearZeros(fractionalQuotientBuilder.toString());

        while (fractionalPart.endsWith(zeroString) && !fractionalPart.isEmpty()) {
            fractionalPart = fractionalPart.substring(0, fractionalPart.length() - 1);
        }

        if (!fractionalPart.isEmpty()) {
            fullQuotient += "." + fractionalPart;
        }

        String integerRemainder = currentBlock; // Ya viene limpio del bucle

        if (firstSubtractionDone) {
            int lastIndex = alignedDividend.length() - 1 + fractionalDigitsCount;
            leftLines.add(alignRight(integerRemainder, Math.max(alignedDividend.length() - 1, lastIndex)));
        } else {
            leftLines.add(alignRight("-".repeat(alignedDividend.length() + 1), alignedDividend.length() - 1));
            leftLines.add(alignRight(integerRemainder, alignedDividend.length() - 1));
        }

        // --- 4. ENSAMBLAJE DE LA TABLA ASCII ---
        StringBuilder processLog = new StringBuilder();
        processLog.append("Método: División Larga con Decimales (Algoritmo Tradicional)\n");
        processLog.append("==============================================\n\n");

        int maxLeftWidth = alignedDividend.length() + Math.max(fractionalDigitsCount, 0) + 2;

        for (int i = 0; i < leftLines.size(); i++) {
            String line = leftLines.get(i);
            if (i == 0) {
                processLog.append(padRight(line, maxLeftWidth)).append(" | ").append(alignedDivisor).append("\n");
            } else if (i == 1) {
                processLog.append(padRight(line, maxLeftWidth)).append(" |").append("-".repeat(alignedDivisor.length() + 1)).append("\n");
            } else if (i == 2) {
                processLog.append(padRight(line, maxLeftWidth)).append("   ").append(fullQuotient).append("\n");
            } else {
                processLog.append(line).append("\n");
            }
        }

        // Escalar residuo
        String scaledRemainder = integerRemainder;

        int totalDecimalShift = maximumFractionalLength + fractionalDigitsCount;

        if (totalDecimalShift > 0) {
            while (scaledRemainder.length() <= totalDecimalShift) {
                scaledRemainder = zeroSymbol + scaledRemainder;
            }

            int decIndex = scaledRemainder.length() - totalDecimalShift;
            scaledRemainder = scaledRemainder.substring(0, decIndex) + "." + scaledRemainder.substring(decIndex);

            while (scaledRemainder.endsWith(zeroString) && scaledRemainder.contains(".")) {
                scaledRemainder = scaledRemainder.substring(0, scaledRemainder.length() - 1);
            }
            if (scaledRemainder.endsWith(".")) {
                scaledRemainder = scaledRemainder.substring(0, scaledRemainder.length() - 1);
            }

            String[] finalParts = scaledRemainder.split("\\.");
            String intPart = alphabet.clearZeros(finalParts[0]);
            if (intPart.isEmpty()) intPart = zeroString; // Para evitar que "0.5" se vuelva ".5"
            scaledRemainder = (finalParts.length > 1) ? intPart + "." + finalParts[1] : intPart;
        }

        processLog.append("\n==============================================\n");
        processLog.append("Cociente Final:  ").append(fullQuotient).append("\n");
        processLog.append("Residuo Final:   ").append(scaledRemainder).append("\n");

        String finalVisualProcess = processLog.toString();

        CalculationResult quotientObj = new CalculationResult(fullQuotient, finalVisualProcess);
        CalculationResult remainderObj = new CalculationResult(scaledRemainder, finalVisualProcess);

        return new CalculationResult[]{ quotientObj, remainderObj };
    }
}