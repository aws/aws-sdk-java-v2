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

package software.amazon.awssdk.core.internal.crac;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.core.crac.RegisteredWarmUpProvider;
import software.amazon.awssdk.core.crac.SdkWarmUpProvider;
import software.amazon.awssdk.testutils.LogCaptor;

/**
 * Unit tests for {@link ClasspathWarmUpInvoker}. Most tests stub {@link WarmUpServiceLoader}; the "fails to load"
 * tests use a real {@link ServiceLoader} over a temporary {@code META-INF/services} file.
 */
class ClasspathWarmUpInvokerTest {

    @Test
    void invokeAll_invokesEveryProviderOnce() {
        CountingProvider first = new CountingProvider();
        CountingProvider second = new CountingProvider();
        CountingProvider third = new CountingProvider();

        invokerLoading(first, second, third).invokeAll();

        assertThat(first.invocations()).isEqualTo(1);
        assertThat(second.invocations()).isEqualTo(1);
        assertThat(third.invocations()).isEqualTo(1);
    }

    @Test
    void invokeAll_whenOneProviderThrows_stillInvokesOthers() {
        CountingProvider before = new CountingProvider();
        SdkWarmUpProvider throwing = () -> {
            throw new RuntimeException("boom");
        };
        CountingProvider after = new CountingProvider();

        WarmUpInvoker invoker = invokerLoading(before, throwing, after);

        assertThatCode(invoker::invokeAll).doesNotThrowAnyException();
        assertThat(before.invocations()).isEqualTo(1);
        assertThat(after.invocations()).isEqualTo(1);
    }

    @Test
    void invokeAll_whenProviderFailsToLoad_stillInvokesOthers(@TempDir Path tempDir) throws IOException {
        // Non-existent class then a real one: ServiceLoader must advance past the bad entry. It builds the
        // instance itself, so we read the static counter on RegisteredWarmUpProvider.
        RegisteredWarmUpProvider.INVOCATIONS.set(0);
        WarmUpInvoker invoker = invokerLoading(createAndLoadTempServicesFile(
            tempDir, "com.example.DoesNotExistProvider", RegisteredWarmUpProvider.class.getName()));

        assertThatCode(invoker::invokeAll).doesNotThrowAnyException();
        assertThat(RegisteredWarmUpProvider.INVOCATIONS.get()).isEqualTo(1);
    }

    @Test
    void invokeAll_whenNoProviders_isNoOp() {
        WarmUpInvoker invoker = invokerLoading(Collections.emptyIterator());
        assertThatCode(invoker::invokeAll).doesNotThrowAnyException();
    }

    @Test
    void invokeAll_whenProviderThrows_logsAtWarn() {
        SdkWarmUpProvider throwing = () -> {
            throw new RuntimeException("boom");
        };

        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            invokerLoading(throwing).invokeAll();

            assertThat(logCaptor.loggedEvents())
                .filteredOn(loggedFromInvoker())
                .anyMatch(event -> event.getLevel() == Level.WARN
                                   && event.getMessage().getFormattedMessage().contains("Warm-up failed for"));
        }
    }

    @Test
    void invokeAll_whenProviderFailsToLoad_logsAtWarn(@TempDir Path tempDir) throws IOException {
        WarmUpInvoker invoker = invokerLoading(createAndLoadTempServicesFile(tempDir, "com.example.DoesNotExistProvider"));

        try (LogCaptor logCaptor = LogCaptor.create(Level.WARN)) {
            invoker.invokeAll();

            assertThat(logCaptor.loggedEvents())
                .filteredOn(loggedFromInvoker())
                .anyMatch(event -> event.getLevel() == Level.WARN
                                   && event.getMessage().getFormattedMessage().contains("could not be loaded"));
        }
    }

    @Test
    void invokeAll_whenNoProviders_logsAtDebug() {
        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            invokerLoading(Collections.emptyIterator()).invokeAll();

            assertThat(logCaptor.loggedEvents())
                .filteredOn(loggedFromInvoker())
                .anyMatch(event -> event.getLevel() == Level.DEBUG
                                   && event.getMessage().getFormattedMessage().contains("No warm-up tasks"));
        }
    }

    private static Predicate<LogEvent> loggedFromInvoker() {
        return event -> WarmUpDiscovery.class.getName().equals(event.getLoggerName());
    }

    private WarmUpInvoker invokerLoading(SdkWarmUpProvider... providers) {
        return invokerLoading(Arrays.asList(providers).iterator());
    }

    private WarmUpInvoker invokerLoading(Iterator<SdkWarmUpProvider> providers) {
        WarmUpServiceLoader loader = new WarmUpServiceLoader() {
            @Override
            Iterator<SdkWarmUpProvider> loadProviders() {
                return providers;
            }
        };
        return new ClasspathWarmUpInvoker(loader);
    }

    // Creates a temp META-INF/services file with the given class names and loads it through a real ServiceLoader.
    private Iterator<SdkWarmUpProvider> createAndLoadTempServicesFile(Path tempDir, String... providerClassNames)
        throws IOException {
        Path servicesDir = tempDir.resolve("META-INF/services");
        Files.createDirectories(servicesDir);
        Path registration = servicesDir.resolve(SdkWarmUpProvider.class.getName());
        Files.write(registration, Arrays.asList(providerClassNames));

        URLClassLoader classLoader = new URLClassLoader(new URL[] {tempDir.toUri().toURL()},
                                                        getClass().getClassLoader());
        return ServiceLoader.load(SdkWarmUpProvider.class, classLoader).iterator();
    }

    private static final class CountingProvider implements SdkWarmUpProvider {
        private final AtomicInteger invocations = new AtomicInteger();

        @Override
        public void warmUp() {
            invocations.incrementAndGet();
        }

        int invocations() {
            return invocations.get();
        }
    }
}
