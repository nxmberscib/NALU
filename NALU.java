package me.jssandoval.nalu;

import java.util.*;
import java.util.regex.*;
import java.math.BigDecimal;
import java.math.BigInteger;

public class NALU {

    // ==========================================
    // 1. CLASE ALPHABET (Gestión de Símbolos)
    // ==========================================
    public static class Alphabet {
        private static final String MASTER_ALPHABET = "0123456789ABCDEFGHIJKLMNÑOPQRSTUVWXYZabcdefghijklmnñopqrstuvwxyz";
        private final int base;
        private final String currentAlphabet;

        public Alphabet(int base) {
            if (base < 2 || base > MASTER_ALPHABET.length()) {
                throw new IllegalArgumentException("Base fuera de rango (2-" + MASTER_ALPHABET.length() + ")");
            }
            this.base = base;
            this.currentAlphabet = MASTER_ALPHABET.substring(0, base);
        }

        public int getBase() { return base; }
        public char getZero() { return currentAlphabet.charAt(0); }
        public char getSymbol(int index) { return currentAlphabet.charAt(index); }
        public int getIndex(char symbol) {
            int idx = currentAlphabet.indexOf(symbol);
            if (idx == -1) throw new IllegalArgumentException("Símbolo '" + symbol + "' no pertenece al alfabeto.");
            return idx;
        }
        public char nextSymbol(char symbol) { return currentAlphabet.charAt((getIndex(symbol) + 1) % base); }
        public char prevSymbol(char symbol) { return currentAlphabet.charAt((getIndex(symbol) - 1 + base) % base); }

        public String limpiarCeros(String num) {
            boolean isNeg = num.startsWith("-");
            if (isNeg) num = num.substring(1);
            char zero = getZero();
            while (num.length() > 1 && num.charAt(0) == zero) { num = num.substring(1); }
            String result = num.isEmpty() ? String.valueOf(zero) : num;
            if (result.equals(String.valueOf(zero))) return result; // Evitar el "-0"
            return isNeg ? "-" + result : result;
        }
    }

    public static class FractionalALU {
            private final ALU alu;
            private final Alphabet alphabet;
            private final char DOT = '.';
            private final int MAX_PRECISION = 10;

            public FractionalALU(ALU alu) {
                this.alu = alu;
                this.alphabet = alu.getAlphabet();
            }

            private int getFractionalLength(String num) {
                int idx = num.indexOf(DOT);
                return idx == -1 ? 0 : num.length() - idx - 1;
            }

            private String removeDot(String num) {
                return num.replace(String.valueOf(DOT), "");
            }

            private String padRight(String num, int zeros) {
                StringBuilder sb = new StringBuilder(num);
                for (int i = 0; i < zeros; i++) sb.append(alphabet.getZero());
                return sb.toString();
            }

            private String injectDot(String num, int decimals) {
                boolean isNeg = num.startsWith("-");
                if (isNeg) num = num.substring(1);

                while (num.length() <= decimals) {
                    num = alphabet.getZero() + num;
                }

                if (decimals > 0) {
                    int insertPos = num.length() - decimals;
                    num = num.substring(0, insertPos) + DOT + num.substring(insertPos);
                }

                if (num.contains(String.valueOf(DOT))) {
                    while (num.endsWith(String.valueOf(alphabet.getZero()))) {
                        num = num.substring(0, num.length() - 1);
                    }
                    if (num.endsWith(String.valueOf(DOT))) {
                        num = num.substring(0, num.length() - 1);
                    }
                }

                String[] parts = num.split("\\.");
                String intPart = alphabet.limpiarCeros(parts[0]);
                num = parts.length > 1 ? intPart + DOT + parts[1] : intPart;

                return isNeg && !num.equals(String.valueOf(alphabet.getZero())) ? "-" + num : num;
            }

