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

package software.amazon.awssdk.s3.mockserver.testarchive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import software.amazon.awssdk.s3.mockserver.client.ClientBuildFailed;
import software.amazon.awssdk.s3.mockserver.client.ClientBuilder;
import software.amazon.awssdk.s3.mockserver.client.ClientBuilds;
import software.amazon.awssdk.s3.mockserver.client.ClientConfig;
import software.amazon.awssdk.s3.mockserver.client.ClientSuccessOrFailure;
import software.amazon.awssdk.s3.mockserver.client.ConfiguredClient;
import software.amazon.awssdk.s3.mockserver.client.HttpClient;
import software.amazon.awssdk.s3.mockserver.client.Payload;
import software.amazon.awssdk.s3.mockserver.client.SyncOverAsync;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.s3.mockserver.invariants.Destination;
import software.amazon.awssdk.s3.mockserver.invariants.Invariant;
import software.amazon.awssdk.s3.mockserver.invariants.Observation;
import software.amazon.awssdk.s3.mockserver.invariants.ObservationSink;
import software.amazon.awssdk.s3.mockserver.server.CapturedExchange;
import software.amazon.awssdk.s3.mockserver.server.S3RequestFilters;
import software.amazon.awssdk.s3.mockserver.server.S3Wiretap;
import software.amazon.awssdk.s3.mockserver.server.invariants.DestinationCheck;
import software.amazon.awssdk.s3.mockserver.server.invariants.WireInvariants;
import software.amazon.awssdk.s3.mockserver.testarchive.helpers.AccountRegionalBucket;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@ExtendWith(WiretapLifecycle.class)
public abstract class S3TestBase {

    private static final Logger LOG = LogManager.getLogger(S3TestBase.class);

    private static final List<String> CLIENT_THREAD_PREFIXES = Immutable.listOf("sdk-", "in-memory-http-");

    private static final long CLIENT_THREAD_END_MILLIS = 10_000;

    private static final AwsCredentialsProvider DUMMY_CREDS = StaticCredentialsProvider.create(
            AwsBasicCredentials.create("AROA123456789EXAMPLE",
                    "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"));

    private static final long LATE_REQUEST_TIMEOUT_MILLIS = 5_000;

    private static final long SETTLE_IDLE_MILLIS = 1_000;
    private static final long SETTLE_TIMEOUT_MILLIS = 15_000;

    private S3Wiretap wiretap;

    private ObservationSink sink;

    private Iteration iteration;

    private static final class Iteration {
        private final List<SdkAutoCloseable> clients = new java.util.concurrent.CopyOnWriteArrayList<>();
        private final CallerFutures futures = new CallerFutures();
    }

    private final Map<Invariant, List<Pin>> expectedViolations = new EnumMap<>(Invariant.class);

    private static final class Pin {
        private final String finding;
        private final String detailMarker;

        private Pin(String finding, String detailMarker) {
            this.finding = finding;
            this.detailMarker = detailMarker;
        }

        public String finding() {
            return finding;
        }

        public String detailMarker() {
            return detailMarker;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Pin that = (Pin) o;
            return Objects.equals(finding, that.finding)
                    && Objects.equals(detailMarker, that.detailMarker);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(finding);
            result = 31 * result + Objects.hashCode(detailMarker);
            return result;
        }

        @Override
        public String toString() {
            return "Pin[finding=" + finding + ", detailMarker=" + detailMarker + "]";
        }
    }

    void attachWiretap(S3Wiretap attached) {
        this.wiretap = attached;
    }

    void attachSink(ObservationSink opened) {
        this.sink = opened;
        S3Wiretap mine = wiretap;
        opened.destinationCheck(destination ->
                DestinationCheck.check(destination, mine.servedVersions(), mine.history(), opened));
    }

