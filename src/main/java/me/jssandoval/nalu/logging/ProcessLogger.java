package me.jssandoval.nalu.logging;

import java.util.ArrayList;
import java.util.List;

public class ProcessLogger {
    private final List<OperationStep> operationHistory;
    private int currentStepCounter;

    public ProcessLogger() {
        this.operationHistory = new ArrayList<>();
        this.currentStepCounter = 1;
    }

    // El Parser llamará a este método cada vez que resuelva un fragmento de la ecuación
    public void registerStep(String subEquation, String operationName,
                             String visualProcessLog, String stepResult, String fullEquationState) {

        OperationStep newStep = new OperationStep(
                currentStepCounter,
                subEquation,
                operationName,
                visualProcessLog,
                stepResult,
                fullEquationState
        );

        operationHistory.add(newStep);
        currentStepCounter++;
    }

    // La interfaz gráfica llamará a este método para construir los botones interactivos
    public List<OperationStep> getHistory() {
        return operationHistory;
    }

    public void clearLog() {
        operationHistory.clear();
        currentStepCounter = 1;
    }

    // Herramienta visual estática para alinear los dibujos matemáticos
    public static String padLeft(String text, int desiredLength) {
        if (text.length() >= desiredLength) return text;
        StringBuilder padding = new StringBuilder();
        for (int i = 0; i < desiredLength - text.length(); i++) {
            padding.append(" ");
        }
        return padding.append(text).toString();
    }
}