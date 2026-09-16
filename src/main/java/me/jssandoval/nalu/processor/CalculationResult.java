package me.jssandoval.nalu.processor;

public class CalculationResult {
    private final String result;
    private final String visualProcess;

    public CalculationResult(String result, String visualProcess) {
        this.result = result;
        this.visualProcess = visualProcess;
    }

    public String getResult() {
        return result;
    }

    public String getVisualProcess() {
        return visualProcess;
    }
}