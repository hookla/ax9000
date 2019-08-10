package com.ax9k.algorohit.ml;

import java.util.Map;
import java.util.Objects;

public final class Prediction {
    static final Prediction NONE = new Prediction(PredictedChange.NO_CHANGE, 1);

    private final PredictedChange change;
    private final double probability;

    Prediction(Map.Entry<String, Float> score) {
        this(PredictedChange.fromLabel(score.getKey()), score.getValue());
    }

    private Prediction(PredictedChange change, double probability) {
        this.change = change;
        this.probability = probability;
    }

    public PredictedChange getChange() {
        return change;
    }

    public double getProbability() {
        return probability;
    }

    @Override
    public int hashCode() {
        return Objects.hash(change, probability);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        Prediction that = (Prediction) other;
        return change == that.change &&
               Double.compare(that.probability, probability) == 0;
    }

    @Override
    public String toString() {
        return "Prediction{" +
               "change=" + change +
               ", probability=" + probability +
               '}';
    }
}