            public String sumar(String a, String b) {
                int decA = getFractionalLength(a);
                int decB = getFractionalLength(b);
                int maxDec = Math.max(decA, decB);

                String intA = removeDot(padRight(a, maxDec - decA));
                String intB = removeDot(padRight(b, maxDec - decB));

                String resInt = alu.sumar(intA, intB);
                String resultado = injectDot(resInt, maxDec);

                int width = Math.max(a.length(), Math.max(b.length(), resultado.length())) + 4;
                System.out.println("\n" + String.format("%" + width + "s", a));
                System.out.println("+" + String.format("%" + (width - 1) + "s", b));
                System.out.println("-".repeat(width));
                System.out.println(" " + String.format("%" + (width - 1) + "s", resultado));

                return resultado;
            }

            public String restar(String a, String b) {
                int decA = getFractionalLength(a);
                int decB = getFractionalLength(b);
                int maxDec = Math.max(decA, decB);

                String intA = removeDot(padRight(a, maxDec - decA));
                String intB = removeDot(padRight(b, maxDec - decB));

                String resInt = alu.restar(intA, intB);
                String resultado = injectDot(resInt, maxDec);

                int width = Math.max(a.length(), Math.max(b.length(), resultado.length())) + 4;
                System.out.println("\n" + String.format("%" + width + "s", a));
                System.out.println("-" + String.format("%" + (width - 1) + "s", b));
                System.out.println("-".repeat(width));
                System.out.println(" " + String.format("%" + (width - 1) + "s", resultado));

                return resultado;
            }

            public String multiplicar(String a, String b) {
                int totalDecimals = getFractionalLength(a) + getFractionalLength(b);
                String cleanA = removeDot(a);
                String cleanB = removeDot(b);

                int width = Math.max(a.length(), Math.max(b.length(), a.length() + b.length())) + 6;
                System.out.println("\n" + String.format("%" + width + "s", a));
                System.out.println("*" + String.format("%" + (width - 1) + "s", b));
                System.out.println("-".repeat(width));

                // Generar y mostrar productos parciales paso a paso
                String zeroStr = String.valueOf(alphabet.getZero());
                String acumuladoFinal = zeroStr;
                List<String> parcialesFormateados = new ArrayList<>();

                for (int i = cleanB.length() - 1; i >= 0; i--) {
                    String singleMultiplier = String.valueOf(cleanB.charAt(i));
                    String parcialInt = alu.multiplicar(cleanA, singleMultiplier);

                    if (!parcialInt.equals(zeroStr)) {
                        for (int k = 0; k < (cleanB.length() - 1 - i); k++) {
                            parcialInt = parcialInt + zeroStr;
                        }
                    }

                    // Ajustar decimales visuales del parcial según corresponda
                    int shiftDecimals = totalDecimals - (cleanB.length() - 1 - i);
                    String parcialVal = injectDot(parcialInt, Math.max(0, shiftDecimals));
                    parcialesFormateados.add(parcialVal);

                    System.out.println("  " + String.format("%" + (width - 3) + "s", parcialVal));
                }

                if (parcialesFormateados.size() > 1) {
                    System.out.println("-".repeat(width));
                }

                String result = alu.multiplicar(cleanA, cleanB);
                String resultado = injectDot(result, totalDecimals);
                System.out.println("=" + String.format("%" + (width - 1) + "s", resultado));

                return resultado;
            }

            public String dividir(String a, String b) {
                int decA = getFractionalLength(a);
                int decB = getFractionalLength(b);
                int maxDec = Math.max(decA, decB);

                String intA = removeDot(padRight(a, maxDec - decA));
                String intB = removeDot(padRight(b, maxDec - decB));
                intA = padRight(intA, MAX_PRECISION);

                String quotient = alu.dividir(intA, intB)[0];
                String resultado = injectDot(quotient, MAX_PRECISION);

                int width = Math.max(a.length(), Math.max(b.length(), resultado.length())) + 6;
                System.out.println("\n  División estructural:");
                System.out.println("  " + b + " | " + a);
                System.out.println("  " + "-".repeat(width));
                System.out.println("    Cociente parcial -> " + resultado);

                return resultado;
            }
        }

    // ALU
    public static class ALU {
        private final Alphabet alphabet;
        public ALU(Alphabet alphabet) { this.alphabet = alphabet; }
        public Alphabet getAlphabet() { return alphabet; }

        private String abs(String val) {
            return val.startsWith("-") ? val.substring(1) : val;
        }

