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

package software.amazon.awssdk.metrics;

import static software.amazon.awssdk.utils.StringUtils.repeat;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.event.Level;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.Validate;

/**
 * An implementation of {@link MetricPublisher} that logs all published metrics under the {@code
 * software.amazon.awssdk.metrics.LoggingMetricPublisher} namespace.
 * <p>
 * {@link LoggingMetricPublisher} can be configured with a {@link Level} to control the log level at which metrics are recorded
 * and a {@link Format} to control the format that metrics are printed in.
 * <p>
 * {@link Format#PLAIN} can be used to print all metrics on a single line. E.g.,
 * <pre>
 * Metrics published: MetricCollection(name=ApiCall, metrics=[MetricRecord(metric=MarshallingDuration, value=PT0.000202197S),
 * MetricRecord(metric=RetryCount, value=0), MetricRecord(metric=ApiCallSuccessful, value=true),
 * MetricRecord(metric=OperationName, value=HeadObject), MetricRecord(metric=ApiCallDuration, value=PT0.468369S),
 * MetricRecord(metric=CredentialsFetchDuration, value=PT0.000003191S), MetricRecord(metric=ServiceId, value=S3)],
 * children=[MetricCollection(name=ApiCallAttempt, metrics=[MetricRecord(metric=SigningDuration, value=PT0.000667268S),
 * MetricRecord(metric=ServiceCallDuration, value=PT0.460529977S), MetricRecord(metric=AwsExtendedRequestId,
 * value=jY/Co5Ge6WjRYk78kGOYQ4Z/CqUBr6pAAPZtexgOQR3Iqs3QP0OfZz3fDraQiXtmx7eXCZ4sbO0=), MetricRecord(metric=HttpStatusCode,
 * value=200), MetricRecord(metric=BackoffDelayDuration, value=PT0S), MetricRecord(metric=AwsRequestId, value=6SJ82R65SADHX098)],
 * children=[MetricCollection(name=HttpClient, metrics=[MetricRecord(metric=AvailableConcurrency, value=0),
 * MetricRecord(metric=LeasedConcurrency, value=0), MetricRecord(metric=ConcurrencyAcquireDuration, value=PT0.230757S),
 * MetricRecord(metric=PendingConcurrencyAcquires, value=0), MetricRecord(metric=MaxConcurrency, value=50),
 * MetricRecord(metric=HttpClientName, value=NettyNio)], children=[])])])
 * </pre>
 * {@link Format#PRETTY} can be used to print metrics over multiple lines in a readable fashion suitable for debugging. E.g.,
 * <pre>
 * [18e5092e] ApiCall
 * [18e5092e] ┌────────────────────────────────────────┐
 * [18e5092e] │ MarshallingDuration=PT0.000227427S     │
 * [18e5092e] │ RetryCount=0                           │
 * [18e5092e] │ ApiCallSuccessful=true                 │
 * [18e5092e] │ OperationName=HeadObject               │
 * [18e5092e] │ ApiCallDuration=PT0.541751S            │
 * [18e5092e] │ CredentialsFetchDuration=PT0.00000306S │
 * [18e5092e] │ ServiceId=S3                           │
 * [18e5092e] └────────────────────────────────────────┘
 * [18e5092e]     ApiCallAttempt
 * [18e5092e]     ┌───────────────────────────────────────────────────────────────────────────────────────────────────┐
 * [18e5092e]     │ SigningDuration=PT0.000974924S                                                                    │
 * [18e5092e]     │ ServiceCallDuration=PT0.531462375S                                                                │
 * [18e5092e]     │ AwsExtendedRequestId=eGfwjV3mSwQZQD4YxHLswYguvhQoGcDTkr2jRvpio37a6QmhWd18C8wagC8LkBzzcnOOKoMuiXw= │
 * [18e5092e]     │ HttpStatusCode=200                                                                                │
 * [18e5092e]     │ BackoffDelayDuration=PT0S                                                                         │
 * [18e5092e]     │ AwsRequestId=ED46TP7NN62DDG4Q                                                                     │
 * [18e5092e]     └───────────────────────────────────────────────────────────────────────────────────────────────────┘
 * [18e5092e]         HttpClient
 * [18e5092e]         ┌────────────────────────────────────────┐
 * [18e5092e]         │ AvailableConcurrency=0                 │
 * [18e5092e]         │ LeasedConcurrency=0                    │
 * [18e5092e]         │ ConcurrencyAcquireDuration=PT0.235851S │
 * [18e5092e]         │ PendingConcurrencyAcquires=0           │
 * [18e5092e]         │ MaxConcurrency=50                      │
 * [18e5092e]         │ HttpClientName=NettyNio                │
 * [18e5092e]         └────────────────────────────────────────┘
 * </pre>
 * Note that the output format may be subject to small changes in future versions and should not be relied upon as a strict public
 * contract.
 */
