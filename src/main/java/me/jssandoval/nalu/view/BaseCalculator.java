package me.jssandoval.nalu.view;

import me.jssandoval.nalu.base.Alphabet;
import me.jssandoval.nalu.logging.OperationStep;
import me.jssandoval.nalu.base.BaseConverter;
import me.jssandoval.nalu.processor.Parser;
import me.jssandoval.nalu.logging.ProcessLogger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseCalculator {
    private JFrame mainFrame;
    private JTextField ecuationField;
    private JSpinner baseSpinner;
    private JSpinner targetBaseSpinner;
    private JButton detectarButton, calcularButton;

    // Contenedores dinámicos
    private JPanel variablesPanel;
    private JPanel stepsPanel;

    // Etiquetas de resultado
    private JTextField finalResultLabel;
    private JTextField verifResultLabel;
    private JTextField targetResultLabel;

    // Memoria de variables
    private Map<String, JTextField> mapVariables;

    public BaseCalculator() {
        mapVariables = new HashMap<>();
        initComponents();
        setupEvents();
    }

    private void initComponents() {
        mainFrame = new JFrame("Calculadora Base N - NALU (Peano)");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1000, 680);
        mainFrame.setLocationRelativeTo(null);

        // ==========================================
        // PANEL IZQUIERDO: INPUTS Y VARIABLES
        // ==========================================
        JPanel leftPanel = new JPanel(new BorderLayout(10, 10));
        leftPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Sub-panel superior izquierdo (Bases y Ecuación)
        JPanel inputConfigPanel = new JPanel(new GridLayout(4, 1, 5, 8));
        inputConfigPanel.setBorder(BorderFactory.createTitledBorder(null, "Configuración Inicial", TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 14)));

        // Fila 1: Base Origen N
        JPanel basePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        basePanel.add(new JLabel("Base Origen N (2-64): "));
        baseSpinner = new JSpinner(new SpinnerNumberModel(32, 2, 64, 1));
        basePanel.add(baseSpinner);
        inputConfigPanel.add(basePanel);

        // Fila 2: Base Destino B
        JPanel targetBasePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        targetBasePanel.add(new JLabel("Base Destino B (2-64): "));
        targetBaseSpinner = new JSpinner(new SpinnerNumberModel(10, 2, 64, 1));
        targetBasePanel.add(targetBaseSpinner);
        inputConfigPanel.add(targetBasePanel);

        // Fila 3: Ecuación
        JPanel eqPanel = new JPanel(new BorderLayout(5, 5));
        eqPanel.add(new JLabel("Ecuación: "), BorderLayout.WEST);
        ecuationField = new JTextField("(A+B)*(B/C)");
        ecuationField.setFont(new Font("Monospaced", Font.PLAIN, 16));
        eqPanel.add(ecuationField, BorderLayout.CENTER);
        inputConfigPanel.add(eqPanel);

        // Fila 4: Botón Detectar
        detectarButton = new JButton("1. Detectar Variables");
        inputConfigPanel.add(detectarButton);

        leftPanel.add(inputConfigPanel, BorderLayout.NORTH);

        // Sub-panel central izquierdo (Variables dinámicas)
        variablesPanel = new JPanel();
        variablesPanel.setLayout(new BoxLayout(variablesPanel, BoxLayout.Y_AXIS));
        JScrollPane variablesScroll = new JScrollPane(variablesPanel);
        variablesScroll.setBorder(BorderFactory.createTitledBorder("Valores de las Variables"));
        leftPanel.add(variablesScroll, BorderLayout.CENTER);

        calcularButton = new JButton("2. Calcular Ecuación");
        calcularButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        calcularButton.setBackground(new Color(0, 120, 215));
        calcularButton.setForeground(Color.WHITE);
        leftPanel.add(calcularButton, BorderLayout.SOUTH);

        // ==========================================
        // PANEL DERECHO: RESULTADO Y PROCESO
        // ==========================================
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        JPanel resultHeaderPanel = new JPanel(new GridLayout(3, 1, 4, 4));
        resultHeaderPanel.setBorder(BorderFactory.createTitledBorder("Resumen de Resultados"));

        // 1. Base Origen
        finalResultLabel = createCopyableLabel("Base Origen: ---", new Color(34, 139, 34), 16);

        // 2. Base 10 (Verificación)
        verifResultLabel = createCopyableLabel("Base 10 (Verificación): ---", new Color(0, 102, 204), 16);

        // 3. Base Destino
        targetResultLabel = createCopyableLabel("Base Destino: ---", new Color(112, 48, 160), 16);

        resultHeaderPanel.add(finalResultLabel);
        resultHeaderPanel.add(verifResultLabel);
        resultHeaderPanel.add(targetResultLabel);

        rightPanel.add(resultHeaderPanel, BorderLayout.NORTH);

        resultHeaderPanel.add(finalResultLabel);
        resultHeaderPanel.add(verifResultLabel);
        resultHeaderPanel.add(targetResultLabel);

        rightPanel.add(resultHeaderPanel, BorderLayout.NORTH);

        // Panel donde se inyectarán los botones de los pasos
        stepsPanel = new JPanel();
        stepsPanel.setLayout(new BoxLayout(stepsPanel, BoxLayout.Y_AXIS));
        JScrollPane stepsScroll = new JScrollPane(stepsPanel);
        stepsScroll.setBorder(BorderFactory.createTitledBorder("Cuaderno de Resolución (Clickeable)"));
        rightPanel.add(stepsScroll, BorderLayout.CENTER);

        // Dividir la pantalla
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(420);
        mainFrame.add(splitPane);
    }

    private void setupEvents() {
        // Evento 1: Detectar Variables
        detectarButton.addActionListener(e -> {
            variablesPanel.removeAll();
            mapVariables.clear();

            String eq = ecuationField.getText();
            Pattern pattern = Pattern.compile("[a-zA-Z_]+");
            Matcher matcher = pattern.matcher(eq);

            Set<String> variablesEncontradas = new LinkedHashSet<>();
            while (matcher.find()) {
                variablesEncontradas.add(matcher.group());
            }

            for (String var : variablesEncontradas) {
                // Creamos un panel por cada fila para mantener la estructura horizontal (rectángulo)
                JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
                rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); // Limitar alto de la fila

                JLabel label = new JLabel(var + " = ");
                label.setFont(new Font("SansSerif", Font.BOLD, 14));
                label.setPreferredSize(new Dimension(40, 25)); // Espacio fijo para la etiqueta

                JTextField field = new JTextField();
                field.setFont(new Font("Monospaced", Font.PLAIN, 14));

                // AQUÍ ESTÁ LA CLAVE: Forzamos una forma rectangular horizontal (Ancho x Alto)
                field.setPreferredSize(new Dimension(300, 30));

                rowPanel.add(label);
                rowPanel.add(field);

                variablesPanel.add(rowPanel);
                mapVariables.put(var, field);
            }

            variablesPanel.revalidate();
            variablesPanel.repaint();
        });

        // Evento 2: Calcular Ecuación, Verificación y Conversión
        calcularButton.addActionListener(e -> {
            try {
                stepsPanel.removeAll();

                // 1. Preparar las bases
                int baseN = (Integer) baseSpinner.getValue();
                int baseB = (Integer) targetBaseSpinner.getValue();

                Alphabet alphabetN = new Alphabet(baseN);
                Alphabet alphabetB = new Alphabet(baseB);

                ProcessLogger logger = new ProcessLogger();
                Parser parser = new Parser(alphabetN, logger);

                // 2. Extraer memoria de variables
                Map<String, String> memoria = new HashMap<>();
                for (Map.Entry<String, JTextField> entry : mapVariables.entrySet()) {
                    String valor = entry.getValue().getText().trim();
                    if (valor.isEmpty()) throw new IllegalArgumentException("La variable " + entry.getKey() + " está vacía.");
                    memoria.put(entry.getKey(), valor);
                }

                // 3. Resolver la ecuación en Base N (Axiomas de Peano)
                String ecuacion = ecuationField.getText();
                String resultadoFinalBaseN = parser.resolve(ecuacion, memoria);

                // 4. NUEVO: Verificación Cruzada Algebraica en Base 10 y Conversión Final a Base B
                BaseConverter.FullVerificationResult verification = BaseConverter.verifyFullEquation(ecuacion, memoria, alphabetN);
                BaseConverter.ConversionResult conversion = BaseConverter.convertToBaseB(verification.getFinalResultBase10(), alphabetB);

                // Mostrar resumen de resultados en la cabecera
                finalResultLabel.setText("Base " + baseN + " (Origen): " + resultadoFinalBaseN);
                verifResultLabel.setText("Base 10 (Verificación): " + verification.getFinalResultBase10());
                targetResultLabel.setText("Base " + baseB + " (Destino): " + conversion.getResultInBaseB());

                // 5. Dibujar los pasos clickeables de las operaciones Peano
                java.util.List<OperationStep> historial = logger.getHistory();
                for (OperationStep paso : historial) {
                    JButton btnPaso = crearBotonPaso(
                            "Paso " + paso.getStepNumber() + ": " + paso.getSubEquation() + " = " + paso.getStepResult(),
                            paso.getOperationName(),
                            paso.getSubEquation(),
                            paso.getVisualProcessLog(),
                            paso.getFullEquationState(),
                            new Color(240, 240, 240),
                            Color.BLACK
                    );
                    stepsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                    stepsPanel.add(btnPaso);
                }

                // Paso extra 1: Verificación cruzada (Variables -> Reemplazo -> Solución Decimal)
                JButton btnVerif = crearBotonPaso(
                        "Paso V: Verificación Completa (Variables -> Ecuación Decimal)",
                        "Verificación Algebraica Cruzada en Base 10",
                        ecuacion,
                        verification.getVisualLog(),
                        "Si la aritmética es correcta, el resultado aquí debe coincidir con Base N convertido.",
                        new Color(0, 102, 204),
                        Color.WHITE
                );
                stepsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                stepsPanel.add(btnVerif);

                // Paso extra 2: Conversión a Base B por Divisiones Sucesivas
                JButton btnConv = crearBotonPaso(
                        "Paso C: Conversión por Divisiones (Base 10 ➔ Base " + baseB + ")",
                        "Conversión por Divisiones/Multiplicaciones",
                        verification.getFinalResultBase10() + " (Base 10)",
                        conversion.getVisualLog(),
                        "Resultado Final en Base " + baseB + " = " + conversion.getResultInBaseB(),
                        new Color(112, 48, 160),
                        Color.WHITE
                );
                stepsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
                stepsPanel.add(btnConv);

            } catch (Exception ex) {
                finalResultLabel.setText("ERROR MATEMÁTICO");
                verifResultLabel.setText("---");
                targetResultLabel.setText("---");
                JOptionPane.showMessageDialog(mainFrame, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }

            stepsPanel.revalidate();
            stepsPanel.repaint();
        });
    }

    private JTextField createCopyableLabel(String initialText, Color textColor, int fontSize) {
        JTextField field = new JTextField(initialText);
        field.setFont(new Font("Monospaced", Font.BOLD, fontSize));
        field.setForeground(textColor);
        field.setEditable(false);       // Impide que el usuario escriba en él
        field.setBorder(null);          // Elimina el borde de caja de texto tradicional
        field.setOpaque(false);         // Fondo transparente para mimetizarse con el panel
        field.setCursor(new Cursor(Cursor.TEXT_CURSOR)); // Cambia el cursor a modo selección de texto
        return field;
    }

    private JButton crearBotonPaso(String textoBoton, String operacion, String subEq, String visualLog, String estadoEcuacion, Color bg, Color fg) {
        JButton btn = new JButton(textoBoton);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(520, 38));
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFocusPainted(false);

        btn.addActionListener(click -> mostrarDetallePaso(textoBoton, operacion, subEq, visualLog, estadoEcuacion));
        return btn;
    }

    /**
     * Muestra la ventana modal del cuaderno matemático con:
     * - Tema oscuro moderno (Catppuccin Mocha).
     * - Ajuste dinámico de dimensiones (Ancho/Alto según el contenido).
     */
    private void mostrarDetallePaso(String tituloPaso, String operacion, String subEq, String visualLog, String estadoEcuacion) {
        JDialog dialog = new JDialog(mainFrame, tituloPaso, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(24, 24, 37)); // Dark Catppuccin Mocha
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Entegado Superior
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 2, 4));
        headerPanel.setOpaque(false);

        JLabel opLabel = new JLabel("Operación: " + operacion);
        opLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        opLabel.setForeground(new Color(249, 226, 175)); // Dorado

        JLabel subEqLabel = new JLabel("Resolviendo: " + subEq);
        subEqLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subEqLabel.setForeground(new Color(137, 180, 250)); // Azul suave

        headerPanel.add(opLabel);
        headerPanel.add(subEqLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Áreal de texto para el dibujo ASCII de la operación
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 15));
        if (textArea.getFont().getFamily().equals("Dialog")) {
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 15));
        }
        textArea.setBackground(new Color(30, 30, 46));
        textArea.setForeground(new Color(166, 227, 161)); // Menta suave
        textArea.setCaretColor(Color.WHITE);
        textArea.setMargin(new Insets(10, 10, 10, 10));

        StringBuilder detail = new StringBuilder();
        detail.append(visualLog).append("\n");
        if (estadoEcuacion != null && !estadoEcuacion.isEmpty()) {
            detail.append("=========================================================================\n");
            detail.append("Estado tras este paso:\n");
            detail.append("-> ").append(estadoEcuacion).append("\n");
        }

        String textContent = detail.toString();
        textArea.setText(textContent);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(69, 71, 90), 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // Panel inferior con botones de acción
        JPanel footerPanel = new JPanel(new BorderLayout(10, 5));
        footerPanel.setOpaque(false);

        JButton copyBtn = new JButton("Copiar Proceso");
        copyBtn.addActionListener(e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textContent), null);
            JOptionPane.showMessageDialog(dialog, "Proceso copiado al portapapeles.", "Copiado", JOptionPane.INFORMATION_MESSAGE);
        });

        JButton closeBtn = new JButton("Cerrar");
        closeBtn.addActionListener(e -> dialog.dispose());

        JPanel btnBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnBox.setOpaque(false);
        btnBox.add(copyBtn);
        btnBox.add(closeBtn);

        footerPanel.add(btnBox, BorderLayout.EAST);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);

        // ==========================================
        // CÁLCULO DINÁMICO DEL TAMAÑO DE LA VENTANA
        // ==========================================
        Font font = textArea.getFont();
        FontMetrics fm = textArea.getFontMetrics(font);
        String[] lines = textContent.split("\n");

        int maxLineLength = 0;
        for (String line : lines) {
            if (line.length() > maxLineLength) {
                maxLineLength = line.length();
            }
        }

        int charWidth = fm.charWidth('M');
        int lineHeight = fm.getHeight();

        // Cálculo de dimensiones basadas en cantidad de texto y líneas
        int contentWidth = maxLineLength * charWidth + 75;
        int contentHeight = lines.length * lineHeight + 155;

        // Limitar al 85% de la pantalla
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = (int) (screenSize.width * 0.85);
        int maxHeight = (int) (screenSize.height * 0.85);

        int minWidth = 630;
        int minHeight = 420;

        int finalWidth = Math.min(Math.max(contentWidth, minWidth), maxWidth);
        int finalHeight = Math.min(Math.max(contentHeight, minHeight), maxHeight);

        dialog.setSize(new Dimension(finalWidth, finalHeight));
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setVisible(true);
    }

    public void mostrar() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.updateComponentTreeUI(mainFrame);
        mainFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new BaseCalculator().mostrar();
        });
    }
}