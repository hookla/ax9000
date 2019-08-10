import com.ax9k.algorohit.ml.AmazonMLAlgoFactory;
import com.ax9k.algorohit.ml.GenerateTrainingDataFactory;

module com.ax9k.algorohit {
    requires transitive com.ax9k.core;
    requires transitive com.ax9k.algo;
    requires transitive com.ax9k.positionmanager;
    requires transitive com.ax9k.utils;

    requires com.ax9k.cex;
    requires org.apache.logging.log4j;
    requires s3;
    requires core;

    requires org.apache.commons.lang3;
    requires machinelearning;
    requires org.apache.logging.log4j.core;

    exports com.ax9k.algorohit to com.ax9k.service;
    opens com.ax9k.algorohit to com.fasterxml.jackson.databind;

    provides com.ax9k.algo.AlgoFactory with
            com.ax9k.algorohit.IntradayMomentumAlgoFactory,
            GenerateTrainingDataFactory,
            AmazonMLAlgoFactory,
            com.ax9k.algorohit.TrainingAlgoFactory;
}