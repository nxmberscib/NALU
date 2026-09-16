package me.jssandoval.nalu.processor;

import me.jssandoval.nalu.base.Alphabet;

public class DecimalAligner {
    private String leftOperand, rightOperand;
    private Alphabet alphabet;
    private int maximumFractionalLength = 0;

    public int getMaximumFractionalLength() {
        return maximumFractionalLength;
    }

    public String getLeftOperand() {
        return leftOperand;
    }

    public String getRightOperand() {
        return rightOperand;
    }

    public DecimalAligner(Alphabet alphabet, String leftOperand, String rightOperand ) {
        this.alphabet = alphabet;
        this.leftOperand = leftOperand;
        this.rightOperand = rightOperand;

        this.align();
    }

    private void align() {
        if (leftOperand.contains(".") || rightOperand.contains(".")) {
            String[] leftOperandParts = leftOperand.contains(".") ? leftOperand.split("\\.") : new String[]{leftOperand, ""};
            String[] rightOperandParts = rightOperand.contains(".") ? rightOperand.split("\\.") : new String[]{rightOperand, ""};

            StringBuilder leftFractionalPart = new StringBuilder(leftOperandParts.length > 1 ? leftOperandParts[1] : "");
            StringBuilder rightFractionalPart = new StringBuilder(rightOperandParts.length > 1 ? rightOperandParts[1] : "");

            maximumFractionalLength = Math.max(leftFractionalPart.length(), rightFractionalPart.length());
            char zeroSymbol = alphabet.getZero();

            // Pad with zeros to the right to equalize lengths
            while (leftFractionalPart.length() < maximumFractionalLength) {
                leftFractionalPart.append(zeroSymbol);
            }
            while (rightFractionalPart.length() < maximumFractionalLength) {
                rightFractionalPart.append(zeroSymbol);
            }

            leftOperand = leftOperandParts[0] + leftFractionalPart;
            rightOperand = rightOperandParts[0] + rightFractionalPart;
        }

    }
}