        private boolean isNeg(String val) {
            return val.startsWith("-") && !val.equals("-" + alphabet.getZero());
        }

        public int compareAbs(String a, String b) {
            a = alphabet.limpiarCeros(abs(a)); b = alphabet.limpiarCeros(abs(b));
            if (a.length() != b.length()) return a.length() > b.length() ? 1 : -1;
            return a.compareTo(b);
        }

        public int compare(String a, String b) {
            boolean negA = isNeg(a), negB = isNeg(b);
            if (negA && !negB) return -1;
            if (!negA && negB) return 1;
            int cmpAbs = compareAbs(a, b);
            return negA ? -cmpAbs : cmpAbs;
        }

        public String increment(String num) {
            num = alphabet.limpiarCeros(num);
            char zero = alphabet.getZero();
            char maxChar = alphabet.getSymbol(alphabet.getBase() - 1);
            if (num.equals(String.valueOf(zero))) return String.valueOf(alphabet.getBase() > 1 ? alphabet.getSymbol(1) : zero);
            char lastChar = num.charAt(num.length() - 1);
            String prefix = num.substring(0, num.length() - 1);
            if (lastChar == maxChar) return alphabet.limpiarCeros(increment(prefix) + zero);
            else return prefix + alphabet.nextSymbol(lastChar);
        }

        public String decrement(String num) {
            num = alphabet.limpiarCeros(num);
            char zero = alphabet.getZero();
            char maxChar = alphabet.getSymbol(alphabet.getBase() - 1);
            if (num.equals(String.valueOf(zero))) throw new ArithmeticException("Underflow estructural.");
            char lastChar = num.charAt(num.length() - 1);
            String prefix = num.substring(0, num.length() - 1);
            if (lastChar == zero) return alphabet.limpiarCeros(decrement(prefix) + maxChar);
            else return alphabet.limpiarCeros(prefix + alphabet.prevSymbol(lastChar));
        }

        // ====== WRAPPERS CON SOPORTE DE SIGNOS ======

        public String sumar(String a, String b) {
            boolean negA = isNeg(a), negB = isNeg(b);
            String absA = abs(a), absB = abs(b);
            if (!negA && !negB) return sumarAbs(absA, absB);
            if (negA && negB) return "-" + sumarAbs(absA, absB);
            if (negA) return restar(absB, absA); // b + (-a) = b - a
            return restar(absA, absB); // a + (-b) = a - b
        }

        public String restar(String a, String b) {
            boolean negA = isNeg(a), negB = isNeg(b);
            String absA = abs(a), absB = abs(b);
            if (!negA && !negB) {
                if (compareAbs(absA, absB) < 0) return "-" + restarAbs(absB, absA);
                return restarAbs(absA, absB);
            }
            if (negA && !negB) return "-" + sumarAbs(absA, absB); // -a - b = -(a + b)
            if (!negA && negB) return sumarAbs(absA, absB); // a - (-b) = a + b
            return restar(absB, absA); // -a - (-b) = b - a
        }

        public String multiplicar(String a, String b) {
            boolean negA = isNeg(a), negB = isNeg(b);
            String resAbs = multiplicarAbs(abs(a), abs(b));
            if (resAbs.equals(String.valueOf(alphabet.getZero()))) return resAbs;
            return (negA != negB) ? "-" + resAbs : resAbs;
        }

        public String[] dividir(String a, String b) {
            boolean negA = isNeg(a), negB = isNeg(b);
            String[] resAbs = dividirAbs(abs(a), abs(b));
            String q = resAbs[0], r = resAbs[1];
            String zeroStr = String.valueOf(alphabet.getZero());

            if (!q.equals(zeroStr) && (negA != negB)) q = "-" + q;
            if (!r.equals(zeroStr) && negA) r = "-" + r; // En Java, el residuo conserva el signo del dividendo

            return new String[]{ q, r };
        }