@SdkPublicApi
public final class LoggingMetricPublisher implements MetricPublisher {

    public enum Format {
        PLAIN,
        PRETTY
    }

    private static final Logger LOGGER = Logger.loggerFor(LoggingMetricPublisher.class);
    private static final Integer PRETTY_INDENT_SIZE = 4;

    private final Level logLevel;
    private final Format format;

    private LoggingMetricPublisher(Level logLevel, Format format) {
        this.logLevel = Validate.notNull(logLevel, "logLevel");
        this.format = Validate.notNull(format, "format");
    }

    /**
     * Create a {@link LoggingMetricPublisher} with the default configuration of {@link Level#INFO} and {@link Format#PLAIN}.
     */
    public static LoggingMetricPublisher create() {
        return new LoggingMetricPublisher(Level.INFO, Format.PLAIN);
    }

    /**
     * Create a {@link LoggingMetricPublisher} with a custom {@link Format} and log {@link Level}.
     *
     * @param logLevel the SLF4J log level to log metrics with
     * @param format   the format to print the metrics with (see class-level documentation for examples)
     */
    public static LoggingMetricPublisher create(Level logLevel, Format format) {
        return new LoggingMetricPublisher(logLevel, format);
    }

    @Override
    public void publish(MetricCollection metrics) {
        if (!LOGGER.isLoggingLevelEnabled(logLevel)) {
            return;
        }
        switch (format) {
            case PLAIN:
                LOGGER.log(logLevel, () -> "Metrics published: " + metrics);
                break;
            case PRETTY:
                String guid = Integer.toHexString(metrics.hashCode());
                logPretty(guid, metrics, 0);
                break;
            default:
                throw new IllegalStateException("Unsupported format: " + format);
        }
    }

    private void logPretty(String guid, MetricCollection metrics, int indent) {
        // Pre-determine metric key-value-pairs so that we can calculate the necessary padding
        List<String> metricValues = new ArrayList<>();
        metrics.forEach(m -> {
            metricValues.add(String.format("%s=%s", m.metric().name(), m.value()));
        });

        int maxLen = getMaxLen(metricValues);

        // MetricCollection name
        LOGGER.log(logLevel, () -> String.format("[%s]%s %s",
                                                 guid,
                                                 repeat(" ", indent),
                                                 metrics.name()));

        // Open box
        LOGGER.log(logLevel, () -> String.format("[%s]%s ┌%s┐",
                                                 guid,
                                                 repeat(" ", indent),
                                                 repeat("─", maxLen + 2)));

        // Metric key-value-pairs
        metricValues.forEach(metric -> LOGGER.log(logLevel, () -> {
            return String.format("[%s]%s │ %s │",
                                 guid,
                                 repeat(" ", indent),
                                 pad(metric, maxLen));
        }));

        // Close box
        LOGGER.log(logLevel, () -> String.format("[%s]%s └%s┘",
                                                 guid,
                                                 repeat(" ", indent),
                                                 repeat("─", maxLen + 2)));

        // Recursively repeat for any children
        metrics.children().forEach(child -> logPretty(guid, child, indent + PRETTY_INDENT_SIZE));
    }

    private static int getMaxLen(List<String> strings) {
        int maxLen = 0;
        for (String str : strings) {
            maxLen = Math.max(maxLen, str.length());
        }
        return maxLen;
    }

    private static String pad(String str, int length) {
        return str + repeat(" ", length - str.length());
    }

    @Override
    public void close() {
    }
}
