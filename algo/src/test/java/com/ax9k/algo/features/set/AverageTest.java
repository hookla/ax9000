package com.ax9k.algo.features.set;

class AverageTest implements SetFeatureSpecification {
    @Override
    public SetFeatureTestCase[] testCases() {
        return new SetFeatureTestCase[] {
                testCase(new double[] { 1, 2, 3, 4, }, 2.5),
                testCase(new double[] { 9, 7, 6, 3, }, 6.25),
                testCase(new double[] { 1, 0, 0, 0, }, 0.25),
                testCase(new double[] { 0, 0, 0, 0, }, 0),
                testCase(new double[] {}, 0),
                testCase(new double[] { 150, -20, }, 65),
                testCase(new double[] { 300, 600, -2000, 100, }, -250),
                };
    }

    @Override
    public SetFeature setFeature() {
        return new Average();
    }
}