        private String sumarAbs(String a, String b) {
            a = alphabet.limpiarCeros(a); b = alphabet.limpiarCeros(b);
            String zeroStr = String.valueOf(alphabet.getZero());
            StringBuilder resultado = new StringBuilder();
            String carry = zeroStr;
            int i = a.length() - 1; int j = b.length() - 1;

            while (i >= 0 || j >= 0 || !carry.equals(zeroStr)) {
                String d1 = (i >= 0) ? String.valueOf(a.charAt(i)) : zeroStr;
                String d2 = (j >= 0) ? String.valueOf(b.charAt(j)) : zeroStr;
                String sumaColumna = zeroStr, cnt = zeroStr;

                while (!cnt.equals(d1)) { sumaColumna = increment(sumaColumna); cnt = increment(cnt); }
                cnt = zeroStr;
                while (!cnt.equals(d2)) { sumaColumna = increment(sumaColumna); cnt = increment(cnt); }
                cnt = zeroStr;
                while (!cnt.equals(carry)) { sumaColumna = increment(sumaColumna); cnt = increment(cnt); }

                int base = alphabet.getBase(); int idxTotal = 0;
                String tmp = sumaColumna;
                while (!tmp.equals(zeroStr)) { idxTotal++; tmp = decrement(tmp); }

                resultado.append(alphabet.getSymbol(idxTotal % base));
                carry = zeroStr;
                for (int c = 0; c < (idxTotal / base); c++) carry = increment(carry);
                i--; j--;
            }
            return alphabet.limpiarCeros(resultado.reverse().toString());
        }

        private String restarAbs(String a, String b) {
            a = alphabet.limpiarCeros(a); b = alphabet.limpiarCeros(b);
            StringBuilder resultado = new StringBuilder();
            int borrow = 0, base = alphabet.getBase(), i = a.length() - 1, j = b.length() - 1;

            while (i >= 0) {
                int d1 = alphabet.getIndex(a.charAt(i));
                int d2 = (j >= 0) ? alphabet.getIndex(b.charAt(j)) : 0;
                int sub = d1 - d2 - borrow;
                if (sub < 0) { sub += base; borrow = 1; } else { borrow = 0; }
                resultado.append(alphabet.getSymbol(sub));
                i--; j--;
            }
            return alphabet.limpiarCeros(resultado.reverse().toString());
        }

        private String multiplicarAbs(String a, String b) {
            a = alphabet.limpiarCeros(a); b = alphabet.limpiarCeros(b);
            String zeroStr = String.valueOf(alphabet.getZero());
            if (a.equals(zeroStr) || b.equals(zeroStr)) return zeroStr;

            String resultadoFinal = zeroStr; int base = alphabet.getBase();
            for (int i = b.length() - 1; i >= 0; i--) {
                int valD2 = alphabet.getIndex(b.charAt(i));
                StringBuilder parcial = new StringBuilder();
                for (int k = 0; k < (b.length() - 1 - i); k++) parcial.append(zeroStr);

                String carry = zeroStr;
                for (int j = a.length() - 1; j >= 0; j--) {
                    int valD1 = alphabet.getIndex(a.charAt(j));
                    String prodCol = zeroStr, cnt = zeroStr;
                    for (int step = 0; step < (valD1 * valD2); step++) prodCol = increment(prodCol);
                    while (!cnt.equals(carry)) { prodCol = increment(prodCol); cnt = increment(cnt); }

                    int valProd = 0; String tmp = prodCol;
                    while (!tmp.equals(zeroStr)) { valProd++; tmp = decrement(tmp); }

                    parcial.append(alphabet.getSymbol(valProd % base));
                    carry = zeroStr;
                    for (int c = 0; c < (valProd / base); c++) carry = increment(carry);
                }
                if (!carry.equals(zeroStr)) parcial.append(carry);
                resultadoFinal = sumarAbs(resultadoFinal, parcial.reverse().toString());
            }
            return alphabet.limpiarCeros(resultadoFinal);
        }

        private String[] dividirAbs(String a, String b) {
            a = alphabet.limpiarCeros(a); b = alphabet.limpiarCeros(b);
            String zeroStr = String.valueOf(alphabet.getZero());
            if (b.equals(zeroStr)) throw new ArithmeticException("División por cero.");
            String cociente = zeroStr, residuo = a;

            while (compareAbs(residuo, b) >= 0) {
                residuo = restarAbs(residuo, b);
                cociente = increment(cociente);
            }
            return new String[]{alphabet.limpiarCeros(cociente), alphabet.limpiarCeros(residuo)};
        }
    }

