package me.jssandoval.nalu.base;

public class Alphabet {
    private static final String MASTER_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
        "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩαβγδεζηθικλμνξοπρστυφχψω" +
        "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзийклмнопрстуфхцчшщъыьэюя" +
        "亜亐亖亡亢交亥亦产亨亩享京亭亮亝亟亰亲亳亵亯亰亱乩亳亴亵亶亷亸亹" +
        "人亻亼亾亿什仁仂仃仄仅仆仇今介仉仌仄付仙仐仝仞仟仠仡仚仛" +
        "仜とにかく代令以仢代令以仦仧仡仩仩仫仪仭仰仱仲仳仴仴件" +
        "价仸仹仺仼ﾡﾢﾣﾤﾥﾦﾧﾨﾩﾪﾫﾬﾭﾮﾯﾰﾱﾲﾳﾴﾵﾶﾷﾸﾹﾺﾻﾼ";

    private final int base;
    private final String currentAlphabet;

    public Alphabet(int base) {
        if (base < 2 || base > MASTER_ALPHABET.length()) {
            throw new IllegalArgumentException("Base fuera de rango (2-" + MASTER_ALPHABET.length() + ")");
        }
        this.base = base;
        this.currentAlphabet = MASTER_ALPHABET.substring(0, base);
    }

    public int getBase() {
        return base;
    }

    public char getZero() {
        return currentAlphabet.charAt(0);
    }

    public char getSymbol(int index) {
        return currentAlphabet.charAt(index);
    }

    public int getIndex(char symbol) {
        int idx = currentAlphabet.indexOf(symbol);
        if (idx == -1) throw new IllegalArgumentException("Símbolo '" + symbol + "' no pertenece al alfabeto.");
        return idx;
    }

    public char nextSymbol(char symbol) {
        return currentAlphabet.charAt((getIndex(symbol) + 1) % base);
    }

    public char prevSymbol(char symbol) {
        return currentAlphabet.charAt((getIndex(symbol) - 1 + base) % base);
    }

    public String clearZeros(String num) {
        char zero = getZero();
        while (num.length() > 1 && num.charAt(0) == zero) {
            num = num.substring(1);
        }
        return num.isEmpty() ? String.valueOf(zero) : num;
    }
}