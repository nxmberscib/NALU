package me.jssandoval.nalu.processor;

import me.jssandoval.nalu.base.Alphabet;
import me.jssandoval.nalu.logging.ProcessLogger;
import me.jssandoval.nalu.math.*;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Parser {

    private final Alphabet alphabet;
    private final ProcessLogger logger;

    private final Adder adder;
    private final Subtracter subtracter;
    private final Multiplier multiplier;
    private final Divider divider;

    public Parser(Alphabet alphabet, ProcessLogger logger) {
        this.alphabet = alphabet;
        this.logger = logger;

        this.adder = new Adder(alphabet);
        this.subtracter = new Subtracter(alphabet);
        this.adder.setSubtracter(this.subtracter);
        this.subtracter.setAdder(this.adder);
        this.multiplier = new Multiplier(alphabet);
        this.divider = new Divider(alphabet);
    }

    public String resolve(String originalEquation, Map<String, String> variablesMemory) {
        // 1. Limpiar espacios. ¡NO reemplazamos las variables aquí para conservar sus nombres!
        String currentEquation = originalEquation.replaceAll("\\s+", "");

        // 2. Patrones actualizados (Agregué guión bajo por si usas nombres como var_A)
        Pattern blockPattern = Pattern.compile("([\\(\\[\\{])([^\\(\\[\\{\\)\\]\\}]+)([\\)\\]\\}])");
        Pattern prio1Pattern = Pattern.compile("(-?[A-Za-z0-9Ññ_\\.]+)([\\*/%])(-?[A-Za-z0-9Ññ_\\.]+)");
        Pattern prio2Pattern = Pattern.compile("(-?[A-Za-z0-9Ññ_\\.]+)([\\+\\-])(-?[A-Za-z0-9Ññ_\\.]+)");

        while (true) {
            Matcher blockMatcher = blockPattern.matcher(currentEquation);

            if (blockMatcher.find()) {
                String openP = blockMatcher.group(1);
                String content = blockMatcher.group(2);
                String closeP = blockMatcher.group(3);

                String newContent = tryOperate(content, prio1Pattern, currentEquation, variablesMemory);
                if (newContent == null) {
                    newContent = tryOperate(content, prio2Pattern, currentEquation, variablesMemory);
                }

                if (newContent != null) {
                    currentEquation = currentEquation.substring(0, blockMatcher.start()) + openP + newContent + closeP + currentEquation.substring(blockMatcher.end());
                } else {
                    currentEquation = currentEquation.substring(0, blockMatcher.start()) + content + currentEquation.substring(blockMatcher.end());
                }

            } else {
                String newEq = tryOperate(currentEquation, prio1Pattern, currentEquation, variablesMemory);
                if (newEq == null) {
                    newEq = tryOperate(currentEquation, prio2Pattern, currentEquation, variablesMemory);
                }

                if (newEq == null) {
                    break;
                }
                currentEquation = newEq;
            }
        }

        // Red de seguridad: si la ecuación era solo "(A)" o "-A" sin operaciones, devuelve su valor real
        return resolveOperand(currentEquation, variablesMemory);
    }

    private String tryOperate(String textToProcess, Pattern pattern, String fullEquation, Map<String, String> variablesMemory) {
        Matcher matcher = pattern.matcher(textToProcess);
        if (!matcher.find()) {
            return null;
        }

        // Aquí capturamos los nombres (ej: "A" y "B") o los resultados previos (ej: "101.1")
        String leftName = matcher.group(1);
        String operator = matcher.group(2);
        String rightName = matcher.group(3);

        // ¡MAGIA!: Buscamos el valor numérico dinámicamente.
        // Si no existe en memoria (porque ya es un número temporal), se queda igual.
        // Si el nombre trae un '-' pegado y el resto SÍ es una variable conocida (ej. "-A"),
        // resolvemos el valor de esa variable y le invertimos el signo.
        String leftValue = resolveOperand(leftName, variablesMemory);
        String rightValue = resolveOperand(rightName, variablesMemory);

        // Armamos el título del paso con los nombres originales para la Interfaz Gráfica
        String subEquation = leftName + " " + operator + " " + rightName;

        CalculationResult calculationResult;
        String operationName;

        // Operamos matemáticamente enviando los valores DECIMALES PUROS, protegiéndolos del Regex
        switch (operator) {
            case "+":
                calculationResult = adder.add(leftValue, rightValue);
                operationName = "Suma";
                break;
            case "-":
                calculationResult = subtracter.subtract(leftValue, rightValue);
                operationName = "Resta";
                break;
            case "*":
                calculationResult = multiplier.multiply(leftValue, rightValue);
                operationName = "Multiplicacion";
                break;
            case "/":
                calculationResult = divider.divide(leftValue, rightValue)[0];
                operationName = "Division (Cociente)";
                break;
            case "%":
                calculationResult = divider.divide(leftValue, rightValue)[1];
                operationName = "Division (Modulo)";
                break;
            default:
                throw new IllegalArgumentException("Operador desconocido: " + operator);
        }

        String resultNumericValue = calculationResult.getResult();

        // Sustituimos el pedazo resuelto en la ecuación por el número
        String processedText = textToProcess.substring(0, matcher.start()) + resultNumericValue + textToProcess.substring(matcher.end());
        String nextFullEquation = fullEquation.replace(textToProcess, processedText);

        // El logger ahora registrará "A + B" en lugar de sus valores crudos
        logger.registerStep(
                subEquation,
                operationName + " (Base " + alphabet.getBase() + ")",
                calculationResult.getVisualProcess(),
                resultNumericValue,
                nextFullEquation
        );

        return processedText;
    }

    /**
     * Resuelve un operando capturado por el regex a su valor numérico:
     * - Si es el nombre de una variable conocida (ej. "A"), retorna su valor.
     * - Si es "-" + nombre de una variable conocida (ej. "-A"), retorna el valor de esa
     *   variable con el signo invertido (esto es lo que faltaba: sin esto, "-A" se quedaba
     *   como texto literal y terminaba pasándose "A" al motor aritmético, que no reconoce
     *   letras como dígitos).
     * - Si no es ninguna variable (ya es un número resuelto, con o sin signo), se devuelve tal cual.
     */
    private String resolveOperand(String name, Map<String, String> variablesMemory) {
        if (name.startsWith("-")) {
            String possibleVarName = name.substring(1);
            if (variablesMemory.containsKey(possibleVarName)) {
                return negateValue(variablesMemory.get(possibleVarName));
            }
        }
        return variablesMemory.getOrDefault(name, name);
    }

    /** Invierte el signo de un valor numérico (sin usar "-0"). */
    private String negateValue(String value) {
        if (value.startsWith("-")) {
            return value.substring(1);
        }
        if (value.equals(String.valueOf(alphabet.getZero()))) {
            return value;
        }
        return "-" + value;
    }
}