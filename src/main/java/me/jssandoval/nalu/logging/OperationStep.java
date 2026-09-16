package me.jssandoval.nalu.logging;

public class OperationStep {
    private final int stepNumber;
    private final String subEquation;         // Ej: "1F.0 + 0A.8"
    private final String operationName;       // Ej: "Suma (Base 16)"
    private final String visualProcessLog;    // El dibujo con acarreos o préstamos
    private final String stepResult;          // Ej: "29.8"
    private final String fullEquationState;   // Ej: "29.8 * (B/C)" (Cómo quedó la ecuación tras este paso)

    public OperationStep(int stepNumber, String subEquation, String operationName,
                         String visualProcessLog, String stepResult, String fullEquationState) {
        this.stepNumber = stepNumber;
        this.subEquation = subEquation;
        this.operationName = operationName;
        this.visualProcessLog = visualProcessLog;
        this.stepResult = stepResult;
        this.fullEquationState = fullEquationState;
    }

    // Getters para que la interfaz gráfica (Swing) pueda leer los datos
    public int getStepNumber() { return stepNumber; }
    public String getSubEquation() { return subEquation; }
    public String getOperationName() { return operationName; }
    public String getVisualProcessLog() { return visualProcessLog; }
    public String getStepResult() { return stepResult; }
    public String getFullEquationState() { return fullEquationState; }
}