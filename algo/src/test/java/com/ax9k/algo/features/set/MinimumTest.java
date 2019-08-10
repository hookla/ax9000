package com.ax9k.algo.features.set;

class MinimumTest implements SetFeatureSpecification {
    @Override
    public SetFeatureTestCase[] testCases() {
        return new SetFeatureTestCase[] {
                testCase(new double[] { 1, 2, 3, 4, }, 1),
                testCase(new double[] { 9, 7, 6, 3, }, 3),
                testCase(new double[] { 1, 0, 0, 0, }, 0),
                testCase(new double[] { 0, 0, 0, 0, }, 0),
                testCase(new double[] {}, 0),
                testCase(new double[] { 150, -20, }, -20),
                testCase(new double[] { 300, 600, -2000, 100, }, -2000),
                };
    }

    @Override
    public SetFeature setFeature() {
        return new Minimum();
    }
}