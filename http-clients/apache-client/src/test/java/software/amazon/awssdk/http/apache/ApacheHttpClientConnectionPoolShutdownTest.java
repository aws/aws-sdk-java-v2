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

package software.amazon.awssdk.http.apache;

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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
import software.amazon.awssdk.http.ConnectionPoolShutdownException;
import software.amazon.awssdk.http.ConnectionPoolShutdownException.Reason;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.utils.IoUtils;

/**
 * A shut-down connection pool through the real Apache stack: Apache shuts its connection pool down when a
 * {@link java.lang.Error} escapes a request, and this client never rebuilds it, so every later request fails with
 * {@link ConnectionPoolShutdownException}.
 *
 * <p>The Error is raised from the request body, because Apache writes the body inside {@code MainClientExec.execute}, in
 * the same try block whose {@code catch (Error)} shuts the connection manager down. That is the code path a real
 * {@code OutOfMemoryError} takes when it lands on a request thread.
 */
public class ApacheHttpClientConnectionPoolShutdownTest {

    /** The parent of this module's loggers. Apache's own loggers, and WireMock's, are outside it. */
    private static final String MODULE_LOGGER = "software.amazon.awssdk.http.apache";
    private static final String POOL_SHUT_DOWN = "Connection pool shut down";
    private static final String RECOVERY_OPTION_PREFIX = "connectionPoolRecovery";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
                                                         .options(wireMockConfig().dynamicPort())
                                                         .build();

    private final List<SdkHttpClient> clients = new ArrayList<>();

    @BeforeEach
    public void setup() {
        wireMock.stubFor(any(urlMatching(".*")).willReturn(aResponse().withStatus(200).withBody("ok")));
    }

    @AfterEach
    public void teardown() {
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
    // After an Error
    // ---------------------------------------------------------------------------------------------------------------

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_afterError_nextRequestsThrowShutDownAfterError(ErrorTrigger trigger) throws Exception {
        SdkHttpClient client = track(ApacheHttpClient.builder().build());

        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);
        trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));

        assertTyped(catchThrowable(() -> execute(client, request())), Reason.SHUT_DOWN_AFTER_ERROR);
        // Nothing rebuilds the pool.
        assertTyped(catchThrowable(() -> execute(client, request())), Reason.SHUT_DOWN_AFTER_ERROR);
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_error_surfacesUnchanged(ErrorTrigger trigger) {
        SdkHttpClient client = track(ApacheHttpClient.builder().build());

        // Equivalence guard: also passes without this change, by design.
        trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void execute_afterError_logsNothing(ErrorTrigger trigger) {
        try (ModuleLogCaptor logCaptor = ModuleLogCaptor.capture(Level.WARN)) {
            SdkHttpClient client = track(ApacheHttpClient.builder().build());

            trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));
            catchThrowable(() -> execute(client, request()));
            catchThrowable(() -> execute(client, request()));

            // Equivalence guard: also passes without this change, by design.
            assertThat(moduleEvents(logCaptor)).isEmpty();
        }
    }

    // ---------------------------------------------------------------------------------------------------------------
    // close()
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void close_subsequentRequestThrowsClientClosed() throws Exception {
        SdkHttpClient client = ApacheHttpClient.builder().build();
        assertThat(execute(client, request()).httpResponse().statusCode()).isEqualTo(200);

        client.close();

        assertTyped(catchThrowable(() -> execute(client, request())), Reason.CLIENT_CLOSED);
    }

    @ParameterizedTest
    @EnumSource(ErrorTrigger.class)
    public void close_afterError_reportsClientClosed(ErrorTrigger trigger) {
        SdkHttpClient client = ApacheHttpClient.builder().build();
        trigger.assertRaised(catchThrowable(() -> execute(client, errorRequest(trigger))));

        client.close();

        assertTyped(catchThrowable(() -> execute(client, request())), Reason.CLIENT_CLOSED);
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Builder
    // ---------------------------------------------------------------------------------------------------------------

    @Test
    public void builder_hasNoConnectionPoolRecoveryOption() throws Exception {
        // Guard: this client does not support connection pool recovery, so it must not offer an option for it.
        ApacheHttpClient.Builder builder = ApacheHttpClient.builder();

        assertThat(Arrays.stream(ApacheHttpClient.Builder.class.getMethods()).map(Method::getName))
            .noneMatch(name -> name.startsWith(RECOVERY_OPTION_PREFIX));
        PropertyDescriptor[] properties = Introspector.getBeanInfo(builder.getClass()).getPropertyDescriptors();
        assertThat(Arrays.stream(properties).map(PropertyDescriptor::getName))
            .noneMatch(name -> name.startsWith(RECOVERY_OPTION_PREFIX));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------------------------

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

    private static List<LogEvent> moduleEvents(ModuleLogCaptor logCaptor) {
        return logCaptor.loggedEvents()
                        .stream()
                        .filter(e -> e.getLevel().isMoreSpecificThan(Level.WARN))
                        .collect(Collectors.toList());
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
