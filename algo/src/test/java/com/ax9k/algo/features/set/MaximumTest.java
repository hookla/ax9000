package com.ax9k.algo.features.set;

class MaximumTest implements SetFeatureSpecification {
    @Override
    public SetFeatureTestCase[] testCases() {
        return new SetFeatureTestCase[] {
                testCase(new double[] { 1, 2, 3, 4, }, 4),
                testCase(new double[] { 9, 7, 6, 3, }, 9),
                testCase(new double[] { 1, 0, 0, 0, }, 1),
                testCase(new double[] { 0, 0, 0, 0, }, 0),
                testCase(new double[] {}, 0),
                testCase(new double[] { 150, -20, }, 150),
                testCase(new double[] { 300, 600, -2000, 100, }, 600),
                };
    }

    @Override
    public SetFeature setFeature() {
        return new Maximum();
    }
}