    protected final void expectInvariantViolation(Invariant invariant, String finding, String detailMarker) {
        if (!getClass().getPackage().getName().equals(S3TestBase.class.getPackage().getName() + ".provocation")) {
            throw new IllegalStateException(getClass().getName()
                    + " may not pin an invariant: only classes in the testarchive.provocation package may");
        }
        expectedViolations.computeIfAbsent(invariant, i -> new java.util.ArrayList<>()).add(new Pin(finding, detailMarker));
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
        ObservationSink checked;
        try {
            checked = checkInvariants();
        } finally {
            wiretap.reset();
        }
        List<Observation> observed = checked == null ? Immutable.listOf() : checked.observations();
        Map<Invariant, Long> checkCounts = checked == null ? Immutable.mapOf() : checked.checkCounts();
        List<Observation> invariants = observed.stream().filter(o -> !pinned(o)).collect(Immutable.toList());
        List<String> unmetExpectations = new java.util.ArrayList<>();
        List<String> uncheckedPins = new java.util.ArrayList<>();
        expectedViolations.forEach((invariant, pins) -> {
            if (checkCounts.getOrDefault(invariant, 0L) == 0) {
                uncheckedPins.add(invariant.label() + " is pinned ("
                        + String.join(", ", pins.stream().map(Pin::finding).distinct().collect(Immutable.toList()))
                        + ") but was not checked");
                return;
            }
            pins.stream().filter(pin -> observed.stream().noneMatch(o -> o.invariant() == invariant && matches(pin, o)))
                    .forEach(pin -> unmetExpectations.add(invariant.label() + " was expected not to hold for \""
                            + pin.detailMarker() + "\", pinning " + pin.finding() + ", and it held"));
        });
        observed.stream().filter(this::pinned).forEach(o -> LOG.info("runtime-checks pinned: {}", o));
        expectedViolations.clear();
        List<String> unrun = checked == null ? Immutable.listOf() : checked.harnessErrors();
        IllegalStateException harness = null;
        try {
            assertLedgerAccountedFor(leftover, unmatched, unmatchedMock);
        } catch (IllegalStateException ledger) {
            harness = ledger;
        }
        if (!unrun.isEmpty() || !uncheckedPins.isEmpty()) {
            List<String> problems = new java.util.ArrayList<>(unrun);
            problems.addAll(uncheckedPins);
            IllegalStateException checks = new IllegalStateException("a runtime check could not run: " + problems);
            if (harness == null) {
                harness = checks;
            } else {
                harness.addSuppressed(checks);
            }
        }
        if (harness != null) {
            if (!invariants.isEmpty()) {
                harness.addSuppressed(new AssertionError(describe(invariants)));
            }
            if (!unmetExpectations.isEmpty()) {
                harness.addSuppressed(new AssertionError(String.join("\n", unmetExpectations)));
            }
            throw harness;
        }
        if (!invariants.isEmpty() || !unmetExpectations.isEmpty()) {
            StringBuilder message = new StringBuilder();
            if (!invariants.isEmpty()) {
                message.append(describe(invariants));
            }
            unmetExpectations.forEach(u -> message.append(message.length() == 0 ? "" : "\n").append(u));
            fail(message.toString());
        }
    }

    private ObservationSink checkInvariants() {
        ObservationSink mine = sink;
        if (mine == null) {
            return null;
        }
        try {
            List<CapturedExchange> history = null;
            try {
                history = wiretap.history();
                WireInvariants.check(history, mine.sources(), mine);
            } catch (RuntimeException e) {
                mine.harnessError("the wire checks failed: " + e);
            }
            for (Destination destination : history == null ? Immutable.<Destination>listOf() : mine.destinations()) {
                try {
                    DestinationCheck.check(destination, wiretap.servedVersions(), history, mine);
                } catch (RuntimeException e) {
                    mine.harnessError(destination.path() + " could not be judged: " + e);
                }
            }
        } finally {
            mine.close();
            sink = null;
        }
        LOG.info("runtime-checks {} | checked {} | observed {}", mine.owner(), mine.checkCounts(),
                mine.observations().size());
        return mine;
    }

    private boolean pinned(Observation o) {
        return expectedViolations.getOrDefault(o.invariant(), Immutable.listOf()).stream().anyMatch(pin -> matches(pin, o));
    }

    private static boolean matches(Pin pin, Observation o) {
        return o.detail() != null && o.detail().contains(pin.detailMarker());
    }

    private static String describe(List<Observation> observed) {
        StringBuilder message = new StringBuilder(observed.size()
                + " runtime invariant(s) did not hold:");
        observed.forEach(o -> message.append("\n  - ").append(o));
        return message.toString();
    }