    public static class Parser {

        public static String aBase10(String numStr, Alphabet alphabet) {
            boolean neg = numStr.startsWith("-");
            String absStr = neg ? numStr.substring(1) : numStr;

            String intPart = absStr;
            String fracPart = "";
            if (absStr.contains(".")) {
                String[] parts = absStr.split("\\.");
                intPart = parts[0];
                fracPart = parts.length > 1 ? parts[1] : "";
            }

            // 1. Cálculo de la parte entera
            BigInteger intResult = BigInteger.ZERO;
            BigInteger baseNum = BigInteger.valueOf(alphabet.getBase());
            for (int i = 0; i < intPart.length(); i++) {
                int val = alphabet.getIndex(intPart.charAt(i));
                intResult = intResult.multiply(baseNum).add(BigInteger.valueOf(val));
            }

            BigDecimal totalResult = new BigDecimal(intResult);

            // 2. Cálculo de la parte fraccionaria (si existe)
            if (!fracPart.isEmpty()) {
                BigDecimal baseDecimal = new BigDecimal(alphabet.getBase());
                BigDecimal divisor = baseDecimal;
                for (int i = 0; i < fracPart.length(); i++) {
                    int val = alphabet.getIndex(fracPart.charAt(i));
                    BigDecimal digitVal = new BigDecimal(val);
                    // Suma el dígito / (base^(i+1)) con alta precisión
                    totalResult = totalResult.add(digitVal.divide(divisor, 15, java.math.RoundingMode.HALF_UP));
                    divisor = divisor.multiply(baseDecimal);
                }
            }

            return neg ? totalResult.negate().toPlainString() : totalResult.toPlainString();
        }

