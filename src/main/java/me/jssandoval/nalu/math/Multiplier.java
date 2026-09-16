package me.jssandoval.nalu.math;

import java.util.ArrayList;
import java.util.List;
import me.jssandoval.nalu.base.Alphabet;
import me.jssandoval.nalu.logging.ProcessLogger;
import me.jssandoval.nalu.processor.CalculationResult;

public class Multiplier extends PeanoOperator {

    private final Adder adder;

    public Multiplier(Alphabet alphabet) {
        super(alphabet);
        this.adder = new Adder(alphabet);
    }

    public CalculationResult multiply(String leftOperand, String rightOperand) {
        boolean negativeResult = isNegative(leftOperand) != isNegative(rightOperand);
        CalculationResult result = multiplyMagnitudes(abs(leftOperand), abs(rightOperand));

        if (negativeResult) {
            return new CalculationResult(applyNegativeSign(result.getResult()), result.getVisualProcess());
        }
        return result;
    }

    private CalculationResult multiplyMagnitudes(String leftOperand, String rightOperand) {
        // Extraer decimal
        int totalFractionalPositions = 0;
        String originalLeft = leftOperand;
        String originalRight = rightOperand;

        if (leftOperand.contains(".")) {
            totalFractionalPositions += leftOperand.length() - leftOperand.indexOf(".") - 1;
            leftOperand = leftOperand.replace(".", "");
        }
        if (rightOperand.contains(".")) {
            totalFractionalPositions += rightOperand.length() - rightOperand.indexOf(".") - 1;
            rightOperand = rightOperand.replace(".", "");
        }

        leftOperand = alphabet.clearZeros(leftOperand);
        rightOperand = alphabet.clearZeros(rightOperand);
        String zeroString = String.valueOf(alphabet.getZero());

        if (leftOperand.equals(zeroString) || rightOperand.equals(zeroString)) {
            String log = "  " + originalLeft + "\n* " + originalRight + "\n------\n  " + zeroString;
            return new CalculationResult(zeroString, log);
        }

        String finalMultiplicationResult = zeroString;
        int alphabetBase = alphabet.getBase();
        List<String> partialProductsLog = new ArrayList<>(); // Para guardar los pasos visuales

        // Ciclo de multipliación con peano
        for (int rightIndex = rightOperand.length() - 1; rightIndex >= 0; rightIndex--) {
            int rightDigitValue = alphabet.getIndex(rightOperand.charAt(rightIndex));
            StringBuilder partialProductBuilder = new StringBuilder();

            int shiftCount = rightOperand.length() - 1 - rightIndex;
            for (int k = 0; k < shiftCount; k++) {
                partialProductBuilder.append(zeroString);
            }

            String carryValue = zeroString;

            for (int leftIndex = leftOperand.length() - 1; leftIndex >= 0; leftIndex--) {
                int leftDigitValue = alphabet.getIndex(leftOperand.charAt(leftIndex));
                String columnProductValue = zeroString;
                String peanoCounter = zeroString;

                int totalSteps = leftDigitValue * rightDigitValue;
                for (int step = 0; step < totalSteps; step++) {
                    columnProductValue = increment(columnProductValue);
                }

                while (!peanoCounter.equals(carryValue)) {
                    columnProductValue = increment(columnProductValue);
                    peanoCounter = increment(peanoCounter);
                }

                int totalColumnDecimalValue = 0;
                String temporaryDecrementValue = columnProductValue;
                while (!temporaryDecrementValue.equals(zeroString)) {
                    totalColumnDecimalValue++;
                    temporaryDecrementValue = decrement(temporaryDecrementValue);
                }

                partialProductBuilder.append(alphabet.getSymbol(totalColumnDecimalValue % alphabetBase));
                carryValue = zeroString;
                for (int carryIncrementIndex = 0; carryIncrementIndex < (totalColumnDecimalValue / alphabetBase); carryIncrementIndex++) {
                    carryValue = increment(carryValue);
                }
            }

            if (!carryValue.equals(zeroString)) {
                partialProductBuilder.append(carryValue);
            }

            String reversedPartialProduct = partialProductBuilder.reverse().toString();
            partialProductsLog.add(reversedPartialProduct); // Para el dibujo

            // Usar Adder interno y sacar solo el resultado numérico
            finalMultiplicationResult = adder.add(finalMultiplicationResult, reversedPartialProduct).getResult();
        }

        // Insertar punto decimal
        if (totalFractionalPositions > 0) {
            while (finalMultiplicationResult.length() <= totalFractionalPositions) {
                finalMultiplicationResult = zeroString + finalMultiplicationResult;
            }

            int decIndex = finalMultiplicationResult.length() - totalFractionalPositions;
            finalMultiplicationResult = finalMultiplicationResult.substring(0, decIndex) + "." + finalMultiplicationResult.substring(decIndex);

            while (finalMultiplicationResult.endsWith(zeroString) && finalMultiplicationResult.contains(".")) {
                finalMultiplicationResult = finalMultiplicationResult.substring(0, finalMultiplicationResult.length() - 1);
            }
            if (finalMultiplicationResult.endsWith(".")) {
                finalMultiplicationResult = finalMultiplicationResult.substring(0, finalMultiplicationResult.length() - 1);
            }

            String[] finalParts = finalMultiplicationResult.split("\\.");
            String integerPart = alphabet.clearZeros(finalParts[0]);
            finalMultiplicationResult = (finalParts.length > 1) ? integerPart + "." + finalParts[1] : integerPart;
        } else {
            finalMultiplicationResult = alphabet.clearZeros(finalMultiplicationResult);
        }

        // Ensamblar proceso visual
        int maxLength = Math.max(Math.max(originalLeft.length(), originalRight.length()), finalMultiplicationResult.length()) + 3;

        StringBuilder graphicLog = new StringBuilder();
        graphicLog.append(ProcessLogger.padLeft(originalLeft, maxLength)).append("\n");
        graphicLog.append("*").append(ProcessLogger.padLeft(originalRight, maxLength - 1)).append("\n");
        graphicLog.append(ProcessLogger.padLeft("-".repeat(maxLength), maxLength)).append("\n");

        // Dibujar todos los productos parciales sumándose
        for (int i = 0; i < partialProductsLog.size(); i++) {
            String prefix = (i == partialProductsLog.size() - 1 && partialProductsLog.size() > 1) ? "+" : " ";
            graphicLog.append(prefix).append(ProcessLogger.padLeft(partialProductsLog.get(i), maxLength - 1)).append("\n");
        }

        if (partialProductsLog.size() > 1) {
            graphicLog.append(ProcessLogger.padLeft("-".repeat(maxLength), maxLength)).append("\n");
        }
        graphicLog.append(ProcessLogger.padLeft(finalMultiplicationResult, maxLength)).append("\n");

        return new CalculationResult(finalMultiplicationResult, graphicLog.toString());
    }
}