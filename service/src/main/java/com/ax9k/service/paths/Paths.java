package com.ax9k.service.paths;

public final class Paths {
    public static final String TRADING_DAY = "/marketdata/tradingday";
    public static final String LATEST_BAR = "/marketdata/latestbar";
    public static final String MARKET_LAST_TRADE = "/marketdata/lasttrade";
    public static final String CURRENT_BOOK = "/marketdata/currentbook";
    public static final String BIDS = "/marketdata/currentbook/bids";
    public static final String ASKS = "/marketdata/currentbook/asks";

    public static final String PROVIDER = "/marketdata/provider";
    public static final String PROVIDER_START_REQUEST = "/marketdata/provider/startrequest";
    public static final String PROVIDER_STOP_REQUEST = "/marketdata/provider/stoprequest";

    public static final String POSITION = "/broker/position";
    public static final String ORDERS = "/broker/orders";
    public static final String BROKER_LAST_TRADE = "/broker/lasttrade";

    public static final String BROKER = "/broker";
    public static final String BROKER_CONNECT = "/broker/connect";
    public static final String BROKER_DISCONNECT = "/broker/disconnect";

    public static final String ALGO = "/algo";

    public static final String FEATURES = "/algo/features";
    public static final String PERIODIC_FEATURES = "/algo/periodicfeatures";

    public static final String HEART_BEAT = "/heartbeat";
    public static final String CLOCK = "/clock";

    public static final String RESUME_TRADING = "/algo/resumetrading";
    public static final String BROKER_REQUEST_DATA = "/broker/requestdata";

    public static final String EXIT_POSITION = "/broker/position/exit";
    public static final String POSITION_MANAGER_BUY = "/positionmanager/buy";
    public static final String POSITION_MANAGER_SELL = "/positionmanager/sell";
    public static final String POSITION_CANCEL_ALL_PENDING_ORDERS = "/positionmanager/cancelallpendingorders";
    public static final String SHUT_DOWN = "/shutdown";

    private Paths() {
        throw new AssertionError("Paths is not instantiable");
    }
}