        public static String aBase10ConProceso(String numStr, Alphabet alphabet) {
            boolean neg = numStr.startsWith("-");
            String absStr = alphabet.limpiarCeros(neg ? numStr.substring(1) : numStr);

            String intPart = absStr;
            String fracPart = "";
            if (absStr.contains(".")) {
                String[] parts = absStr.split("\\.");
                intPart = parts[0];
                fracPart = parts.length > 1 ? parts[1] : "";
            }

            BigDecimal baseDecimal = new BigDecimal(alphabet.getBase());
            BigDecimal sumaVerificacion = BigDecimal.ZERO;

            System.out.println("\n=========================================================================");
            System.out.println("                   VERIFICACIÓN (BASE " + alphabet.getBase() + " A BASE 10)");
            System.out.println("=========================================================================\n");

            System.out.printf("%-10s | %-9s | %-7s | %-20s | %s%n",
                    "Posición", "Símbolo", "Valor", "Potencia (Base^Pos)", "Subtotal");
            System.out.println("-".repeat(75));

            int lenInt = intPart.length();
            // 1. Procesar parte entera (posiciones positivas/cero)
            for (int j = 0; j < lenInt; j++) {
                int pos = lenInt - 1 - j;
                char simbolo = intPart.charAt(j);
                int valorPos = alphabet.getIndex(simbolo);

                BigDecimal peso = baseDecimal.pow(pos);
                BigDecimal subtotal = new BigDecimal(valorPos).multiply(peso);
                sumaVerificacion = sumaVerificacion.add(subtotal);

                String infoPotencia = alphabet.getBase() + "^" + pos + " = " + peso.toPlainString();
                String infoSimbolo = "'" + simbolo + "'";

                System.out.printf("D%-9d | %-9s | %-7d | %-20s | %s%n",
                        pos, infoSimbolo, valorPos, infoPotencia, subtotal.toPlainString());
            }

            // 2. Procesar parte fraccionaria (posiciones negativas)
            int lenFrac = fracPart.length();
            for (int j = 0; j < lenFrac; j++) {
                int pos = -(j + 1);
                char simbolo = fracPart.charAt(j);
                int valorPos = alphabet.getIndex(simbolo);

                BigDecimal divisor = baseDecimal.pow(j + 1);
                BigDecimal peso = BigDecimal.ONE.divide(divisor, 15, java.math.RoundingMode.HALF_UP);
                BigDecimal subtotal = new BigDecimal(valorPos).multiply(peso);
                sumaVerificacion = sumaVerificacion.add(subtotal);

                String infoPotencia = alphabet.getBase() + "^" + pos + " = " + peso.stripTrailingZeros().toPlainString();
                String infoSimbolo = "'" + simbolo + "'";

                System.out.printf("D%-9d | %-9s | %-7d | %-20s | %s%n",
                        pos, infoSimbolo, valorPos, infoPotencia, subtotal.stripTrailingZeros().toPlainString());
            }

            System.out.println("-".repeat(75));
            if (neg && sumaVerificacion.compareTo(BigDecimal.ZERO) != 0) {
                sumaVerificacion = sumaVerificacion.negate();
                System.out.printf("%-53s %s%n", "SUMA TOTAL EN BASE 10 (Negativa):", sumaVerificacion.toPlainString());
            } else {
                System.out.printf("%-53s %s%n", "SUMA TOTAL EN BASE 10:", sumaVerificacion.toPlainString());
            }
            System.out.println("=========================================================================");

            return sumaVerificacion.toPlainString();
        }
        public static void convertirAOtraBaseConProceso(String numStr, Alphabet sourceAlphabet, int targetBase) {
            BigInteger decimalVal = new BigInteger(aBase10(numStr, sourceAlphabet));
            Alphabet targetAlphabet = new Alphabet(targetBase);
            BigInteger baseDestino = BigInteger.valueOf(targetBase);

            boolean neg = decimalVal.compareTo(BigInteger.ZERO) < 0;
            if (neg) {
                decimalVal = decimalVal.negate();
            }

            System.out.println("\n=========================================================================");
            System.out.println("                     CONVERSIÓN (BASE 10 A BASE " + targetBase + ")");
            System.out.println("=========================================================================\n");

            if (neg) System.out.println(" -> Nota: Operando valor absoluto. El signo '-' se añadirá al final.");
            System.out.println(" -> Valor intermedio en Base 10: " + decimalVal + "\n");

            String resultadoBaseDestino;

            if (decimalVal.equals(BigInteger.ZERO)) {
                resultadoBaseDestino = String.valueOf(targetAlphabet.getZero());
                System.out.println("  Paso 1: Valor 0 -> Símbolo: '" + targetAlphabet.getZero() + "'");
            } else {
                BigInteger tempB = decimalVal;
                resultadoBaseDestino = "";
                int pasoB = 1;

                System.out.printf("%-6s | %-50s | %-9s | %s%n",
                        "Paso", "Ecuación (Dividendo = Cociente * Base + Residuo)", "Símbolo", "Acumulado");
                System.out.println("-".repeat(92));

                while (tempB.compareTo(BigInteger.ZERO) > 0) {
                    BigInteger[] divRem = tempB.divideAndRemainder(baseDestino);
                    BigInteger cociente = divRem[0];
                    int residuo = divRem[1].intValue();
                    char caracter = targetAlphabet.getSymbol(residuo);
                    resultadoBaseDestino = caracter + resultadoBaseDestino;

                    String ecuacion = tempB + " = " + cociente + " * " + targetBase + " + " + residuo;
                    String infoSimbolo = residuo + " -> '" + caracter + "'";

                    System.out.printf("%-6d | %-50s | %-9s | %s%n",
                            pasoB, ecuacion, infoSimbolo, resultadoBaseDestino);

                    tempB = cociente;
                    pasoB++;
                }
            }

            if (neg && !resultadoBaseDestino.equals(String.valueOf(targetAlphabet.getZero()))) {
                resultadoBaseDestino = "-" + resultadoBaseDestino;
            }

            System.out.println("\n -> Resultado final en Base " + targetBase + ": " + resultadoBaseDestino);
            System.out.println("=========================================================================");
        }

