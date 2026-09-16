package me.jssandoval.nalu.base;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseConverter {

    public static class FullVerificationResult {
        private final String finalResultBase10;
        private final String visualLog;

        public FullVerificationResult(String finalResultBase10, String visualLog) {
            this.finalResultBase10 = finalResultBase10;
            this.visualLog = visualLog;
        }

        public String getFinalResultBase10() { return finalResultBase10; }
        public String getVisualLog() { return visualLog; }
    }

    public static class ConversionResult {
        private final String resultInBaseB;
        private final String visualLog;

        public ConversionResult(String resultInBaseB, String visualLog) {
            this.resultInBaseB = resultInBaseB;
            this.visualLog = visualLog;
        }

        public String getResultInBaseB() { return resultInBaseB; }
        public String getVisualLog() { return visualLog; }
    }

    /**
     * Verifica la ecuación completa:
     * 1. Convierte cada variable de Base N a Base 10 mostrando la tabla.
     * 2. Sustituye las variables en la ecuación original.
     * 3. Ejecuta la matemática paso a paso en Base 10.
     */
    public static FullVerificationResult verifyFullEquation(String originalEquation, Map<String, String> variablesMemory, Alphabet alphabetN) {
        StringBuilder log = new StringBuilder();
        log.append("=========================================================================\n");
        log.append("          VERIFICACIÓN ALGEBRAICA GLOBAL (MATEMÁTICA EN BASE 10)\n");
        log.append("=========================================================================\n\n");

        log.append("--- 1. CONVERSIÓN DE VARIABLES (BASE ").append(alphabetN.getBase()).append(" -> BASE 10) ---\n\n");

        Map<String, String> base10Memory = new HashMap<>();

        // Traducimos cada variable individualmente
        for (Map.Entry<String, String> entry : variablesMemory.entrySet()) {
            String varName = entry.getKey();
            String varBaseNValue = entry.getValue();

            log.append(String.format("Variable '%s' = %s\n", varName, varBaseNValue));
            String base10Val = verifyVariablePositional(varBaseNValue, alphabetN, log);
            base10Memory.put(varName, base10Val);
            log.append("\n");
        }

        log.append("--- 2. RESOLUCIÓN DE LA ECUACIÓN EN BASE 10 ---\n\n");
        log.append("Ecuación Original (Base N): ").append(originalEquation).append("\n");

        String currentEquation = originalEquation.replaceAll("\\s+", "");

        // Sustituir de mayor a menor longitud para evitar errores (ej. pisar 'A' dentro de 'AB')
        List<String> sortedVars = new ArrayList<>(base10Memory.keySet());
        sortedVars.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String varName : sortedVars) {
            currentEquation = currentEquation.replace(varName, base10Memory.get(varName));
        }

        log.append("Sustitución en Base 10:     ").append(currentEquation).append("\n\n");
        log.append("Desarrollo de las Operaciones:\n");
        log.append("-".repeat(78)).append("\n");

        // Expresiones regulares para evaluar matemáticas en notación decimal estándar
        Pattern blockPattern = Pattern.compile("([\\(\\[\\{])([^\\(\\[\\{\\)\\]\\}]+)([\\)\\]\\}])");
        Pattern prio1Pattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)([\\*/%])(-?\\d+(?:\\.\\d+)?)");
        Pattern prio2Pattern = Pattern.compile("(-?\\d+(?:\\.\\d+)?)([\\+\\-])(-?\\d+(?:\\.\\d+)?)");

        int step = 1;
        while (true) {
            Matcher blockMatcher = blockPattern.matcher(currentEquation);

            if (blockMatcher.find()) {
                String openP = blockMatcher.group(1);
                String content = blockMatcher.group(2);
                String closeP = blockMatcher.group(3);

                String newContent = tryOperateBase10(content, prio1Pattern, log, step);
                if (newContent != null) step++;
                else {
                    newContent = tryOperateBase10(content, prio2Pattern, log, step);
                    if (newContent != null) step++;
                }

                if (newContent != null) {
                    currentEquation = currentEquation.substring(0, blockMatcher.start()) + openP + newContent + closeP + currentEquation.substring(blockMatcher.end());
                    log.append("   -> Estado: ").append(currentEquation).append("\n\n");
                } else {
                    currentEquation = currentEquation.substring(0, blockMatcher.start()) + content + currentEquation.substring(blockMatcher.end());
                }
            } else {
                String newEq = tryOperateBase10(currentEquation, prio1Pattern, log, step);
                if (newEq != null) step++;
                else {
                    newEq = tryOperateBase10(currentEquation, prio2Pattern, log, step);
                    if (newEq != null) step++;
                }

                if (newEq == null) break;
                currentEquation = newEq;
                log.append("   -> Estado: ").append(currentEquation).append("\n\n");
            }
        }

        currentEquation = currentEquation.replaceAll("[\\(\\[\\{\\)\\]\\}]", "");

        log.append("-".repeat(78)).append("\n");
        log.append(String.format("%-55s %s\n", "RESULTADO FINAL VERIFICADO EN BASE 10:", currentEquation));
        log.append("=========================================================================\n");

        return new FullVerificationResult(currentEquation, log.toString());
    }

    private static String tryOperateBase10(String text, Pattern pattern, StringBuilder log, int stepNum) {
        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return null;

        BigDecimal left = new BigDecimal(matcher.group(1));
        String op = matcher.group(2);
        BigDecimal right = new BigDecimal(matcher.group(3));
        BigDecimal result;

        switch (op) {
            case "+": result = left.add(right); break;
            case "-": result = left.subtract(right); break;
            case "*": result = left.multiply(right); break;
            case "/":
                if (right.compareTo(BigDecimal.ZERO) == 0) throw new ArithmeticException("División por cero en Base 10");
                result = left.divide(right, 10, RoundingMode.HALF_UP).stripTrailingZeros();
                break;
            case "%": result = left.remainder(right); break;
            default: return null;
        }

        String resStr = result.toPlainString();
        if (resStr.contains(".")) resStr = resStr.replaceAll("0*$", "").replaceAll("\\.$", "");
        if (resStr.isEmpty()) resStr = "0";

        log.append("Paso ").append(stepNum).append(": Resolver [ ").append(matcher.group(0)).append(" ] = ").append(resStr).append("\n");

        return text.substring(0, matcher.start()) + resStr + text.substring(matcher.end());
    }

    public static String verifyVariablePositional(String numberInBaseN, Alphabet alphabetN, StringBuilder log) {
        int baseN = alphabetN.getBase();
        String cleanNum = numberInBaseN.trim();
        if (cleanNum.isEmpty()) cleanNum = String.valueOf(alphabetN.getZero());

        boolean isNegative = cleanNum.startsWith("-") && !cleanNum.equals("-" + alphabetN.getZero());
        if (cleanNum.startsWith("-")) cleanNum = cleanNum.substring(1);

        String[] parts = cleanNum.split("\\.");
        String intPart = alphabetN.clearZeros(parts[0]);
        if (intPart.isEmpty()) intPart = String.valueOf(alphabetN.getZero());
        String fracPart = (parts.length > 1) ? parts[1] : "";

        BigInteger integerBase10 = BigInteger.ZERO;
        BigDecimal fractionalBase10 = BigDecimal.ZERO;

        log.append(String.format("%-10s | %-9s | %-7s | %-22s | %-15s\n", "Posición", "Símbolo", "Valor", "Potencia (Base^Pos)", "Subtotal"));
        log.append("-".repeat(78)).append("\n");

        int lenInt = intPart.length();
        BigInteger baseNBig = BigInteger.valueOf(baseN);

        for (int i = lenInt - 1; i >= 0; i--) {
            int posExp = lenInt - 1 - i;
            char symbol = intPart.charAt(i);
            int digitValue = alphabetN.getIndex(symbol);

            BigInteger powerWeight = baseNBig.pow(posExp);
            BigInteger subtotal = BigInteger.valueOf(digitValue).multiply(powerWeight);
            integerBase10 = integerBase10.add(subtotal);

            log.append(String.format("D%-9d | '%-7c' | %-7d | %d^%d = %-15s | %s\n", posExp, symbol, digitValue, baseN, posExp, powerWeight, subtotal));
        }

        if (!fracPart.isEmpty()) {
            log.append("-".repeat(78)).append("\n");
            BigDecimal baseNDec = new BigDecimal(baseN);
            BigDecimal currentWeight = BigDecimal.ONE.divide(baseNDec, 15, RoundingMode.HALF_UP);

            for (int j = 0; j < fracPart.length(); j++) {
                int posExp = -(j + 1);
                char symbol = fracPart.charAt(j);
                int digitValue = alphabetN.getIndex(symbol);

                BigDecimal subtotalFrac = BigDecimal.valueOf(digitValue).multiply(currentWeight);
                fractionalBase10 = fractionalBase10.add(subtotalFrac);

                log.append(String.format("D%-9d | '%-7c' | %-7d | %d^(%d)%-13s | %s\n", posExp, symbol, digitValue, baseN, posExp, "", subtotalFrac.stripTrailingZeros().toPlainString()));
                currentWeight = currentWeight.divide(baseNDec, 15, RoundingMode.HALF_UP);
            }
        }

        BigDecimal totalBase10 = new BigDecimal(integerBase10).add(fractionalBase10);
        String base10Str = totalBase10.stripTrailingZeros().toPlainString();
        if (base10Str.contains(".")) base10Str = base10Str.replaceAll("0*$", "").replaceAll("\\.$", "");
        if (base10Str.isEmpty()) base10Str = "0";
        if (isNegative && !base10Str.equals("0")) base10Str = "-" + base10Str;

        log.append("-".repeat(78)).append("\n");
        log.append(String.format("=> Equivalencia Decimal: %s\n", base10Str));

        return base10Str;
    }

    public static ConversionResult convertToBaseB(String base10Str, Alphabet targetAlphabet) {
        boolean isNegative = base10Str.trim().startsWith("-");
        String unsignedBase10Str = isNegative ? base10Str.trim().substring(1) : base10Str.trim();

        BigDecimal totalDec = new BigDecimal(unsignedBase10Str);
        BigInteger tempB = totalDec.toBigInteger();
        int baseB = targetAlphabet.getBase();
        StringBuilder resultBuilder = new StringBuilder();

        StringBuilder log = new StringBuilder();
        log.append("=========================================================================\n");
        log.append(String.format("                     CONVERSIÓN (BASE 10 A BASE %d)\n", baseB));
        log.append("=========================================================================\n\n");

        if (tempB.equals(BigInteger.ZERO)) {
            char zeroSymbol = targetAlphabet.getZero();
            resultBuilder.append(zeroSymbol);
            log.append(String.format("  Paso 1: Valor 0 -> Símbolo: '%c'\n", zeroSymbol));
        } else {
            log.append(String.format("%-6s | %-50s | %-10s | %s\n", "Paso", "Ecuación (Dividendo = Cociente * Base + Residuo)", "Símbolo", "Acumulado"));
            log.append("-".repeat(92)).append("\n");

            BigInteger baseBBig = BigInteger.valueOf(baseB);
            int paso = 1;

            while (tempB.compareTo(BigInteger.ZERO) > 0) {
                BigInteger[] divRem = tempB.divideAndRemainder(baseBBig);
                BigInteger cociente = divRem[0];
                int residuo = divRem[1].intValue();

                char caracter = targetAlphabet.getSymbol(residuo);
                resultBuilder.insert(0, caracter);

                String ecuacion = String.format("%s = %s * %d + %d", tempB.toString(), cociente.toString(), baseB, residuo);
                String infoSimbolo = String.format("%d -> '%c'", residuo, caracter);

                log.append(String.format("%-6d | %-50s | %-10s | %s\n", paso, ecuacion, infoSimbolo, resultBuilder.toString()));

                tempB = cociente;
                paso++;
            }
        }

        BigDecimal fracDec = totalDec.subtract(new BigDecimal(totalDec.toBigInteger()));

        if (fracDec.compareTo(BigDecimal.ZERO) > 0) {
            log.append("\n").append("-".repeat(92)).append("\n");
            log.append("Conversión de la Parte Fraccionaria (Multiplicaciones Sucesivas):\n");
            log.append(String.format("%-6s | %-50s | %-10s | %s\n", "Paso", "Ecuación (Fracción * Base = Entero + NuevaFrac)", "Símbolo", "Acumulado Frac"));
            log.append("-".repeat(92)).append("\n");

            StringBuilder fracResult = new StringBuilder();
            BigDecimal currentFrac = fracDec;
            BigDecimal baseBDec = new BigDecimal(baseB);
            int maxPrecision = 10;
            int pasoFrac = 1;

            while (currentFrac.compareTo(BigDecimal.ZERO) > 0 && pasoFrac <= maxPrecision) {
                BigDecimal prod = currentFrac.multiply(baseBDec);
                int intPart = prod.intValue();
                BigDecimal newFrac = prod.subtract(new BigDecimal(intPart));

                char symbol = targetAlphabet.getSymbol(intPart);
                fracResult.append(symbol);

                String eqStr = String.format("%s * %d = %d + %s",
                        currentFrac.stripTrailingZeros().toPlainString(), baseB, intPart, newFrac.stripTrailingZeros().toPlainString());
                String infoSim = String.format("%d -> '%c'", intPart, symbol);

                log.append(String.format("%-6d | %-50s | %-10s | .%s\n", pasoFrac, eqStr, infoSim, fracResult.toString()));

                currentFrac = newFrac;
                pasoFrac++;
            }
            if (fracResult.length() > 0) resultBuilder.append(".").append(fracResult);
        }

        String finalResultB = resultBuilder.toString();
        if (isNegative && !finalResultB.equals(String.valueOf(targetAlphabet.getZero()))) {
            finalResultB = "-" + finalResultB;
        }
        log.append("\n=========================================================================\n");
        log.append(String.format("RESULTADO FINAL EN BASE %d: %s\n", baseB, finalResultB));
        log.append("=========================================================================\n");

        return new ConversionResult(finalResultB, log.toString());
    }
}