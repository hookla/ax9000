module core.ax9k.demo {
    requires com.ax9k.core;
    requires com.ax9k.positionmanager;
    requires com.ax9k.algo;
    requires com.ax9k.utils;

    requires org.apache.logging.log4j;

    provides com.ax9k.algo.AlgoFactory with com.ax9k.demo.DemoAlgoFactory;
}