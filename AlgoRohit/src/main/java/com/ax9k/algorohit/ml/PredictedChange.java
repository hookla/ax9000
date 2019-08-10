package com.ax9k.algorohit.ml;

public enum PredictedChange {
    DOWN("2"), UP("1"), NO_CHANGE("0");

    private final String code;

    PredictedChange(String code) {
        this.code = code;
    }

    static PredictedChange fromLabel(String label) {
        label = label.trim().toUpperCase();
        try {
            return PredictedChange.valueOf(label);
        } catch (IllegalArgumentException noMatchingName) {
            return fromCode(label);
        }
    }

    private static PredictedChange fromCode(String code) {
        if (code.equals(DOWN.code)) {
            return DOWN;
        } else if (code.equals(NO_CHANGE.code)) {
            return NO_CHANGE;
        } else if (code.equals(UP.code)) {
            return UP;
        }

        throw new IllegalArgumentException(
                "PredictedChange with given label or code '" +
                code +
                "' does not exist."
        );
    }
}
