package vn.edu.fpt.medicaldiagnosis.enums;

public enum SatisfactionLevel {
    VERY_BAD(1),
    BAD(2),
    AVERAGE(3),
    GOOD(4),
    EXCELLENT(5);

    private final int value;

    SatisfactionLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
