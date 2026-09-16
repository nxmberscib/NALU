package me.jssandoval.nalu.math;

import me.jssandoval.nalu.base.Alphabet;
import me.jssandoval.nalu.logging.ProcessLogger;
import me.jssandoval.nalu.processor.CalculationResult;
import me.jssandoval.nalu.processor.DecimalAligner;

public class Adder extends PeanoOperator {

    private Subtracter subtracter;

    public Adder(Alphabet alphabet) {
        super(alphabet);
    }

    /** Necesario para resolver sumas de signos mixtos (a + (-b) = a - b). */
    public void setSubtracter(Subtracter subtracter) {
        this.subtracter = subtracter;
    }

    public CalculationResult add(String leftOperand, String rightOperand) {
        boolean negLeft = isNegative(leftOperand);
        boolean negRight = isNegative(rightOperand);
        String absLeft = abs(leftOperand);
        String absRight = abs(rightOperand);

        // Mismo signo: se suman las magnitudes y se conserva el signo
        if (negLeft == negRight) {
            CalculationResult result = addMagnitudes(absLeft, absRight);
            if (negLeft) {
                return new CalculationResult(applyNegativeSign(result.getResult()), result.getVisualProcess());
            }
            return result;
        }

        // Signos distintos: a + (-b) = a - b   |   (-a) + b = b - a
        if (subtracter == null) {
            throw new IllegalStateException("Adder necesita un Subtracter enlazado (setSubtracter) para sumar signos mixtos.");
        }
        return negLeft ? subtracter.subtract(absRight, absLeft) : subtracter.subtract(absLeft, absRight);
    }

    private CalculationResult addMagnitudes(String leftOperand, String rightOperand) {
        // --- 1. DECIMAL ALIGNMENT ---
        DecimalAligner aligner = new DecimalAligner(alphabet, leftOperand, rightOperand);
        int maximumFractionalLength = aligner.getMaximumFractionalLength();

        String alignedLeft = aligner.getLeftOperand();
        String alignedRight = aligner.getRightOperand();

        String cleanLeft = alphabet.clearZeros(alignedLeft);
        String cleanRight = alphabet.clearZeros(alignedRight);

        String zeroString = String.valueOf(alphabet.getZero());
        StringBuilder sumResultBuilder = new StringBuilder();
        StringBuilder carriesLogBuilder = new StringBuilder();

        String carryValue = zeroString;
        int leftOperandIndex = cleanLeft.length() - 1;
        int rightOperandIndex = cleanRight.length() - 1;

        // --- 2. INTEGER ADDITION VIA PEANO AXIOMS ---
        while (leftOperandIndex >= 0 || rightOperandIndex >= 0 || !carryValue.equals(zeroString)) {
            String leftDigit = (leftOperandIndex >= 0) ? String.valueOf(cleanLeft.charAt(leftOperandIndex)) : zeroString;
            String rightDigit = (rightOperandIndex >= 0) ? String.valueOf(cleanRight.charAt(rightOperandIndex)) : zeroString;

            String columnSumValue = zeroString;
            String peanoCounter = zeroString;

            while (!peanoCounter.equals(leftDigit)) { columnSumValue = increment(columnSumValue); peanoCounter = increment(peanoCounter); }
            peanoCounter = zeroString;
            while (!peanoCounter.equals(rightDigit)) { columnSumValue = increment(columnSumValue); peanoCounter = increment(peanoCounter); }
            peanoCounter = zeroString;
            while (!peanoCounter.equals(carryValue)) { columnSumValue = increment(columnSumValue); peanoCounter = increment(peanoCounter); }

            int alphabetBase = alphabet.getBase();
            int totalColumnDecimalValue = 0;
            String temporaryDecrementValue = columnSumValue;

            while (!temporaryDecrementValue.equals(zeroString)) {
                totalColumnDecimalValue++;
                temporaryDecrementValue = decrement(temporaryDecrementValue);
            }

            sumResultBuilder.append(alphabet.getSymbol(totalColumnDecimalValue % alphabetBase));

            // Registro de acarreos visuales
            if (carryValue.equals(zeroString)) {
                carriesLogBuilder.append(" ");
            } else {
                carriesLogBuilder.append(carryValue);
            }

            carryValue = zeroString;
            for (int carryIncrementIndex = 0; carryIncrementIndex < (totalColumnDecimalValue / alphabetBase); carryIncrementIndex++) {
                carryValue = increment(carryValue);
            }

            leftOperandIndex--;
            rightOperandIndex--;
        }

        String finalSumString = sumResultBuilder.reverse().toString();
        String carriesString = carriesLogBuilder.reverse().toString();

        // --- 3. DECIMAL POINT REINSERTION ---
        if (maximumFractionalLength > 0) {
            char zeroSymbol = alphabet.getZero();
            while (finalSumString.length() <= maximumFractionalLength) {
                finalSumString = zeroSymbol + finalSumString;
            }
            int decimalPointInsertionIndex = finalSumString.length() - maximumFractionalLength;
            finalSumString = finalSumString.substring(0, decimalPointInsertionIndex) + "." + finalSumString.substring(decimalPointInsertionIndex);

            alignedLeft = alignedLeft.substring(0, alignedLeft.length() - maximumFractionalLength) + "." + alignedLeft.substring(alignedLeft.length() - maximumFractionalLength);
            alignedRight = alignedRight.substring(0, alignedRight.length() - maximumFractionalLength) + "." + alignedRight.substring(alignedRight.length() - maximumFractionalLength);

            if (carriesString.length() > maximumFractionalLength) {
                carriesString = carriesString.substring(0, carriesString.length() - maximumFractionalLength) + " " + carriesString.substring(carriesString.length() - maximumFractionalLength);
            }

            while (finalSumString.endsWith(String.valueOf(zeroSymbol)) && finalSumString.contains(".")) { finalSumString = finalSumString.substring(0, finalSumString.length() - 1); }
            if (finalSumString.endsWith(".")) { finalSumString = finalSumString.substring(0, finalSumString.length() - 1); }
        } else {
            finalSumString = alphabet.clearZeros(finalSumString);
        }

        // --- 4. ASSEMBLE VISUAL PROCESS ---
        int maxLength = Math.max(Math.max(alignedLeft.length(), alignedRight.length()), finalSumString.length()) + 2;

        StringBuilder graphicLog = new StringBuilder();
        graphicLog.append(ProcessLogger.padLeft(carriesString, maxLength)).append("  (Acarreos)\n");
        graphicLog.append(ProcessLogger.padLeft(alignedLeft, maxLength)).append("\n");
        graphicLog.append("+").append(ProcessLogger.padLeft(alignedRight, maxLength - 1)).append("\n");
        graphicLog.append(ProcessLogger.padLeft("-".repeat(maxLength), maxLength)).append("\n");
        graphicLog.append(ProcessLogger.padLeft(finalSumString, maxLength)).append("\n");

        // Devolvemos el objeto contenedor con ambas cosas
        return new CalculationResult(finalSumString, graphicLog.toString());
    }
}