        public String resolverBaseN(String ecuacion, Map<String, String> memoria, FractionalALU fracAlu) {
            return resolverPasoAPaso(ecuacion, memoria, (left, op, right) -> {
                switch (op) {
                    case "+": return fracAlu.sumar(left, right);
                    case "-": return fracAlu.restar(left, right);
                    case "*": return fracAlu.multiplicar(left, right);
                    case "/": return fracAlu.dividir(left, right);
                    // El módulo con fracciones es matemáticamente ambiguo en este contexto,
                    // puedes dejarlo apuntando a un error o a la ALU original entera.
                    default: throw new IllegalArgumentException("Operador desconocido");
                }
            });
        }

        public String resolverBase10(String ecuacion, Map<String, String> memoria) {
            return resolverPasoAPaso(ecuacion, memoria, (left, op, right) -> {
                BigInteger n1 = new BigInteger(left);
                BigInteger n2 = new BigInteger(right);
                switch (op) {
                    case "+": return n1.add(n2).toString();
                    case "-": return n1.subtract(n2).toString();
                    case "*": return n1.multiply(n2).toString();
                    case "/": return n1.divide(n2).toString();
                    case "%": return n1.remainder(n2).toString();
                    default: throw new IllegalArgumentException("Operador desconocido");
                }
            });
        }

        private String resolverPasoAPaso(String ecuacionOriginal, Map<String, String> memoria, OperadorBinario opBinario) {
            String eq = ecuacionOriginal.replaceAll("\\s+", "");

            System.out.println("   " + eq);
            for (Map.Entry<String, String> entry : memoria.entrySet()) {
                eq = eq.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
            }
            if (!eq.equals(ecuacionOriginal)) {
                System.out.println(" = " + eq);
            }

            Pattern patternBloque = Pattern.compile("([\\(\\[\\{])([^\\(\\[\\{\\)\\]\\}]+)([\\)\\]\\}])");

            Pattern opPri1 = Pattern.compile("(\\-?[A-Za-z0-9Ññ\\.]+)([\\*/%])(\\-?[A-Za-z0-9Ññ\\.]+)");
            Pattern opPri2 = Pattern.compile("(\\-?[A-Za-z0-9Ññ\\.]+)([\\+\\-])(\\-?[A-Za-z0-9Ññ\\.]+)");

            while (true) {
                Matcher mBloque = patternBloque.matcher(eq);

                if (mBloque.find()) {
                    String openP = mBloque.group(1);
                    String contenido = mBloque.group(2);
                    String closeP = mBloque.group(3);

                    String nuevoContenido = intentarOperar(contenido, opPri1, opBinario)
                            .or(() -> intentarOperar(contenido, opPri2, opBinario))
                            .orElse(null);

                    if (nuevoContenido != null) {
                        eq = eq.substring(0, mBloque.start()) + openP + nuevoContenido + closeP + eq.substring(mBloque.end());
                    } else {
                        eq = eq.substring(0, mBloque.start()) + contenido + eq.substring(mBloque.end());
                    }
                    System.out.println(" = " + eq);
                    continue;

                } else {
                    var tempEq = eq;
                    String nuevaEq = intentarOperar(eq, opPri1, opBinario)
                            .or(() -> intentarOperar(tempEq, opPri2, opBinario))
                            .orElse(null);

                    if (nuevaEq == null) break;

                    eq = nuevaEq;
                    System.out.println(" = " + eq);
                }
            }
            return eq;
        }

        private Optional<String> intentarOperar(String texto, Pattern patron, OperadorBinario opBinario) {
            Matcher m = patron.matcher(texto);
            if (!m.find()) return Optional.empty();
            String res = opBinario.operar(m.group(1), m.group(2), m.group(3));
            return Optional.of(texto.substring(0, m.start()) + res + texto.substring(m.end()));
        }

        @FunctionalInterface
        interface OperadorBinario {
            String operar(String left, String op, String right);
        }
    }

