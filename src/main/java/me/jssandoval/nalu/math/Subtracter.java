package me.jssandoval.nalu.math;

import me.jssandoval.nalu.base.Alphabet;
import me.jssandoval.nalu.logging.ProcessLogger;
import me.jssandoval.nalu.processor.CalculationResult;
import me.jssandoval.nalu.processor.DecimalAligner;

public class Subtracter extends PeanoOperator {

    private Adder adder;

    public Subtracter(Alphabet alphabet) {
        super(alphabet);
    }

    /** Necesario para resolver restas de signos mixtos (-a - b = -(a+b), a - (-b) = a+b). */
    public void setAdder(Adder adder) {
        this.adder = adder;
    }

    public CalculationResult subtract(String leftOperand, String rightOperand) {
        boolean negLeft = isNegative(leftOperand);
        boolean negRight = isNegative(rightOperand);
        String absLeft = abs(leftOperand);
        String absRight = abs(rightOperand);

        if (!negLeft && !negRight) {
            // a - b: subtractMagnitudes ya decide el signo si a < b
            return subtractMagnitudes(absLeft, absRight);
        }

        if (adder == null) {
            throw new IllegalStateException("Subtracter necesita un Adder enlazado (setAdder) para restar signos mixtos.");
        }

        if (negLeft && !negRight) {
            // -a - b = -(a + b)
            CalculationResult result = adder.add(absLeft, absRight);
            return new CalculationResult(applyNegativeSign(result.getResult()), result.getVisualProcess());
        }

        if (!negLeft && negRight) {
            // a - (-b) = a + b
            return adder.add(absLeft, absRight);
        }

        // -a - (-b) = b - a  (ambos operandos ya son magnitudes positivas)
        return subtractMagnitudes(absRight, absLeft);
    }

    private CalculationResult subtractMagnitudes(String leftOperand, String rightOperand) {
        // --- 1. DECIMAL ALIGNMENT ---
        DecimalAligner aligner = new DecimalAligner(alphabet, leftOperand, rightOperand);
        int maximumFractionalLength = aligner.getMaximumFractionalLength();

        String alignedLeft = aligner.getLeftOperand();
        String alignedRight = aligner.getRightOperand();

        String cleanLeft = alphabet.clearZeros(alignedLeft);
        String cleanRight = alphabet.clearZeros(alignedRight);

        // Si a < b, el resultado es negativo: resolvemos b - a y volteamos el signo
        if (compare(cleanLeft, cleanRight) < 0) {
            CalculationResult inverted = subtractMagnitudes(rightOperand, leftOperand);
            return new CalculationResult(applyNegativeSign(inverted.getResult()), inverted.getVisualProcess());
        }

        StringBuilder subtractionResultBuilder = new StringBuilder();
        StringBuilder borrowsLogBuilder = new StringBuilder(); // Para el registro visual

        int borrowValue = 0;
        int alphabetBase = alphabet.getBase();
        int leftOperandIndex = cleanLeft.length() - 1;
        int rightOperandIndex = cleanRight.length() - 1;

        // --- 2. INTEGER SUBTRACTION ---
        while (leftOperandIndex >= 0) {
            int leftDigitValue = alphabet.getIndex(cleanLeft.charAt(leftOperandIndex));
            int rightDigitValue = (rightOperandIndex >= 0) ? alphabet.getIndex(cleanRight.charAt(rightOperandIndex)) : 0;

            int columnDifferenceValue = leftDigitValue - rightDigitValue - borrowValue;

            if (columnDifferenceValue < 0) {
                columnDifferenceValue += alphabetBase;
                borrowValue = 1;
                borrowsLogBuilder.append("1"); // Registramos que hubo préstamo
            } else {
                borrowValue = 0;
                borrowsLogBuilder.append(" "); // Espacio en blanco si no hubo préstamo
            }

            subtractionResultBuilder.append(alphabet.getSymbol(columnDifferenceValue));

            leftOperandIndex--;
            rightOperandIndex--;
        }

        String finalSubtractionString = subtractionResultBuilder.reverse().toString();
        String borrowsString = borrowsLogBuilder.reverse().toString();

        // --- 3. DECIMAL POINT REINSERTION ---
        if (maximumFractionalLength > 0) {
            char zeroSymbol = alphabet.getZero();
            while (finalSubtractionString.length() <= maximumFractionalLength) {
                finalSubtractionString = zeroSymbol + finalSubtractionString;
            }
            int decimalPointInsertionIndex = finalSubtractionString.length() - maximumFractionalLength;
            finalSubtractionString = finalSubtractionString.substring(0, decimalPointInsertionIndex) + "." + finalSubtractionString.substring(decimalPointInsertionIndex);

            alignedLeft = alignedLeft.substring(0, alignedLeft.length() - maximumFractionalLength) + "." + alignedLeft.substring(alignedLeft.length() - maximumFractionalLength);
            alignedRight = alignedRight.substring(0, alignedRight.length() - maximumFractionalLength) + "." + alignedRight.substring(alignedRight.length() - maximumFractionalLength);

            if (borrowsString.length() > maximumFractionalLength) {
                borrowsString = borrowsString.substring(0, borrowsString.length() - maximumFractionalLength) + " " + borrowsString.substring(borrowsString.length() - maximumFractionalLength);
            }

            while (finalSubtractionString.endsWith(String.valueOf(zeroSymbol)) && finalSubtractionString.contains(".")) {
                finalSubtractionString = finalSubtractionString.substring(0, finalSubtractionString.length() - 1);
            }
            if (finalSubtractionString.endsWith(".")) {
                finalSubtractionString = finalSubtractionString.substring(0, finalSubtractionString.length() - 1);
            }

            String[] finalParts = finalSubtractionString.split("\\.");
            String integerPart = alphabet.clearZeros(finalParts[0]);
            finalSubtractionString = (finalParts.length > 1) ? integerPart + "." + finalParts[1] : integerPart;
        } else {
            finalSubtractionString = alphabet.clearZeros(finalSubtractionString);
        }

        // --- 4. ASSEMBLE VISUAL PROCESS ---
        int maxLength = Math.max(Math.max(alignedLeft.length(), alignedRight.length()), finalSubtractionString.length()) + 2;

        StringBuilder graphicLog = new StringBuilder();
        graphicLog.append(ProcessLogger.padLeft(borrowsString, maxLength)).append("  (Prestamos)\n");
        graphicLog.append(ProcessLogger.padLeft(alignedLeft, maxLength)).append("\n");
        graphicLog.append("-").append(ProcessLogger.padLeft(alignedRight, maxLength - 1)).append("\n");
        graphicLog.append(ProcessLogger.padLeft("-".repeat(maxLength), maxLength)).append("\n");
        graphicLog.append(ProcessLogger.padLeft(finalSubtractionString, maxLength)).append("\n");

        return new CalculationResult(finalSubtractionString, graphicLog.toString());
    }
}