package com.ax9k.algo.trading;

import org.apache.commons.lang3.Validate;

public final class Directives {
    private static final TradeDirective NO_OP = (__) -> {};

    private Directives() {
        throw new AssertionError("Directives is not instantiable");
    }

    public static TradeDirective buy(double quantity) {
        return new Buy(quantity);
    }

    public static TradeDirective sell(double quantity) {
        return new Sell(quantity);
    }

    public static TradeDirective exit(String reason) {
        return new Exit(reason);
    }

    public static TradeDirective none() {
        return NO_OP;
    }

    private static final class Buy implements TradeDirective {
        private final double quantity;

        private Buy(double quantity) {
            Validate.finite(quantity, "Invalid quantity: %s", quantity);
            Validate.isTrue(quantity > 0, "Cannot buy zero or less: %s", quantity);
            this.quantity = quantity;
        }

        @Override
        public void execute(TradingAlgo algo) {
            algo.buyAtMarket(quantity);
        }
    }

    private static final class Sell implements TradeDirective {
        private final double quantity;

        private Sell(double quantity) {
            Validate.finite(quantity, "Invalid quantity: %s", quantity);
            Validate.isTrue(quantity > 0, "Cannot sell zero or less: %s", quantity);
            this.quantity = quantity;
        }

        @Override
        public void execute(TradingAlgo algo) {
            algo.sellAtMarket(quantity);
        }
    }

    private static final class Exit implements TradeDirective {
        private final String reason;

        private Exit(String reason) {
            this.reason = Validate.notBlank(reason, "Exit reason");
        }

        @Override
        public void execute(TradingAlgo algo) {
            algo.exitPosition(reason);
        }
    }
}
