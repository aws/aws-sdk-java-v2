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

package software.amazon.awssdk.http.apache5;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import software.amazon.awssdk.http.ConnectionPoolShutdownException;
import software.amazon.awssdk.http.ConnectionPoolShutdownException.Reason;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache5.internal.conn.RecreatingHttpClientConnectionManager;
import software.amazon.awssdk.http.apache5.internal.conn.RecreatingHttpClientConnectionManagerInspector;
import software.amazon.awssdk.utils.IoUtils;

/**
 * Connection pool recovery through the real Apache stack: Apache shuts its connection pool down when a
 * {@link java.lang.Error} escapes a request, and whether and how the client recovers depends on
 * {@link Apache5HttpClient.Builder#connectionPoolRecoveryMode(ConnectionPoolRecoveryMode)}.
 *
 * <p>The Error is raised from the request body, because Apache writes the body inside {@code MainClientExec.execute}, in
 * the same try block whose {@code catch (Error)} closes the connection manager. That is the code path a real
 * {@code OutOfMemoryError} takes when it lands on a request thread.
 */
public class Apache5HttpClientConnectionPoolRecoveryTest {

    /** The parent of this module's loggers. Apache's own loggers, and WireMock's, are outside it. */
    private static final String MODULE_LOGGER = "software.amazon.awssdk.http.apache5";
    private static final String POOL_SHUT_DOWN = "Connection pool shut down";
    private static final String RECOVERY_OPTION_PREFIX = "connectionPoolRecovery";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort())
                                                         .build();

    private final List<SdkHttpClient> clients = new ArrayList<>();
    private final List<ExecutorService> executors = new ArrayList<>();
    /** A fake clock for the recovery backoff. It only moves when a test moves it. */
    private final AtomicLong now = new AtomicLong(1_000_000_000L);

    @BeforeEach
    public void setup() {
        wireMock.stubFor(any(urlMatching(".*")).willReturn(aResponse().withStatus(200).withBody("ok")));
    }

    @AfterEach
    public void teardown() {
        executors.forEach(ExecutorService::shutdownNow);
        clients.forEach(SdkHttpClient::close);
    }

    /**
     * The two ways an Error reaches Apache in these tests. A real {@link StackOverflowError} is raised by unbounded
     * recursion in the request body stream; an {@link OutOfMemoryError} is simulated, because a real one would destabilize
     * the test JVM.
     */
    enum ErrorTrigger {
        SIMULATED_OUT_OF_MEMORY {
            @Override
            int read() {
                throw simulatedOutOfMemory;
            }

            @Override
            void assertRaised(Throwable thrown) {
                assertThat(thrown).isSameAs(simulatedOutOfMemory);
            }
        },
        REAL_STACK_OVERFLOW {
            @Override
            int read() {
                return recurse(0);
            }

            @Override
            void assertRaised(Throwable thrown) {
                assertThat(thrown).isExactlyInstanceOf(StackOverflowError.class);
            }
        };

        final OutOfMemoryError simulatedOutOfMemory =
            new OutOfMemoryError("Simulated heap exhaustion while writing the request body");

        /** Raises this trigger's Error. */
        abstract int read();

        /** Asserts that the Error the caller received is the one this trigger raised. */
        abstract void assertRaised(Throwable thrown);

        private static int recurse(int depth) {
            return recurse(depth + 1) + 1;
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // DISABLED (the default)
    // ---------------------------------------------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_disabledByDefault_poolStaysShutDownAfterError(ErrorTrigger trigger) throws Exception {
        SdkHttpClient client = track(Apache5HttpClient.builder().build());

        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);
        trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));

        assertTyped(catchThrowable(() -> execute(client, request())), Reason.SHUT_DOWN_AFTER_ERROR);
        // Nothing rebuilds the pool.
        assertTyped(catchThrowable(() -> execute(client, request())), Reason.SHUT_DOWN_AFTER_ERROR);
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_disabled_errorSurfacesUnchanged(ErrorTrigger trigger) {
        SdkHttpClient client = track(Apache5HttpClient.builder().build());

        // Equivalence guard: also passes without this change, by design.
        trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value = ConnectionPoolRecoveryMode.class, names = "DISABLED")
    public void connectionPoolRecoveryMode_nullOrDisabled_behavesAsDefault(ConnectionPoolRecoveryMode mode) {
        SdkHttpClient client = track(Apache5HttpClient.builder().connectionPoolRecoveryMode(mode).build());

        ErrorTrigger.SIMULATED_OUT_OF_MEMORY.assertRaised(
            catchThrowable(() -> execute(client, errorRequest(ErrorTrigger.SIMULATED_OUT_OF_MEMORY))));

        assertTyped(catchThrowable(() -> execute(client, request())), Reason.SHUT_DOWN_AFTER_ERROR);
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_disabled_logsNothing(ErrorTrigger trigger) {
        try (ModuleLogCaptor logCaptor = ModuleLogCaptor.capture(Level.WARN)) {
            SdkHttpClient client = track(Apache5HttpClient.builder().build());

            trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));
            catchThrowable(() -> execute(client, request()));
            catchThrowable(() -> execute(client, request()));

            assertThat(moduleWarnings(logCaptor)).isEmpty();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // WITH_BACKOFF
    // ---------------------------------------------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_withBackoff_nextRequestSucceeds(ErrorTrigger trigger) throws Exception {
        // The real clock: the first recovery is immediate, however recently the client was built.
        SdkHttpClient client = track(Apache5HttpClient.builder()
                                                      .connectionPoolRecoveryMode(ConnectionPoolRecoveryMode.WITH_BACKOFF)
                                                      .build());

        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);
        trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));

        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_withBackoff_errorInsideBackoff_failsFastWithRecoveryBackoff(ErrorTrigger trigger)
            throws Exception {
        SdkHttpClient client = track(Apache5HttpClient.buildWithRecoveryClock(
            Apache5HttpClient.builder()
                            .connectionPoolRecoveryMode(ConnectionPoolRecoveryMode.WITH_BACKOFF)
                            .connectionAcquisitionTimeout(Duration.ofSeconds(30)),
            now::get));

        enterRecoveryBackoff(client, trigger);

        int requestsBefore = wireMock.getAllServeEvents().size();
        for (int i = 0; i < 3; i++) {
            long start = System.nanoTime();
            assertTyped(catchThrowable(() -> execute(client, request())), Reason.RECOVERY_BACKOFF);
            // Fails fast: no wait for the 30 second connection acquisition timeout.
            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
        }
        // And without reaching the network.
        assertThat(wireMock.getAllServeEvents()).hasSize(requestsBefore);
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_withBackoff_backoffElapsed_rebuildsAndStartsNewWindow(ErrorTrigger trigger) throws Exception {
        SdkHttpClient client = recoveringClientWithFakeClock(ConnectionPoolRecoveryMode.WITH_BACKOFF);
        enterRecoveryBackoff(client, trigger);

        now.addAndGet(TimeUnit.MINUTES.toNanos(5));
        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);

        // That rebuild started a new window.
        trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));
        assertTyped(catchThrowable(() -> execute(client, request())), Reason.RECOVERY_BACKOFF);
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_withBackoff_logsOneWarnPerRecoveryAndPerBackoffEntry(ErrorTrigger trigger) throws Exception {
        try (ModuleLogCaptor logCaptor = ModuleLogCaptor.capture(Level.WARN)) {
            SdkHttpClient client = recoveringClientWithFakeClock(ConnectionPoolRecoveryMode.WITH_BACKOFF);

            enterRecoveryBackoff(client, trigger);
            for (int i = 0; i < 3; i++) {
                assertTyped(catchThrowable(() -> execute(client, request())), Reason.RECOVERY_BACKOFF);
            }
            now.addAndGet(TimeUnit.MINUTES.toNanos(5));
            assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);

            List<LogEvent> warnings = moduleWarnings(logCaptor);
            List<String> messages = messages(warnings);
            assertThat(messages).hasSize(3);
            assertThat(messages).filteredOn(m -> m.contains("Rebuilt the Apache HTTP client's connection pool"))
                                .hasSize(2)
                                .anyMatch(m -> m.contains("recoveryNumber=1"))
                                .anyMatch(m -> m.contains("recoveryNumber=2"));
            assertThat(messages).filteredOn(m -> m.contains("will not be rebuilt until")).hasSize(1);
            // The typed exceptions reach the caller, so they are not logged.
            assertThat(warnings).noneMatch(e -> e.getThrown() instanceof ConnectionPoolShutdownException);
        }
    }

    /**
     * Errors usually hit several threads at once. A request that was still running on the destroyed pool, and whose own
     * Error arrives after the pool was rebuilt, must not destroy the healthy replacement, which would start a backoff
     * outage right after a successful recovery.
     */
    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_withBackoff_errorStraddlingRebuild_doesNotShutDownReplacementPool(ErrorTrigger trigger)
            throws Exception {
        try (ModuleLogCaptor logCaptor = ModuleLogCaptor.capture(Level.WARN)) {
            SdkHttpClient client = recoveringClientWithFakeClock(ConnectionPoolRecoveryMode.WITH_BACKOFF);

            runErrorStraddlingRebuild(client, trigger);

            RecreatingHttpClientConnectionManager connectionManager = recreatingConnectionManager(client);
            assertThat(RecreatingHttpClientConnectionManagerInspector.recreationCount(connectionManager)).isEqualTo(1);
            assertThat(connectionManager.inRecoveryBackoff()).isFalse();
            List<String> warnings = messages(moduleWarnings(logCaptor));
            assertThat(warnings).hasSize(1);
            assertThat(warnings.get(0)).contains("Rebuilt the Apache HTTP client's connection pool")
                                       .contains("recoveryNumber=1");
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // ALWAYS
    // ---------------------------------------------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_always_errorAfterRebuild_rebuildsAgainImmediately(ErrorTrigger trigger) throws Exception {
        // The clock never moves, so with a backoff every Error after the first would leave the pool down.
        SdkHttpClient client = recoveringClientWithFakeClock(ConnectionPoolRecoveryMode.ALWAYS);
        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);

        for (int i = 0; i < 3; i++) {
            trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));
            assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);
        }
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_always_logsRecoveryWarningAtMostOncePerFiveMinutes(ErrorTrigger trigger) throws Exception {
        try (ModuleLogCaptor logCaptor = ModuleLogCaptor.capture(Level.DEBUG)) {
            SdkHttpClient client = recoveringClientWithFakeClock(ConnectionPoolRecoveryMode.ALWAYS);

            for (int i = 0; i < 3; i++) {
                trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));
                assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);
            }

            List<String> warnings = messages(moduleWarnings(logCaptor));
            assertThat(warnings).hasSize(1);
            assertThat(warnings.get(0)).contains("Rebuilt the Apache HTTP client's connection pool")
                                       .contains("recoveryNumber=1, rebuildsSincePreviousWarning=1");
            List<String> debugRebuilds = messages(moduleEvents(logCaptor, Level.DEBUG))
                .stream()
                .filter(m -> m.startsWith("Rebuilt the Apache HTTP client's connection pool after"))
                .collect(Collectors.toList());
            assertThat(debugRebuilds).hasSize(2);
            assertThat(debugRebuilds.get(0)).contains("recoveryNumber=2");
            assertThat(debugRebuilds.get(1)).contains("recoveryNumber=3");

            now.addAndGet(TimeUnit.MINUTES.toNanos(5));
            trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));
            assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);

            warnings = messages(moduleWarnings(logCaptor));
            assertThat(warnings).hasSize(2);
            assertThat(warnings.get(1)).contains("recoveryNumber=4, rebuildsSincePreviousWarning=3");
            List<LogEvent> all = moduleEvents(logCaptor, Level.DEBUG);
            assertThat(messages(all)).noneMatch(m -> m.contains("will not be rebuilt"));
            // The typed exceptions reach the caller, so they are not logged.
            assertThat(all).noneMatch(e -> e.getThrown() instanceof ConnectionPoolShutdownException);
        }
    }

    /**
     * The {@code WITH_BACKOFF} straddle case in {@code ALWAYS} mode. A destroyed replacement would simply be rebuilt
     * again here, so the rebuild count is what shows it was not destroyed.
     */
    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_always_errorStraddlingRebuild_doesNotShutDownReplacementPool(ErrorTrigger trigger)
            throws Exception {
        SdkHttpClient client = recoveringClientWithFakeClock(ConnectionPoolRecoveryMode.ALWAYS);

        runErrorStraddlingRebuild(client, trigger);

        RecreatingHttpClientConnectionManager connectionManager = recreatingConnectionManager(client);
        assertThat(RecreatingHttpClientConnectionManagerInspector.recreationCount(connectionManager)).isEqualTo(1);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // close()
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void close_disabled_subsequentRequestThrowsClientClosed() throws Exception {
        SdkHttpClient client = Apache5HttpClient.builder().build();
        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);

        client.close();

        assertTyped(catchThrowable(() -> execute(client, request())), Reason.CLIENT_CLOSED);
    }

    @Test
    public void close_withBackoff_subsequentRequestThrowsClientClosed() throws Exception {
        assertCloseThenClientClosed(recoveringClientWithFakeClock(ConnectionPoolRecoveryMode.WITH_BACKOFF));
    }

    @Test
    public void close_always_subsequentRequestThrowsClientClosed() throws Exception {
        assertCloseThenClientClosed(recoveringClientWithFakeClock(ConnectionPoolRecoveryMode.ALWAYS));
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void close_withBackoff_afterError_neverResurrects(ErrorTrigger trigger) {
        assertErrorThenCloseNeverResurrects(recoveringClientWithFakeClock(ConnectionPoolRecoveryMode.WITH_BACKOFF),
                                            trigger);
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void close_always_afterError_neverResurrects(ErrorTrigger trigger) {
        assertErrorThenCloseNeverResurrects(recoveringClientWithFakeClock(ConnectionPoolRecoveryMode.ALWAYS), trigger);
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void close_disabled_afterError_reportsClientClosed(ErrorTrigger trigger) {
        SdkHttpClient client = Apache5HttpClient.builder().build();
        trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));

        client.close();

        assertTyped(catchThrowable(() -> execute(client, request())), Reason.CLIENT_CLOSED);
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void close_duringRecoveryBackoff_reportsClientClosedAndNeverRebuilds(ErrorTrigger trigger) throws Exception {
        SdkHttpClient client = recoveringClientWithFakeClock(ConnectionPoolRecoveryMode.WITH_BACKOFF);
        enterRecoveryBackoff(client, trigger);

        client.close();

        assertTyped(catchThrowable(() -> execute(client, request())), Reason.CLIENT_CLOSED);
        now.addAndGet(TimeUnit.MINUTES.toNanos(10));
        assertTyped(catchThrowable(() -> execute(client, request())), Reason.CLIENT_CLOSED);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Builder
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void build_beanSetter_setsRecoveryMode() throws Exception {
        Apache5HttpClient.Builder builder = Apache5HttpClient.builder();
        // DefaultBuilder is private, so its bean setter is reached the way an Introspector-based framework reaches it.
        Method setter = builder.getClass().getMethod("setConnectionPoolRecoveryMode", ConnectionPoolRecoveryMode.class);
        setter.setAccessible(true);
        setter.invoke(builder, ConnectionPoolRecoveryMode.WITH_BACKOFF);
        SdkHttpClient client = track(builder.build());

        ErrorTrigger.SIMULATED_OUT_OF_MEMORY.assertRaised(
            catchThrowable(() -> execute(client, errorRequest(ErrorTrigger.SIMULATED_OUT_OF_MEMORY))));
        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);

        PropertyDescriptor[] properties = Introspector.getBeanInfo(builder.getClass()).getPropertyDescriptors();
        assertThat(Arrays.stream(properties)
                         .filter(p -> p.getName().startsWith(RECOVERY_OPTION_PREFIX))
                         .collect(Collectors.toList()))
            .singleElement()
            .satisfies(p -> {
                assertThat(p.getName()).isEqualTo("connectionPoolRecoveryMode");
                assertThat(p.getWriteMethod()).isNotNull();
            });
        assertThat(Arrays.stream(Apache5HttpClient.Builder.class.getMethods())
                         .map(Method::getName)
                         .filter(name -> name.startsWith(RECOVERY_OPTION_PREFIX)))
            .containsExactly("connectionPoolRecoveryMode");
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

    private SdkHttpClient recoveringClientWithFakeClock(ConnectionPoolRecoveryMode mode) {
        return track(Apache5HttpClient.buildWithRecoveryClock(
            Apache5HttpClient.builder().connectionPoolRecoveryMode(mode), now::get));
    }

    /** Error, recovery, then a second Error inside the frozen window. */
    private void enterRecoveryBackoff(SdkHttpClient client, ErrorTrigger trigger) throws IOException {
        trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));
        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);
        trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));
    }

    private void assertCloseThenClientClosed(SdkHttpClient client) throws IOException {
        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);

        client.close();

        assertTyped(catchThrowable(() -> execute(client, request())), Reason.CLIENT_CLOSED);
    }

    private void assertErrorThenCloseNeverResurrects(SdkHttpClient client, ErrorTrigger trigger) {
        trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));

        client.close();

        assertTyped(catchThrowable(() -> execute(client, request())), Reason.CLIENT_CLOSED);
        now.addAndGet(TimeUnit.MINUTES.toNanos(10));
        assertTyped(catchThrowable(() -> execute(client, request())), Reason.CLIENT_CLOSED);
    }

    /**
     * Request B leases from the first pool and blocks, A's Error destroys that pool, a third request rebuilds it, and only
     * then does B raise its Error. Afterwards two more requests must succeed.
     */
    private void runErrorStraddlingRebuild(SdkHttpClient client, ErrorTrigger trigger) throws Exception {
        ExecutorService threadA = newExecutor();
        ExecutorService threadB = newExecutor();
        CountDownLatch bStarted = new CountDownLatch(1);
        CountDownLatch releaseB = new CountDownLatch(1);
        SdkHttpFullRequest blockingErrorRequest = request().toBuilder().contentStreamProvider(() -> new InputStream() {
            @Override
            public int read() {
                bStarted.countDown();
                try {
                    releaseB.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return trigger.read();
            }
        }).build();

        // B leases from the first pool and blocks while writing its body.
        Future<Throwable> b = threadB.submit(() -> catchThrowable(() -> execute(client, blockingErrorRequest)));
        assertThat(bStarted.await(10, TimeUnit.SECONDS)).isTrue();

        // A's Error destroys the first pool.
        ErrorTrigger simulated = ErrorTrigger.SIMULATED_OUT_OF_MEMORY;
        Throwable a = threadA.submit(() -> catchThrowable(() -> execute(client, errorRequest(simulated))))
                             .get(10, TimeUnit.SECONDS);
        simulated.assertRaised(a);

        // The next request rebuilds it.
        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);

        // B's Error now arrives, from a request on the replaced pool.
        releaseB.countDown();
        trigger.assertRaised(b.get(10, TimeUnit.SECONDS));

        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);
        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);
    }

    /**
     * The typed exception, checked against the compatibility contract too: still an {@link IllegalStateException} with
     * exactly Apache's message, and Apache's own exception as the cause.
     */
    private static void assertTyped(Throwable thrown, Reason reason) {
        assertThat(thrown).isInstanceOf(IllegalStateException.class).hasMessage(POOL_SHUT_DOWN);
        assertThat(thrown).isInstanceOf(ConnectionPoolShutdownException.class);
        assertThat(((ConnectionPoolShutdownException) thrown).reason()).isEqualTo(reason);
        assertThat(thrown.getCause()).isExactlyInstanceOf(IllegalStateException.class).hasMessage(POOL_SHUT_DOWN);
    }

    private SdkHttpFullRequest request() {
        URI uri = URI.create("http://localhost:" + wireMock.getPort());
        byte[] content = "Body".getBytes(StandardCharsets.UTF_8);
        return SdkHttpFullRequest.builder()
                                 .uri(uri)
                                 .method(SdkHttpMethod.POST)
                                 .putHeader("Host", uri.getHost())
                                 .putHeader("Content-Length", Integer.toString(content.length))
                                 .contentStreamProvider(() -> new ByteArrayInputStream(content))
                                 .build();
    }

    private SdkHttpFullRequest errorRequest(ErrorTrigger trigger) {
        return request().toBuilder()
                        .contentStreamProvider(() -> new InputStream() {
                            @Override
                            public int read() {
                                return trigger.read();
                            }
                        })
                        .build();
    }

    private static HttpExecuteResponse execute(SdkHttpClient client, SdkHttpFullRequest request) throws IOException {
        HttpExecuteResponse response =
            client.prepareRequest(HttpExecuteRequest.builder()
                                                    .request(request)
                                                    .contentStreamProvider(request.contentStreamProvider().orElse(null))
                                                    .build())
                  .call();
        response.responseBody().ifPresent(IoUtils::drainInputStream);
        return response;
    }

    private SdkHttpClient track(SdkHttpClient client) {
        clients.add(client);
        return client;
    }

    private ExecutorService newExecutor() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executors.add(executor);
        return executor;
    }

    private static RecreatingHttpClientConnectionManager recreatingConnectionManager(SdkHttpClient client) {
        return (RecreatingHttpClientConnectionManager) ((Apache5HttpClient) client).connectionManager();
    }

    private static List<LogEvent> moduleWarnings(ModuleLogCaptor logCaptor) {
        return moduleEvents(logCaptor, Level.WARN);
    }

    /** Events from this module's loggers at {@code minimumLevel} or above. */
    private static List<LogEvent> moduleEvents(ModuleLogCaptor logCaptor, Level minimumLevel) {
        return logCaptor.loggedEvents()
                        .stream()
                        .filter(e -> e.getLevel().isMoreSpecificThan(minimumLevel))
                        .collect(Collectors.toList());
    }

    private static List<String> messages(List<LogEvent> events) {
        return events.stream().map(e -> e.getMessage().getFormattedMessage()).collect(Collectors.toList());
    }

    /**
     * Captures the events of this module's loggers, and nothing else.
     *
     * <p>{@link software.amazon.awssdk.testutils.LogCaptor} captures the root logger, so WireMock's Jetty threads log into
     * it concurrently with the test. That loses events from its unsynchronized list, and a Jetty thread that logs while
     * the captor is being stopped gets an exception that kills WireMock's selector, after which every later request in the
     * class times out. Only the client's own threads log to this module's loggers, the list is thread-safe, and appending
     * to a stopped captor is ignored rather than thrown.
     */
    private static final class ModuleLogCaptor extends AbstractAppender implements AutoCloseable {
        private final List<LogEvent> loggedEvents = new CopyOnWriteArrayList<>();

        private ModuleLogCaptor() {
            super(ModuleLogCaptor.class.getName(), null, null, /* ignoreExceptions */ true, Property.EMPTY_ARRAY);
        }

        /** Starts capturing this module's events at {@code level} or above. Close the captor to stop. */
        static ModuleLogCaptor capture(Level level) {
            ModuleLogCaptor captor = new ModuleLogCaptor();
            captor.start();
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration configuration = context.getConfiguration();
            assertThat(configuration.getLoggers()).doesNotContainKey(MODULE_LOGGER);
            LoggerConfig loggerConfig = new LoggerConfig(MODULE_LOGGER, level, /* additive */ true);
            loggerConfig.addAppender(captor, null, null);
            configuration.addLogger(MODULE_LOGGER, loggerConfig);
            context.updateLoggers();
            return captor;
        }

        List<LogEvent> loggedEvents() {
            return new ArrayList<>(loggedEvents);
        }

        @Override
        public void append(LogEvent event) {
            loggedEvents.add(event.toImmutable());
        }

        @Override
        public void close() {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            context.getConfiguration().removeLogger(MODULE_LOGGER);
            context.updateLoggers();
            stop();
        }
    }
}
