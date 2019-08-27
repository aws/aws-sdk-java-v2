package software.amazon.awssdk.benchmark.apicall.httpclient.async;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import software.amazon.awssdk.benchmark.apicall.httpclient.SdkHttpClientBenchmark;
import software.amazon.awssdk.benchmark.utils.MockServer;
import software.amazon.awssdk.crt.io.ClientBootstrap;
import software.amazon.awssdk.crt.io.SocketOptions;
import software.amazon.awssdk.crt.io.TlsContext;
import software.amazon.awssdk.crt.io.TlsContextOptions;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static software.amazon.awssdk.benchmark.utils.BenchmarkConstant.CONCURRENT_CALLS;
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.awaitCountdownLatchUninterruptibly;
import static software.amazon.awssdk.benchmark.utils.BenchmarkUtils.countDownUponCompletion;

/**
 * Using aws-crt-client to test against local mock https server.
 */
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 15, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2) // To reduce difference between each run
@BenchmarkMode(Mode.Throughput)
public class AwsCrtClientBenchmark implements SdkHttpClientBenchmark {

    private MockServer mockServer;
    private SdkAsyncHttpClient sdkHttpClient;
    private ProtocolRestJsonAsyncClient client;
    private TlsContextOptions tlsOptions;
    private TlsContext tlsContext;
    private SocketOptions socketOptions;
    private ClientBootstrap bootstrap;

    @Setup(Level.Trial)
    public void setup() throws Exception {
        mockServer = new MockServer();
        mockServer.start();

        int numCores = Runtime.getRuntime().availableProcessors();
        bootstrap = new ClientBootstrap(numCores);
        socketOptions = new SocketOptions();

        tlsOptions = new TlsContextOptions();
        tlsOptions.setVerifyPeer(false);
        tlsContext = new TlsContext(tlsOptions);

        sdkHttpClient = AwsCrtAsyncHttpClient.builder()
                .bootstrap(bootstrap)
                .socketOptions(socketOptions)
                .tlsContext(tlsContext)
                .build();

        client = ProtocolRestJsonAsyncClient.builder()
                .endpointOverride(mockServer.getHttpsUri())
                .httpClient(sdkHttpClient)
                .build();

        // Making sure the request actually succeeds
        client.allTypes().join();
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception {
        mockServer.stop();
        client.close();
        sdkHttpClient.close();
        tlsContext.close();
        tlsOptions.close();
        socketOptions.close();
        bootstrap.close();
    }

    @Override
    @Benchmark
    @OperationsPerInvocation(CONCURRENT_CALLS)
    public void concurrentApiCall(Blackhole blackhole) {
        CountDownLatch countDownLatch = new CountDownLatch(CONCURRENT_CALLS);
        for (int i = 0; i < CONCURRENT_CALLS; i++) {
            countDownUponCompletion(blackhole, client.allTypes(), countDownLatch);
        }

        awaitCountdownLatchUninterruptibly(countDownLatch, 10, TimeUnit.SECONDS);

    }

    @Override
    @Benchmark
    public void sequentialApiCall(Blackhole blackhole) {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownUponCompletion(blackhole, client.allTypes(), countDownLatch);
        awaitCountdownLatchUninterruptibly(countDownLatch, 1, TimeUnit.SECONDS);
    }

    public static void main(String... args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(AwsCrtClientBenchmark.class.getSimpleName())
                .addProfiler(StackProfiler.class)
                .build();
        Collection<RunResult> run = new Runner(opt).run();
    }
}
