package com.ax9k.algorohit.ml;

import com.ax9k.core.time.Time;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.awssdk.core.regions.Region;
import software.amazon.awssdk.services.machinelearning.MachineLearningClient;
import software.amazon.awssdk.services.machinelearning.model.GetMLModelRequest;
import software.amazon.awssdk.services.machinelearning.model.GetMLModelResponse;
import software.amazon.awssdk.services.machinelearning.model.PredictRequest;
import software.amazon.awssdk.services.machinelearning.model.PredictResponse;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

final class ModelClient implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Region REGION = Region.US_EAST_1;
    private static final String TIME_PROPERTY = "eventTime";
    private static final int MAX_PREDICTIONS = 1000;
    private final MachineLearningClient client;
    private final PredictRequest.Builder request;
    private int predictionCounter = 0;

    public ModelClient(String modelId) {
        client = MachineLearningClient.builder().region(REGION).build();

        String endpoint = getModelEndpoint(modelId);
        LOGGER.debug("Found ML model endpoint: {}", endpoint);
        request = PredictRequest.builder()
                                .mlModelId(modelId)
                                .predictEndpoint(endpoint);
    }

    private String getModelEndpoint(String modelId) {
        GetMLModelRequest request = GetMLModelRequest.builder()
                                                     .mlModelId(modelId)
                                                     .build();

        GetMLModelResponse model = client.getMLModel(request);
        return model.endpointInfo().endpointUrl();
    }

    com.ax9k.algorohit.ml.Prediction predict(Map<String, ?> features) {
        if (predictionCounter++ > MAX_PREDICTIONS) {
            throw new IllegalStateException("'max predictions exceeded");
        }

        StopWatch stopWatch = StopWatch.createStarted();

        request.record(prepareFeatureData(features));

        Map<String, Float> scores = getPredictedScores();
        LOGGER.debug("Predicted scores: {}", scores::toString);
        com.ax9k.algorohit.ml.Prediction prediction = findMostLikelyPrediction(scores);
        LOGGER.info("got external prediction {} micro sec. {}", stopWatch.getTime(TimeUnit.MICROSECONDS),
                    scores.toString()
        );

        return prediction;
    }

    private Map<String, String> prepareFeatureData(Map<String, ?> features) {
        Map<String, String> result = features.entrySet().stream()
                                             .collect(Collectors.toMap(
                                                     Map.Entry::getKey,
                                                     (entry) -> String.valueOf(entry.getValue())
                                             ));
        if (!result.containsKey(TIME_PROPERTY)) {
            result.put(TIME_PROPERTY, Time.currentTime().toString());
        }
        return result;
    }

    private Map<String, Float> getPredictedScores() {
        PredictResponse response = client.predict(request.build());
        software.amazon.awssdk.services.machinelearning.model.Prediction prediction = response.prediction();
        return prediction.predictedScores();
    }

    private com.ax9k.algorohit.ml.Prediction findMostLikelyPrediction(Map<String, Float> scores) {
        return scores.entrySet().stream()
                     .max(comparing(Map.Entry::getValue))
                     .map(com.ax9k.algorohit.ml.Prediction::new)
                     .orElseThrow(() -> new IllegalStateException("could find max score: " + scores));
    }

    @Override
    public void close() {
        client.close();
    }
}
