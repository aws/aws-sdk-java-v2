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

package software.amazon.awssdk.benchmark.endpoint;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.endpoints.EndpointUrl;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;

/**
 * JMH benchmark comparing endpoint resolution performance between the old {@link URI}-based
 * approach and the new {@link EndpointUrl}-based approach.
 *
 * <p>The primary optimization is replacing {@code URI.create()} with {@code EndpointUrl.parse()}
 * on the per-request hot path. {@code URI.create()} performs extensive RFC 2396 validation and
 * parsing that is unnecessary when the endpoint rules engine guarantees valid URLs.</p>
 *
 * <p>A further optimization uses {@code EndpointUrl.of()} with pre-split components, simulating
 * what the codegen-time pre-parsing achieves: URL components are known at compile time and
 * bypass even the lightweight string parsing of {@code EndpointUrl.parse()}.</p>
 *
 * <p>Run with: {@code java -jar target/benchmarks.jar EndpointCreateBenchmark}</p>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(2)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@State(Scope.Benchmark)
public class EndpointCreateBenchmark {

    @Param({
        "https://s3.us-east-1.amazonaws.com",
        "https://mybucket.s3.us-west-2.amazonaws.com/prefix/key",
        "https://account-id.s3-control.us-east-1.amazonaws.com"
    })
    private String url;

    private URI clientEndpoint;
    private SdkHttpRequest baseRequest;

    // Pre-split components simulating codegen-time pre-parsing (components known at compile time).
    // These are extracted once in setup, then the benchmark methods use them directly via EndpointUrl.of().
    private String preSplitScheme;
    private String preSplitHost;
    private int preSplitPort;
    private String preSplitEncodedPath;

    @Setup
    public void setup() {
        clientEndpoint = URI.create("https://s3.us-east-1.amazonaws.com");
        baseRequest = SdkHttpRequest.builder()
                                    .uri(clientEndpoint)
                                    .method(SdkHttpMethod.GET)
                                    .build();

        // Parse once to extract components, simulating what codegen does at build time.
        EndpointUrl parsed = EndpointUrl.parse(url);
        preSplitScheme = parsed.scheme();
        preSplitHost = parsed.host();
        preSplitPort = parsed.port();
        preSplitEncodedPath = parsed.encodedPath();
    }

    // --- Isolated parse cost ---

    @Benchmark
    public URI baseline_uriCreate() {
        return URI.create(url);
    }

    @Benchmark
    public EndpointUrl optimized_endpointUrlParse() {
        return EndpointUrl.parse(url);
    }

    @Benchmark
    public EndpointUrl optimized_endpointUrlOf() {
        return EndpointUrl.of(preSplitScheme, preSplitHost, preSplitPort, preSplitEncodedPath);
    }

    // --- Full path: parse + Endpoint construction + request building ---

    @Benchmark
    public SdkHttpRequest baseline_fullSetUriWithUri() {
        URI uri = URI.create(url);
        Endpoint endpoint = Endpoint.builder().url(uri).build();
        URI resolvedUri = endpoint.url();
        return baseRequest.toBuilder()
                          .protocol(resolvedUri.getScheme())
                          .host(resolvedUri.getHost())
                          .port(resolvedUri.getPort())
                          .encodedPath(resolvedUri.getRawPath())
                          .build();
    }

    @Benchmark
    public SdkHttpRequest optimized_fullSetUriWithEndpointUrl() {
        EndpointUrl endpointUrl = EndpointUrl.parse(url);
        Endpoint endpoint = Endpoint.builder().endpointUrl(endpointUrl).build();
        EndpointUrl resolvedUrl = endpoint.endpointUrl();
        return baseRequest.toBuilder()
                          .protocol(resolvedUrl.scheme())
                          .host(resolvedUrl.host())
                          .port(resolvedUrl.port())
                          .encodedPath(resolvedUrl.encodedPath())
                          .build();
    }

    @Benchmark
    public SdkHttpRequest optimized_fullSetUriWithEndpointUrlOf() {
        EndpointUrl endpointUrl = EndpointUrl.of(preSplitScheme, preSplitHost, preSplitPort, preSplitEncodedPath);
        Endpoint endpoint = Endpoint.builder().endpointUrl(endpointUrl).build();
        EndpointUrl resolvedUrl = endpoint.endpointUrl();
        return baseRequest.toBuilder()
                          .protocol(resolvedUrl.scheme())
                          .host(resolvedUrl.host())
                          .port(resolvedUrl.port())
                          .encodedPath(resolvedUrl.encodedPath())
                          .build();
    }
}
