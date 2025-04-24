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

package software.amazon.awssdk.services.s3.plugin.legacymd5;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.any;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.http.Header.CONTENT_MD5;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.LegacyMd5Plugin;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@WireMockTest
@DisplayName("Legacy MD5 Plugin Operation Tests")
class LegacyMd5PluginOperationTest {

    private static final String BUCKET_NAME = "bucket";
    private static final String KEY_NAME = "key";
    private static final String TEST_CONTENT = "Hello";
    private static final CapturingInterceptor CAPTURING_INTERCEPTOR = new CapturingInterceptor();

    private S3AsyncClient clientWithPlugin;
    private S3AsyncClient clientWithoutPlugin;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(any(anyUrl()).willReturn(aResponse().withStatus(200)));
        URI endpoint = URI.create("http://localhost:" + wmRuntimeInfo.getHttpPort());
        clientWithPlugin = createS3Client(endpoint, true);
        clientWithoutPlugin = createS3Client(endpoint, false);
    }

    private S3AsyncClient createS3Client(URI endpoint, boolean withPlugin) {
        S3AsyncClientBuilder builder = S3AsyncClient.builder()
                                                    .credentialsProvider(StaticCredentialsProvider.create(
                                                         AwsBasicCredentials.create("akid", "skid")))
                                                    .region(Region.US_WEST_2)
                                                    .endpointOverride(endpoint)
                                                    .serviceConfiguration(S3Configuration.builder()
                                                                                          .pathStyleAccessEnabled(true)
                                                                                          .build())
                                                    .overrideConfiguration(c -> c.addExecutionInterceptor(CAPTURING_INTERCEPTOR));

        if (withPlugin) {
            builder.addPlugin(LegacyMd5Plugin.create());
        }
        return builder.build();
    }

    @AfterEach
    void tearDown() {
        CAPTURING_INTERCEPTOR.reset();
        if (clientWithPlugin != null) {
            clientWithPlugin.close();
        }
        if (clientWithoutPlugin != null) {
            clientWithoutPlugin.close();
        }
    }

    @Nested
    @DisplayName("With Legacy MD5 Plugin")
    class WithLegacyMd5Plugin {

        @Test
        @DisplayName("DeleteObjects operation should include MD5 checksum since its required")
        void deleteObjectsShouldIncludeMd5Checksum() {
            clientWithPlugin.deleteObjects(r -> r.bucket(BUCKET_NAME)
                                                 .delete(d -> d.objects(o -> o.key(KEY_NAME))))
                            .join();

            assertThat(CAPTURING_INTERCEPTOR.md5Checksum).isEqualTo("/JqOxTf3mydOdMWAqGGa3w==");
            assertThat(CAPTURING_INTERCEPTOR.crc32ChecksumHeader).isEqualTo("Xyuzcg==");
            assertThat(CAPTURING_INTERCEPTOR.checksumTrailer).isNull();
        }

        @Test
        @DisplayName("PutObject  operation should not include checksums since its not required but just supported")
        void putObjectShouldNotIncludeChecksums() {
            clientWithPlugin.putObject(
                r -> r.bucket(BUCKET_NAME).key(KEY_NAME),
                AsyncRequestBody.fromBytes(TEST_CONTENT.getBytes(StandardCharsets.UTF_8))
            ).join();

            assertThat(CAPTURING_INTERCEPTOR.md5Checksum).isNull();
            assertThat(CAPTURING_INTERCEPTOR.crc32ChecksumHeader).isNull();
            assertThat(CAPTURING_INTERCEPTOR.checksumTrailer).isNull();
        }
    }

    @Nested
    @DisplayName("Without Legacy MD5 Plugin")
    class WithoutLegacyMd5Plugin {

        @Test
        @DisplayName("DeleteObjects operation should not include MD5 checksum")
        void deleteObjectsShouldNotIncludeMd5Checksum() {
            clientWithoutPlugin.deleteObjects(r -> r.bucket(BUCKET_NAME)
                                                    .delete(d -> d.objects(o -> o.key(KEY_NAME))))
                               .join();

            assertThat(CAPTURING_INTERCEPTOR.md5Checksum).isNull();
            // default CRC32
            assertThat(CAPTURING_INTERCEPTOR.crc32ChecksumHeader).isEqualTo("Xyuzcg==");
            assertThat(CAPTURING_INTERCEPTOR.checksumTrailer).isNull();
        }

        @Test
        @DisplayName("PutObject operation should use checksum trailer")
        void putObjectShouldUseChecksumTrailer() {
            clientWithoutPlugin.putObject(
                r -> r.bucket(BUCKET_NAME).key(KEY_NAME),
                AsyncRequestBody.fromBytes(TEST_CONTENT.getBytes(StandardCharsets.UTF_8))
            ).join();

            assertThat(CAPTURING_INTERCEPTOR.md5Checksum).isNull();
            assertThat(CAPTURING_INTERCEPTOR.crc32ChecksumHeader).isNull();
            // default CRC32 in trailer for streaming
            assertThat(CAPTURING_INTERCEPTOR.checksumTrailer).isEqualTo("x-amz-checksum-crc32");
        }
    }

    private static final class CapturingInterceptor implements ExecutionInterceptor {
        private String md5Checksum;
        private String crc32ChecksumHeader;
        private String checksumTrailer;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            SdkHttpFullRequest request = (SdkHttpFullRequest) context.httpRequest();
            request.firstMatchingHeader(CONTENT_MD5).ifPresent(md5 -> md5Checksum = md5);
            request.firstMatchingHeader("x-amz-checksum-crc32").ifPresent(crc32 -> crc32ChecksumHeader = crc32);
            request.firstMatchingHeader("x-amz-trailer").ifPresent(trailer -> checksumTrailer = trailer);
        }

        void reset() {
            this.crc32ChecksumHeader = null;
            this.checksumTrailer = null;
            this.md5Checksum = null;
        }
    }
}
