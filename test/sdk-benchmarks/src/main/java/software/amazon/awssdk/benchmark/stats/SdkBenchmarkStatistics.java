/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.benchmark.stats;

import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.openjdk.jmh.util.Statistics;

/**
 * SDK wrapper of benchmark statistics, created for easy serialization/deserialization.
 */
public class SdkBenchmarkStatistics implements StatisticalSummary {

    private double mean;

    private double variance;

    private double standardDeviation;

    private double max;

    private double min;

    private long n;

    private double sum;

    public SdkBenchmarkStatistics() {
    }

    public SdkBenchmarkStatistics(Statistics statistics) {
        this.mean = statistics.getMean();
        this.variance = statistics.getVariance();
        this.standardDeviation = statistics.getStandardDeviation();
        this.max = statistics.getMax();
        this.min = statistics.getMin();
        this.n = statistics.getN();
        this.sum = statistics.getSum();
    }

    @Override
    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    @Override
    public double getVariance() {
        return variance;
    }

    public void setVariance(double variance) {
        this.variance = variance;
    }

    @Override
    public double getStandardDeviation() {
        return standardDeviation;
    }

    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }

    @Override
    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    @Override
    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }

    @Override
    public long getN() {
        return n;
    }

    public void setN(long n) {
        this.n = n;
    }

    @Override
    public double getSum() {
        return sum;
    }

    public void setSum(double sum) {
        this.sum = sum;
    }
}
