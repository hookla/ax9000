package com.ax9k.algo.features.set;

import com.ax9k.algo.features.Feature;
import com.ax9k.algo.features.Parameters;
import com.ax9k.core.event.Event;
import com.ax9k.core.history.Source;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StandardDeviation implements SetFeature {
    private static final Core TA_LIB = new Core();
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public <T extends Event> double calculate(Feature<T> feature, Source<T> history, Parameters ignored) {
        if (history.isEmpty()) {return 0;}
        int size = history.getSize();

        if (size < 2) {
            return 0;
        }
        double[] inReal = history.stream()
                                 .mapToDouble(feature)
                                 .toArray();

        int startIdx = 0;
        int endIdx = history.getSize() - 1;
        int optInTimePeriod = Integer.MIN_VALUE;
        double optInNbDev = Integer.MIN_VALUE;
        MInteger outBegIdx = new MInteger();
        MInteger outNBElement = new MInteger();
        double[] outReal = new double[size];

        RetCode retCode = TA_LIB.stdDev(
                startIdx, endIdx, inReal, optInTimePeriod, optInNbDev,
                outBegIdx, outNBElement, outReal
        );

        if (retCode != RetCode.Success) {
            LOGGER.warn(retCode);
        }
        return outReal[outBegIdx.value];
    }
}
