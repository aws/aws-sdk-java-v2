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

package software.amazon.awssdk.services.s3.functionaltests;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

/**
 * End-to-end (through a real client over WireMock) verification that a concatenated (multi-member) gzip response body
 * with NO {@code Content-Encoding: gzip} header — i.e. the caller decodes it themselves — round-trips through both the
 * sync {@code toInputStream()} and async {@code toBlockingInputStream()} paths and decodes to ALL members.
 *
 * <p>This is a happy-path regression guard: it proves the {@code GzipAvailabilityInputStream} wrap the SDK now applies
 * does not corrupt, stall, or truncate a streaming gzip download over the real HTTP stack. It does NOT reproduce the
 * underlying truncation — WireMock delivers the whole body into the socket buffer, so {@code available()} is never
 * transiently {@code 0} at a member boundary. Deterministic reproduction of the boundary condition lives in the
 * lower-level stream and handler tests.
 */
@WireMockTest
public class GetObjectConcatenatedGzipTest {

    private static final String BUCKET = "Example-Bucket";
    private static final String KEY = "concatenated.gz";
    private static final String[] MEMBERS = {"member-0", "member-1", "member-2", "member-3", "member-4",
                                             "member-5", "member-6", "member-7", "member-8", "member-9"};
    private static final String EXPECTED = String.join("", MEMBERS);

    @Test
    public void syncGetObject_whenBodyContainsConcatenatedGzip_decodesAllMembers(WireMockRuntimeInfo wm) throws Exception {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(concatenatedGzip(MEMBERS))));

        try (S3Client s3 = syncClient(wm)) {
            ResponseInputStream<GetObjectResponse> body =
                s3.getObject(r -> r.bucket(BUCKET).key(KEY), ResponseTransformer.toInputStream());
            assertThat(gunzip(body)).isEqualTo(EXPECTED);
        }
    }

    @Test
    public void asyncGetObject_whenBodyContainsConcatenatedGzip_decodesAllMembers(WireMockRuntimeInfo wm) throws Exception {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200).withBody(concatenatedGzip(MEMBERS))));

        try (S3AsyncClient s3Async = asyncClient(wm)) {
            ResponseInputStream<GetObjectResponse> body =
                s3Async.getObject(r -> r.bucket(BUCKET).key(KEY), AsyncResponseTransformer.toBlockingInputStream()).join();
            assertThat(gunzip(body)).isEqualTo(EXPECTED);
        }
    }

    private static S3Client syncClient(WireMockRuntimeInfo wm) {
        return S3Client.builder()
                       .region(Region.US_EAST_1)
                       .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                       .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                       .build();
    }

    private static S3AsyncClient asyncClient(WireMockRuntimeInfo wm) {
        return S3AsyncClient.builder()
                            .region(Region.US_EAST_1)
                            .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
                            .build();
    }

    private static byte[] concatenatedGzip(String... members) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (String member : members) {
            ByteArrayOutputStream one = new ByteArrayOutputStream();
            try (GZIPOutputStream gz = new GZIPOutputStream(one)) {
                gz.write(member.getBytes(StandardCharsets.UTF_8));
            }
            out.write(one.toByteArray());
        }
        return out.toByteArray();
    }

    private static String gunzip(InputStream in) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(in)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[64];
            int n;
            while ((n = gz.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