    private static void assertLedgerAccountedFor(List<CapturedExchange> leftover, int unmatched,
                                                 List<String> unmatchedMock) {
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
                            + " " + e.state()).collect(Immutable.toList())
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
        ClientBuilder builder = new ClientBuilder(wiretap.port())
                .credentials(credentials())
                .region(region());
        Iteration current = iteration;
        if (current != null) {
            builder.onBuilt(current.clients::add);
        }
        builder.setInMemoryTransport(RowRepeater.isInstalled());
        return builder;
    }

    protected ClientBuilder clientBuilder(HttpClient.Type type) {
        Iteration current = iteration;
        if (current != null) {
            current.futures.unrecorded("the test built a client of its own, whose calls are not recorded");
        }
        ClientBuilder builder = newBuilder();
        HttpClient.apply(builder, type);
        return builder;
    }

    protected S3Client putClient(HttpClient.Type type) {
        return type.isAsync() ? new SyncOverAsync(asyncClient(type)) : syncClient(type);
    }

    protected static boolean isManagedCrt(S3AsyncClient client) {
        return CallerFutures.unwrap(client).getClass().getSimpleName().contains("Crt");
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

    void runRepeated(RowRepeater repeater, String row, Method method, List<Object> resolved, ClientConfig config,
                     Class<?> paramType, int payloadSize) throws Throwable {
        resolved.forEach(S3TestBase::closeIfClient);
        if (sink != null) {
            sink.close();
            sink = null;
        }
        repeater.repeat(row, () -> runIteration(row, method, resolved, config, paramType, payloadSize));
    }

    private void runIteration(String row, Method method, List<Object> resolved, ClientConfig config,
                              Class<?> paramType, int payloadSize) {
        attachSink(ObservationSink.open(row));
        Iteration current = new Iteration();
        iteration = current;
        java.util.Set<Thread> before = Thread.getAllStackTraces().keySet();
        Throwable failure = null;
        try {
            Object[] args = iterationArguments(method.getParameterTypes(), resolved, payloadSize,
                    () -> recorded(buildOrThrow(config, paramType), current.futures));
            method.setAccessible(true);
            method.invoke(this, args);
        } catch (InvocationTargetException e) {
            failure = e.getCause();
        } catch (ReflectiveOperationException | RuntimeException e) {
            failure = e;
        }
        try {
            checkAfterTheTest(current, before);
            resetWiretap();
        } catch (RuntimeException | AssertionError e) {
            if (failure == null) {
                failure = e;
            } else {
                failure.addSuppressed(e);
            }
        } finally {
            iteration = null;
            expectedViolations.clear();
            if (sink != null) {
                sink.close();
                sink = null;
            }
            current.clients.forEach(S3TestBase::closeIfClient);
        }
        if (failure instanceof RuntimeException) {
            throw (RuntimeException) failure;
        }
        if (failure instanceof Error) {
            throw (Error) failure;
        }
        if (failure != null) {
            throw new IllegalStateException(failure);
        }
    }

    static Object[] iterationArguments(Class<?>[] types, List<Object> resolved, int payloadSize,
                                       java.util.function.Supplier<Object> client) {
        Object[] args = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            if (isClientParameter(types[i])) {
                args[i] = client.get();
            } else if (types[i] == Payload.class) {
                args[i] = Payload.ofSize(payloadSize);
            } else {
                args[i] = resolved.get(i);
            }
        }
        return args;
    }

    private static boolean isClientParameter(Class<?> type) {
        return type == S3Client.class || type == S3AsyncClient.class || type == ConfiguredClient.class
                || type == ClientBuildFailed.class || type == ClientSuccessOrFailure.class;
    }

    private static Object recorded(Object argument, CallerFutures futures) {
        if (argument instanceof ConfiguredClient && ((ConfiguredClient) argument).asyncClient() != null) {
            ConfiguredClient c = (ConfiguredClient) argument;
            S3AsyncClient async = CallerFutures.record(c.asyncClient(), futures);
            return new ConfiguredClient(new SyncOverAsync(async), async, c.config());
        }
        if (argument instanceof S3AsyncClient) {
            return CallerFutures.record((S3AsyncClient) argument, futures);
        }
        if (argument instanceof SyncOverAsync) {
            futures.unrecorded("the row takes an S3Client over an async client, whose calls are not recorded");
        }
        return argument;
    }

    private void checkAfterTheTest(Iteration current, java.util.Set<Thread> before) {
        ObservationSink mine = sink;
        current.futures.check(mine);
        long closedNanos = System.nanoTime();
        current.clients.forEach(S3TestBase::closeIfClient);
        for (Thread t : Thread.getAllStackTraces().keySet()) {
            if (before.contains(t) || CLIENT_THREAD_PREFIXES.stream().noneMatch(t.getName()::startsWith)) {
                continue;
            }
            mine.checked(Invariant.CLIENT_THREADS_END_AT_CLOSE);
            try {
                t.join(CLIENT_THREAD_END_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                mine.harnessError("invariants 9 and 10 were cut short: interrupted while waiting for "
                        + t.getName() + " to end");
                return;
            }
            if (t.isAlive()) {
                mine.record(new Observation(Invariant.CLIENT_THREADS_END_AT_CLOSE, "thread " + t.getName()
                        + " is still alive " + CLIENT_THREAD_END_MILLIS + " ms after the client was closed, in "
                        + Arrays.stream(t.getStackTrace()).limit(8).map(String::valueOf).collect(Immutable.toList())));
            }
        }
        List<CapturedExchange> wire = new java.util.ArrayList<>(wiretap.history());
        wiretap.exchanges().stream().filter(CapturedExchange::inFlight).forEach(wire::add);
        for (CapturedExchange e : wire) {
            mine.checked(Invariant.CLIENT_THREADS_END_AT_CLOSE);
            if (e.startNanos() > closedNanos) {
                mine.record(new Observation(Invariant.CLIENT_THREADS_END_AT_CLOSE, e.request().method() + " "
                        + e.request().path() + " started after the client was closed"));
            }
        }
        String unrecorded = current.futures.unrecordedReason();
        java.util.OptionalLong last = current.futures.lastCompletionNanos();
        if (unrecorded != null || !last.isPresent()) {
            LOG.info("runtime-checks {} | invariant 9 not judged: {}", mine.owner(),
                    unrecorded != null ? unrecorded : "no future the client returned completed");
            return;
        }
        for (CapturedExchange e : wire) {
            mine.checked(Invariant.NO_REQUEST_AFTER_THE_CALLER_FUTURE);
            if (e.startNanos() > last.getAsLong() && e.startNanos() <= closedNanos
                    && !S3RequestFilters.abortMultipartUpload().test(e.request())) {
                mine.record(new Observation(Invariant.NO_REQUEST_AFTER_THE_CALLER_FUTURE, e.request().method() + " "
                        + e.request().path() + " started after the last future the client returned completed"));
            }
        }
    }

    private Object buildOrThrow(ClientConfig config, Class<?> paramType) {
        BuildOutcome outcome = buildConfigured(config, paramType);
        if (outcome.skip()) {
            throw new IllegalStateException("a row that built once did not build again: " + outcome.reason());
        }
        return outcome.value();
    }

    private static void closeIfClient(Object argument) {
        if (argument instanceof ConfiguredClient) {
            ConfiguredClient configured = (ConfiguredClient) argument;
            closeIfClient(configured.asyncClient());
            closeIfClient(configured.client());
        } else if (argument instanceof SdkAutoCloseable) {
            ((SdkAutoCloseable) argument).close();
        }
    }

    static final class BuildOutcome {
        private final boolean skip;
        private final String reason;
        private final Object value;

        BuildOutcome(boolean skip, String reason, Object value) {
            this.skip = skip;
            this.reason = reason;
            this.value = value;
        }

        public boolean skip() {
            return skip;
        }

        public String reason() {
            return reason;
        }

        public Object value() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            BuildOutcome that = (BuildOutcome) o;
            return skip == that.skip
                    && Objects.equals(reason, that.reason)
                    && Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(skip);
            result = 31 * result + Objects.hashCode(reason);
            result = 31 * result + Objects.hashCode(value);
            return result;
        }

        @Override
        public String toString() {
            return "BuildOutcome[skip=" + skip + ", reason=" + reason + ", value=" + value + "]";
        }

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
