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

package software.amazon.awssdk.benchmark.coldstart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.core.SdkClient;
import software.amazon.awssdk.core.warmup.SdkWarmUp;
import software.amazon.awssdk.core.warmup.SdkWarmUpProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Measures how long {@link SdkWarmUp#warmUp} takes. The batched, sequential and no-arg methods cover the same scope
 * (every discovered {@link SdkWarmUpProvider}, sync and async), so their scores are directly comparable.
 *
 * <p>Scores include a real STS network call per HTTP client, so they vary by host: do not add them to
 * {@code baseline.json}.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 0)
@Measurement(iterations = 1)
@Fork(20)
public class V2SdkWarmUpExecutionTimeBenchmark {

    /** Sync and async client class per discovered provider, for the sequential (one call per service) method. */
    private List<Class<? extends SdkClient>[]> clientClassesByService;

    /** The same classes in one array, for the single batched {@code prime} call. */
    private Class<? extends SdkClient>[] allClientClasses;

    /**
     * Resolves the discovered providers' client classes so the timed methods pay only for {@code prime}. Fails rather
     * than skipping an unresolvable provider, which would silently narrow the batched/sequential scope.
     */
    @Setup(Level.Trial)
    @SuppressWarnings("unchecked")
    public void setup() {
        clientClassesByService = new ArrayList<>();
        List<Class<? extends SdkClient>> flattened = new ArrayList<>();

        for (SdkWarmUpProvider provider : ServiceLoader.load(SdkWarmUpProvider.class)) {
            Class<? extends SdkClient> syncClient = loadClientClass(provider.syncClientClassName());
            Class<? extends SdkClient> asyncClient = loadClientClass(provider.asyncClientClassName());
            clientClassesByService.add(new Class[] {syncClient, asyncClient});
            flattened.add(syncClient);
            flattened.add(asyncClient);
        }

        if (clientClassesByService.isEmpty()) {
            throw new IllegalStateException("No SdkWarmUpProvider found on the classpath; nothing to prime.");
        }
        allClientClasses = flattened.toArray(new Class[0]);
    }

    private Class<? extends SdkClient> loadClientClass(String className) {
        try {
            return Class.forName(className).asSubclass(SdkClient.class);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("A registered SdkWarmUpProvider names client class " + className
                                            + ", which is not on the classpath.", e);
        }
    }

    @Benchmark
    public void primeSingleService() throws Exception {
        SdkWarmUp.warmUp(DynamoDbClient.class);
    }

    /** One {@code prime} call naming every discovered client, so the HTTP client warm-up runs once. */
    @Benchmark
    public void primeAllServicesBatched() throws Exception {
        SdkWarmUp.warmUp(allClientClasses);
    }

    /** One {@code prime} call per discovered service, so the HTTP client warm-up runs once per service. */
    @Benchmark
    public void primeAllServicesSequentially() throws Exception {
        for (Class<? extends SdkClient>[] perService : clientClassesByService) {
            SdkWarmUp.warmUp(perService);
        }
    }

    /** The no-arg overload: same scope as the two methods above, reached through discovery inside {@code prime}. */
    @Benchmark
    public void primeAllRegisteredProviders() throws Exception {
        SdkWarmUp.warmUp();
    }

    public static void main(String... args) throws RunnerException, CommandLineOptionException {
        Options opt = new OptionsBuilder()
            .parent(new CommandLineOptions())
            .include(V2SdkWarmUpExecutionTimeBenchmark.class.getSimpleName())
            .addProfiler(StackProfiler.class)
            .build();
        Collection<RunResult> run = new Runner(opt).run();
    }
}
