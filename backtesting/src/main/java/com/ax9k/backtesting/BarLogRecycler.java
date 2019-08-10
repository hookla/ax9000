package com.ax9k.backtesting;

import com.ax9k.core.marketmodel.bar.OhlcvBar;
import com.ax9k.utils.json.JsonUtils;

import java.io.BufferedReader;
import java.util.function.Function;

class BarLogRecycler implements Function<BufferedReader, OhlcvBar> {
    @Override
    public OhlcvBar apply(BufferedReader lines) {
        return JsonUtils.readLines(lines, OhlcvBar.class);
    }
}
