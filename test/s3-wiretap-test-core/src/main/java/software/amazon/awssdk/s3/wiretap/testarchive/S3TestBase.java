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

package software.amazon.awssdk.s3.wiretap.testarchive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletionException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.s3.wiretap.client.ClientBuildFailed;
import software.amazon.awssdk.s3.wiretap.client.ClientBuilder;
import software.amazon.awssdk.s3.wiretap.client.ClientBuilds;
import software.amazon.awssdk.s3.wiretap.client.ClientConfig;
import software.amazon.awssdk.s3.wiretap.client.ClientSuccessOrFailure;
import software.amazon.awssdk.s3.wiretap.client.ConfiguredClient;
import software.amazon.awssdk.s3.wiretap.client.HttpClient;
import software.amazon.awssdk.s3.wiretap.client.SyncOverAsync;
import software.amazon.awssdk.s3.wiretap.server.CapturedExchange;
import software.amazon.awssdk.s3.wiretap.server.S3Wiretap;
import software.amazon.awssdk.s3.wiretap.testarchive.helpers.AccountRegionalBucket;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(WiretapLifecycle.class)
public abstract class S3TestBase {

    private static final Logger LOG = LogManager.getLogger(S3TestBase.class);

    private static final AwsCredentialsProvider DUMMY_CREDS = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("AROA123456789EXAMPLE",
                    "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"));

    private static final long LATE_REQUEST_TIMEOUT_MILLIS = 5_000;

    private static final long SETTLE_IDLE_MILLIS = 1_000;
    private static final long SETTLE_TIMEOUT_MILLIS = 15_000;

    private S3Wiretap wiretap;

    void attachWiretap(S3Wiretap attached) {
        this.wiretap = attached;
    }

    protected S3Wiretap.Mode mode() {
        return S3Wiretap.Mode.MOCK;
    }

    protected String bucket() {
        return target().name();
    }

    protected String versionedBucket() {
        return versionedTarget().name();
    }

    protected Region region() {
        return target().region();
    }

    private AccountRegionalBucket target() {
        return mode() == S3Wiretap.Mode.FORWARD
                ? AccountRegionalBucket.resolved()
                : AccountRegionalBucket.MOCK;
    }

    private AccountRegionalBucket versionedTarget() {
        return mode() == S3Wiretap.Mode.FORWARD
                ? AccountRegionalBucket.resolvedVersioned()
                : AccountRegionalBucket.MOCK_VERSIONED;
    }

    @AfterEach
    void resetWiretap() {
        java.util.List<CapturedExchange> leftover = wiretap.exchanges();
        int unmatched = wiretap.unmatchedInterceptorCount();
        java.util.List<String> unmatchedMock = wiretap.unmatchedMockRequests();
        wiretap.reset();
        if (!unmatchedMock.isEmpty()) {
            throw new IllegalStateException(unmatchedMock.size()
                    + " MOCK request(s) had no matching mock response (answered 500) — the test"
                    + " did not program a response for them: " + unmatchedMock);
        }
        if (!leftover.isEmpty()) {
            long inFlight = leftover.stream().filter(CapturedExchange::inFlight).count();
            throw new IllegalStateException(leftover.size()
                    + " request(s) were recorded but not asserted via assertTotalRequests"
                    + " (unaccounted/leaked traffic): "
                    + (leftover.size() - inFlight) + " settled, " + inFlight + " still in flight "
                    + leftover.stream().map(e -> e.request().method() + " " + e.request().path()
                            + " " + e.state()).toList()
                    + (inFlight > 0
                    ? ". A test that deliberately abandons a response must still clear the ledger with"
                    + " its terminal assertion (assertTotalRequests / assertSettledRequestCount) —"
                    + " clearing detaches the exchange, so the wiretap may finish with it afterwards."
                    : ""));
        }
        if (unmatched > 0) {
            throw new IllegalStateException(unmatched
                    + " registered interceptor(s) never matched any request"
                    + " (check the matcher's method and path)");
        }
    }

    protected S3Wiretap wiretap() {
        return wiretap;
    }

    protected void assertTotalRequests(int expected) {
        assertTotalRequests(expected, false);
    }

    protected void assertTotalRequests(int expected, boolean awaitLate) {
        if (awaitLate) {
            wiretap.awaitExchangeCount(expected, S3Wiretap.Inclusion.REGISTERED,
                    LATE_REQUEST_TIMEOUT_MILLIS);
        }
        assertEquals(expected, wiretap.requestCount(),
                () -> "unexpected request count; requests were: " + wiretap.requests());
        wiretap.clearExchanges();
    }

    protected void assertSettledRequestCount(int expected) {
        wiretap.awaitExchangeCount(expected, S3Wiretap.Inclusion.SETTLED, LATE_REQUEST_TIMEOUT_MILLIS);
        wiretap.awaitQuiescence(SETTLE_IDLE_MILLIS, SETTLE_TIMEOUT_MILLIS);
        assertTotalRequests(expected);
    }

    protected void awaitWireQuiescence() {
        wiretap.awaitQuiescence(SETTLE_IDLE_MILLIS, SETTLE_TIMEOUT_MILLIS);
    }

    protected AwsCredentialsProvider credentials() {
        return mode() == S3Wiretap.Mode.FORWARD
                ? AccountRegionalBucket.credentials()
                : DUMMY_CREDS;
    }

    protected S3Client syncClient(HttpClient.Type type) {
        return clientBuilder(type).buildSync();
    }

    protected S3AsyncClient asyncClient(HttpClient.Type type) {
        if (!type.isAsync()) {
            throw new IllegalArgumentException("Not an async type: " + type);
        }
        ClientBuilder builder = clientBuilder(type);
        return type == HttpClient.Type.CRT_CREATE
                ? builder.buildCrtAsync()
                : builder.buildAsync();
    }

    private ClientBuilder newBuilder() {
        return new ClientBuilder(wiretap.port())
                .credentials(credentials())
                .region(region());
    }

    protected ClientBuilder clientBuilder(HttpClient.Type type) {
        ClientBuilder builder = newBuilder();
        HttpClient.apply(builder, type);
        return builder;
    }

    protected S3Client putClient(HttpClient.Type type) {
        return type.isAsync() ? new SyncOverAsync(asyncClient(type)) : syncClient(type);
    }

    protected static boolean isManagedCrt(S3AsyncClient client) {
        return client.getClass().getSimpleName().contains("Crt");
    }

    BuildOutcome buildConfigured(ClientConfig config, Class<?> paramType) {
        if (paramType == ClientBuildFailed.class) {
            return buildExpectingFailure(config);
        }
        if (paramType == ClientSuccessOrFailure.class) {
            return buildEither(config);
        }
        ClientBuilder builder = newBuilder();
        try {
            if (paramType == ConfiguredClient.class) {
                return BuildOutcome.value(config.buildConfigured(builder));
            }
            Object client = paramType == S3AsyncClient.class
                    ? config.buildAsync(builder)
                    : config.buildSync(builder);
            return BuildOutcome.value(client);
        } catch (RuntimeException e) {
            ClientBuilds.rethrowIfEnvironmentFailure(config, e);
            return BuildOutcome.skip("skipped: config [" + config.describe()
                    + "] is not a buildable combination: " + e.getMessage());
        }
    }

    private BuildOutcome buildEither(ClientConfig config) {
        ClientBuilder builder = newBuilder();
        return BuildOutcome.value(ClientBuilds.buildEither(config, builder));
    }

    private BuildOutcome buildExpectingFailure(ClientConfig config) {
        ClientBuilder builder = newBuilder();
        S3Client client;
        try {
            client = config.buildSync(builder);
        } catch (RuntimeException e) {
            ClientBuilds.rethrowIfEnvironmentFailure(config, e);
            return BuildOutcome.value(new ClientBuildFailed(config, e));
        }
        try {
            client.close();
        } catch (RuntimeException e) {
            LOG.debug("best-effort close of a throwaway client failed", e);
        }
        return BuildOutcome.skip("skipped: config [" + config.describe()
                + "] builds successfully; a ClientBuildFailed test covers only failing combos");
    }

    record BuildOutcome(boolean skip, String reason, Object value) {
        static BuildOutcome value(Object value) {
            return new BuildOutcome(false, null, value);
        }

        static BuildOutcome skip(String reason) {
            return new BuildOutcome(true, reason, null);
        }
    }

    protected static void expectS3Error(String errorCode, Executable action) {
        Throwable thrown = assertThrows(Throwable.class, action);
        Throwable cause = thrown instanceof CompletionException && thrown.getCause() != null
                ? thrown.getCause()
                : thrown;
        S3Exception ex = assertInstanceOf(S3Exception.class, cause);
        assertEquals(errorCode, ex.awsErrorDetails().errorCode());
    }

    protected static <T extends Throwable> T expectThrows(Class<T> expected, Executable action) {
        T thrown = assertThrows(expected, action);
        if (!(thrown instanceof S3Exception)) {
            LOG.info("SDK rejected the request/response client-side: {}", thrown.toString());
        }
        return thrown;
    }
}
