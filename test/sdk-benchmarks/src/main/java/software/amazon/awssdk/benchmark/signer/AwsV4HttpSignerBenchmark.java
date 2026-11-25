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

import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.REGION_NAME;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.spi.signer.HttpSigner.SIGNING_CLOCK;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

/**
 * Measures {@link AwsV4HttpSigner#sign} end to end on a DynamoDB {@code PutItem}-shaped request: a JSON 1.0 POST with
 * a body of about 500 bytes, static credentials, and header-based signing with payload signing enabled.
 *
 * <p>{@link Sigv4SignerBenchmark} measures only the {@code V4RequestSigner} component. This benchmark also covers the
 * payload checksum, the request builder round trip, and signing-key lookup, which is what a service call pays.
 */
@State(Scope.Thread)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Fork(2)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class AwsV4HttpSignerBenchmark {
    private static final String PUT_ITEM_BODY =
        "{\"TableName\":\"benchmark-table\",\"Item\":{"
        + "\"pk\":{\"S\":\"benchmark-key\"},"
        + "\"sk\":{\"N\":\"100\"},"
        + "\"stringField\":{\"S\":\"test-value\"},"
        + "\"numberField\":{\"N\":\"123.456\"},"
        + "\"binaryField\":{\"B\":\"aGVsbG8gd29ybGQ=\"},"
        + "\"stringSetField\":{\"SS\":[\"value1\",\"value2\",\"value3\"]},"
        + "\"numberSetField\":{\"NS\":[\"1.1\",\"2.2\",\"3.3\"]},"
        + "\"boolField\":{\"BOOL\":false},"
        + "\"nullField\":{\"NULL\":true},"
        + "\"mapField\":{\"M\":{"
        + "\"nested\":{\"S\":\"nested-value\"},"
        + "\"deepNested\":{\"M\":{\"level2\":{\"N\":\"999\"}}}}},"
        + "\"listField\":{\"L\":["
        + "{\"S\":\"item1\"},{\"N\":\"42\"},{\"BOOL\":true},{\"NULL\":true}]}"
        + "}}";

    private AwsV4HttpSigner signer;
    private SignRequest<AwsCredentialsIdentity> putItemRequest;

    @Setup
    public void setup() {
        signer = AwsV4HttpSigner.create();
        byte[] body = PUT_ITEM_BODY.getBytes(StandardCharsets.UTF_8);
        SdkHttpRequest httpRequest =
            SdkHttpRequest.builder()
                          .method(SdkHttpMethod.POST)
                          .uri(URI.create("https://dynamodb.us-east-1.amazonaws.com/"))
                          .putHeader("Content-Type", "application/x-amz-json-1.0")
                          .putHeader("X-Amz-Target", "DynamoDB_20120810.PutItem")
                          .putHeader("Content-Length", Integer.toString(body.length))
                          .build();
        putItemRequest =
            SignRequest.builder(AwsCredentialsIdentity.create("akid", "skid"))
                       .request(httpRequest)
                       .payload(ContentStreamProvider.fromByteArrayUnsafe(body))
                       .putProperty(REGION_NAME, "us-east-1")
                       .putProperty(SERVICE_SIGNING_NAME, "dynamodb")
                       .putProperty(SIGNING_CLOCK, Clock.systemUTC())
                       .build();
    }

    @Benchmark
    public SignedRequest sign() {
        return signer.sign(putItemRequest);
    }
}