    // CLASE CLI (Consola e Interfaz)
    public static class CLI {
        public static void iniciar() {
            Scanner scanner = new Scanner(System.in);
            System.out.println("=======================================================");
            System.out.println("             CALCULADORA DE BASES (PEANO)");
            System.out.println("=======================================================");

            int base = 32;
            System.out.print("\nIngrese la Base numérica (2 - 64): ");
            String baseInput = scanner.nextLine().trim();
            if (!baseInput.isEmpty()) base = Integer.parseInt(baseInput);

            Alphabet alphabet = new Alphabet(base);
            ALU alu = new ALU(alphabet);
            Parser parser = new Parser();

            System.out.println("\nEjemplo de formato: {[(A*B)+(A+C)]+(B%C)}");
            System.out.print("Ingrese la ecuación a resolver: ");
            String ecuacion = scanner.nextLine().replaceAll("\\s+", "");

            Set<String> variables = new TreeSet<>();
            Matcher matcher = Pattern.compile("[a-zA-Z_]\\w*").matcher(ecuacion);
            while (matcher.find()) variables.add(matcher.group());

            Map<String, String> memoriaRam = new HashMap<>();
            Map<String, String> memoriaDecimal = new HashMap<>();

            if (!variables.isEmpty()) {
                System.out.println("\n--- ASIGNACIÓN DE VARIABLES (EN BASE " + base + ") ---");
                for (String var : variables) {
                    while (true) {
                        System.out.print("Ingrese el valor para [" + var + "]: ");
                        String val = scanner.nextLine().trim();
                        try {
                            // 1. Ignorar el signo menos inicial y remover el punto decimal para la validación
                            String checkVal = val.startsWith("-") ? val.substring(1) : val;
                            checkVal = checkVal.replace(".", "");

                            // 2. Verificar que cada símbolo pertenezca al alfabeto
                            for (char c : checkVal.toCharArray()) {
                                alphabet.getIndex(c);
                            }

                            String limpio = alphabet.limpiarCeros(val);
                            memoriaRam.put(var, limpio);

                            memoriaDecimal.put(var, Parser.aBase10(limpio, alphabet));
                            break;
                        } catch (Exception e) {
                            System.out.println(" -> Error: Carácter o formato inválido. Intente de nuevo.");
                        }
                    }
                }
            }

            System.out.println("\n=======================================================");
            System.out.println("    SOLUCIÓN DE LA ECUACIÓN (BASE " + base + ")");
            System.out.println("=======================================================");

            FractionalALU fracAlu = new FractionalALU(alu);
            String resultadoFinalBaseN = parser.resolverBaseN(ecuacion, memoriaRam, fracAlu);

            System.out.println("\n[PAUSA] Resultado final en Base " + base + ": " + resultadoFinalBaseN);
            System.out.println("Presiona [ENTER] para auditar la verificación en Base 10...");
            scanner.nextLine();

            String esperadoDecimal = Parser.aBase10ConProceso(resultadoFinalBaseN, alphabet);

            System.out.println("\n=======================================================");
            System.out.println("   VERIFICACIÓN DECIMAL (BASE 10)");
            System.out.println("=======================================================");

            String resultadoFinalDecimal = parser.resolverBase10(ecuacion, memoriaDecimal);

            System.out.println("\n=======================================================");
            System.out.println(" -> RESUMEN:");
            System.out.println(" -> Ecuacion (Traducida a Base 10) : " + esperadoDecimal);
            System.out.println(" -> Matemáticas Decimales Directas   : " + resultadoFinalDecimal);
            if (esperadoDecimal.equals(resultadoFinalDecimal)) {
                System.out.println(" -> El resultado Base 10 coincide.");
            } else {
                System.out.println(" -> El resultado base 10 no coincide.");
            }
            System.out.println("=======================================================");

            // --- 3. CONVERSIÓN ADICIONAL A OTRA BASE ---
            System.out.print("\n¿Deseas convertir el resultado a otra base? (s/n): ");
            String respuesta = scanner.nextLine().trim();
            if (respuesta.equalsIgnoreCase("s") || respuesta.equalsIgnoreCase("si") || respuesta.equalsIgnoreCase("sí")) {
                System.out.print("Ingrese la base de destino (2 - 64): ");
                int targetBase = Integer.parseInt(scanner.nextLine().trim());
                try {
                    Parser.convertirAOtraBaseConProceso(resultadoFinalBaseN, alphabet, targetBase);
                } catch (Exception e) {
                    System.out.println(" -> Error en la conversión: " + e.getMessage());
                }
            }
        }
    }

    public static void main(String[] args) {
        CLI.iniciar();
    }
}
