https://drive.google.com/drive/folders/1sjV4AQg4qQPKFJRH47IyneEzJKNqLiyp

public static String convertToBase10(String numberStr, Alphabet alphabet) {
        numberStr = alphabet.clearZeros(numberStr);
        
        String integerPart = numberStr;
        String fractionalPart = "";
        
        if (numberStr.contains(".")) {
            String[] parts = numberStr.split("\\.");
            integerPart = parts[0];
            fractionalPart = parts.length > 1 ? parts[1] : "";
        }

        BigInteger base = BigInteger.valueOf(alphabet.getBase());
        BigInteger decimalInteger = BigInteger.ZERO;

        for (int i = 0; i < integerPart.length(); i++) {
            int digitValue = alphabet.getIndex(integerPart.charAt(i));
            decimalInteger = decimalInteger.multiply(base).add(BigInteger.valueOf(digitValue));
        }

        if (fractionalPart.isEmpty()) {
            return decimalInteger.toString();
        }

        BigDecimal decimalFraction = BigDecimal.ZERO;
        BigDecimal baseDecimal = new BigDecimal(base);
        BigDecimal currentDivisor = baseDecimal; 

        for (int i = 0; i < fractionalPart.length(); i++) {
            int digitValue = alphabet.getIndex(fractionalPart.charAt(i));
            BigDecimal digitDecimal = new BigDecimal(digitValue);
            
            BigDecimal term = digitDecimal.divide(currentDivisor, 15, RoundingMode.HALF_UP);
            decimalFraction = decimalFraction.add(term);
            
            currentDivisor = currentDivisor.multiply(baseDecimal);
        }

        BigDecimal totalDecimal = new BigDecimal(decimalInteger).add(decimalFraction);
        
        return totalDecimal.stripTrailingZeros().toPlainString();
    }