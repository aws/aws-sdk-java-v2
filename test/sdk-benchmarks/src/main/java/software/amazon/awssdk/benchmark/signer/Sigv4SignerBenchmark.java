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

package software.amazon.awssdk.benchmark.signer;

import java.net.URI;
import java.time.Clock;
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
import org.openjdk.jmh.infra.Blackhole;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.http.auth.aws.internal.signer.V4Properties;
import software.amazon.awssdk.http.auth.aws.internal.signer.V4RequestSigner;
import software.amazon.awssdk.http.auth.aws.internal.signer.V4RequestSigningResult;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;


@State(Scope.Thread)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class Sigv4SignerBenchmark {
    SdkHttpRequest.Builder request;
    V4RequestSigner siger;

    // Setup runs once per trial or iteration (not measured)
    @Setup(Level.Iteration)
    public void setup() {
        URI target = URI.create("https://test.com/");
        request = SdkHttpRequest.builder()
                             .method(SdkHttpMethod.GET)
                             .uri(target)
                             .encodedPath(target.getPath())
                             .putHeader("x-amz-content-sha256", "checksum")
                             .putHeader("x-amz-archive-description", "test  test");
        AwsCredentialsIdentity creds =
            AwsCredentialsIdentity.create("access", "secret");
        Clock clock = Clock.systemUTC();
        V4Properties properties = V4Properties.builder()
                           .credentials(creds)
                           .credentialScope(new CredentialScope("us-east-1", "demo", clock.instant()))
                           .signingClock(clock)
                           .doubleUrlEncode(true)
                           .normalizePath(true)
                           .build();

        siger = V4RequestSigner.create(properties, "abc123");
    }

    @Benchmark
    public void benchmarkSign(Blackhole blackhole) {
        V4RequestSigningResult signingResult = siger.sign(request);
        blackhole.consume(signingResult);
    }
}