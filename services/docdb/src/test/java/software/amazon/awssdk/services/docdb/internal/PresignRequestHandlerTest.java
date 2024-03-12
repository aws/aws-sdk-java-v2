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

package software.amazon.awssdk.services.docdb.internal;


import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.docdb.DocDbClient;
import software.amazon.awssdk.services.docdb.DocDbClientBuilder;
import software.amazon.awssdk.services.docdb.DocDbServiceClientConfiguration;
import software.amazon.awssdk.services.docdb.auth.scheme.DocDbAuthSchemeProvider;
import software.amazon.awssdk.services.docdb.model.CopyDbClusterSnapshotRequest;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * Unit Tests for {@link RdsPresignInterceptor}
 */
class PresignRequestHandlerTest {
    private static String TEST_KMS_KEY_ID = "arn:aws:kms:us-west-2:123456789012:key/"
                                            + "11111111-2222-3333-4444-555555555555";

    @ParameterizedTest
    @MethodSource("testCases")
    public void testExpectations(TestCase testCase) {
        // Arrange
        CapturingInterceptor interceptor = new CapturingInterceptor();
        DocDbClientBuilder clientBuilder = client(interceptor, testCase.signingClockOverride);
        testCase.clientConfigure.accept(clientBuilder);
        DocDbClient client = clientBuilder.build();

        // Act
        assertThatThrownBy(() -> testCase.clientConsumer.accept(client))
            .hasMessageContaining("boom!");

        // Assert
        SdkHttpFullRequest request = (SdkHttpFullRequest) interceptor.httpRequest();
        Map<String, List<String>> rawQueryParameters = rawQueryParameters(request);

        // The following params should not be included in the outgoing request
        assertFalse(rawQueryParameters.containsKey("SourceRegion"));
        assertFalse(rawQueryParameters.containsKey("DestinationRegion"));

        if (testCase.shouldContainPreSignedUrl) {
            List<String> rawPresignedUrlValue = rawQueryParameters.get("PreSignedUrl");
            assertNotNull(rawPresignedUrlValue);
            assertTrue(rawPresignedUrlValue.size() == 1);
            String presignedUrl = rawPresignedUrlValue.get(0);
            assertNotNull(presignedUrl);
            // Validate that the URL can be parsed back
            URI presignedUrlAsUri = URI.create(presignedUrl);
            assertNotNull(presignedUrlAsUri);
            if (testCase.expectedDestinationRegion != null) {
                assertTrue(presignedUrl.contains("DestinationRegion=" + testCase.expectedDestinationRegion));
            }
            if (testCase.expectedUri != null) {
                assertEquals(normalize(URI.create(testCase.expectedUri)), normalize(presignedUrlAsUri));
            }
        } else {
            assertFalse(rawQueryParameters.containsKey("PreSignedUrl"));
        }
    }

    public static List<TestCase> testCases() {
        return Arrays.asList(
            builder("CopyDbClusterSnapshot - Sets pre-signed URL when sourceRegion is set")
                .clientConsumer(c -> c.copyDBClusterSnapshot(makeTestRequestBuilder()
                                                                 .sourceRegion("us-east-1")
                                                                 .build()))
                .shouldContainPreSignedUrl(true)
                .expectedDestinationRegion("us-east-1")
                .build(),
            builder("CopyDbClusterSnapshot - Doesn't set pre-signed URL when sourceRegion is NOT set")
                .clientConsumer(c -> c.copyDBClusterSnapshot(makeTestRequestBuilder().build()))
                .shouldContainPreSignedUrl(false)
                .build(),
            builder("CopyDbClusterSnapshot - Does not override pre-signed URL")
                .clientConsumer(c -> c.copyDBClusterSnapshot(
                    makeTestRequestBuilder()
                        .sourceRegion("us-west-2")
                        .preSignedUrl("http://localhost?foo=bar")
                        .build()))
                .shouldContainPreSignedUrl(true)
                .expectedUri("http://localhost?foo=bar")
                .build(),
            builder("CopyDbClusterSnapshot - Fixed time")
                .clientConfigure(c -> c.region(Region.US_WEST_2))
                .clientConsumer(c -> c.copyDBClusterSnapshot(
                    makeTestRequestBuilder()
                        .sourceRegion("us-east-1")
                        .build()))
                .shouldContainPreSignedUrl(true)
                .signingClockOverride(Clock.fixed(Instant.parse("2016-12-21T18:07:35.000Z"), ZoneId.of("UTC")))
                .expectedUri(fixedTimePresignedUrl())
                .build(),

            builder("createDBCluster With SourceRegion Sends Presigned Url")
                .clientConsumer(c -> c.createDBCluster(r -> r.kmsKeyId(TEST_KMS_KEY_ID)
                                                             .sourceRegion("us-west-2")))
                .shouldContainPreSignedUrl(true)
                .expectedDestinationRegion("us-east-1")
                .build(),
            builder("createDBCluster Without SourceRegion Does NOT Send PresignedUrl")
                .clientConsumer(c -> c.createDBCluster(r -> r.kmsKeyId(TEST_KMS_KEY_ID)))
                .shouldContainPreSignedUrl(false)
                .build()
        );
    }

    private static CopyDbClusterSnapshotRequest.Builder makeTestRequestBuilder() {
        return CopyDbClusterSnapshotRequest
            .builder()
            .sourceDBClusterSnapshotIdentifier("arn:aws:rds:us-east-1:123456789012:snapshot:rds"
                                               + ":test-instance-ss-2016-12-20-23-19")
            .targetDBClusterSnapshotIdentifier("test-instance-ss-copy-2")
            .kmsKeyId(TEST_KMS_KEY_ID);
    }

    private static DocDbClientBuilder client(CapturingInterceptor interceptor, Clock signingClockOverride) {
        DocDbClientBuilder builder = DocDbClient
            .builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("foo", "bar")))
            .region(Region.US_EAST_1)
            .addPlugin(c -> {
                // Adds the capturing interceptor.
                DocDbServiceClientConfiguration.Builder config =
                    Validate.isInstanceOf(DocDbServiceClientConfiguration.Builder.class, c,
                                          "\uD83E\uDD14");
                config.overrideConfiguration(oc -> oc.addExecutionInterceptor(interceptor));
            });

        if (signingClockOverride != null) {
            // Adds a auth scheme wrapper that handles the clock override
            builder.addPlugin(c -> {
                DocDbServiceClientConfiguration.Builder config =
                    Validate.isInstanceOf(DocDbServiceClientConfiguration.Builder.class, c, "\uD83E\uDD14");
                config.authSchemeProvider(clockOverridingAuthScheme(config.authSchemeProvider(), signingClockOverride));
            });
        }
        return builder;
    }

    private static DocDbAuthSchemeProvider clockOverridingAuthScheme(DocDbAuthSchemeProvider source, Clock signingClockOverride) {
        return authSchemeParams -> {
            List<AuthSchemeOption> authSchemeOptions = source.resolveAuthScheme(authSchemeParams);
            List<AuthSchemeOption> result = new ArrayList<>(authSchemeOptions.size());
            for (AuthSchemeOption option : authSchemeOptions) {
                if (option.schemeId().equals(AwsV4AuthScheme.SCHEME_ID)) {
                    option = option.toBuilder()
                                   .putSignerProperty(AwsV4FamilyHttpSigner.SIGNING_CLOCK, signingClockOverride)
                                   .build();
                }
                result.add(option);
            }
            return result;
        };
    }

    static String fixedTimePresignedUrl() {
        return
            "https://rds.us-east-1.amazonaws.com?" +
            "Action=CopyDBClusterSnapshot" +
            "&Version=2014-10-31" +
            "&SourceDBClusterSnapshotIdentifier=arn%3Aaws%3Ards%3Aus-east-1%3A123456789012"
            + "%3Asnapshot%3Ards%3Atest-instance-ss-2016-12-20-23-19" +
            "&TargetDBClusterSnapshotIdentifier=test-instance-ss-copy-2" +
            "&KmsKeyId=arn%3Aaws%3Akms%3Aus-west-2%3A123456789012%3Akey%2F11111111-2222-3333"
            + "-4444-555555555555" +
            "&DestinationRegion=us-west-2" +
            "&X-Amz-Algorithm=AWS4-HMAC-SHA256" +
            "&X-Amz-Date=20161221T180735Z" +
            "&X-Amz-SignedHeaders=host" +
            "&X-Amz-Credential=foo%2F20161221%2Fus-east-1%2Frds%2Faws4_request" +
            "&X-Amz-Expires=604800" +
            "&X-Amz-Signature=00822ebbba95e2e6ac09112aa85621fbef060a596e3e1480f9f4ac61493e9821";
    }

    private Map<String, List<String>> rawQueryParameters(SdkHttpFullRequest request) {
        // Retrieve back from the query parameters from the body, this is best-effort only.
        try {
            String decodedQueryParams = IoUtils.toUtf8String(request.contentStreamProvider().get().newStream());
            String[] keyValuePairs = decodedQueryParams.split("&");
            Map<String, List<String>> result = new LinkedHashMap<>();
            for (String keyValuePair : keyValuePairs) {
                String[] kvpParts = keyValuePair.split("=", 2);
                String value = URLDecoder.decode(kvpParts.length > 1 ? kvpParts[1] : "", StandardCharsets.UTF_8.name());
                result.computeIfAbsent(kvpParts[0], x -> new ArrayList<>()).add(value);
            }
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static TestCaseBuilder builder(String name) {
        return new TestCaseBuilder()
            .clientConfigure(c -> {
            })
            .name(name);
    }

    private static String normalize(URI uri) {
        String uriAsString = uri.toString();
        int queryStart = uriAsString.indexOf('?');
        if (queryStart == -1) {
            return uriAsString;
        }
        String uriQueryPrefix = uriAsString.substring(0, queryStart);
        String query = uri.getQuery();
        if (query == null) {
            return uriAsString;
        }
        if (!query.isEmpty()) {
            String[] queryParts = query.split("&");
            query = Arrays.stream(queryParts)
                          .sorted()
                          .collect(Collectors.joining("&"));

        }
        return uriQueryPrefix + "?" + query;
    }

    static class TestCase {
        private final String name;
        private final Consumer<DocDbClientBuilder> clientConfigure;
        private final Consumer<DocDbClient> clientConsumer;
        private final Boolean shouldContainPreSignedUrl;
        private final String expectedDestinationRegion;
        private final Clock signingClockOverride;
        private final String expectedUri;

        TestCase(TestCaseBuilder builder) {
            this.name = Validate.notNull(builder.name, "name");
            this.clientConsumer = Validate.notNull(builder.clientConsumer, "clientConsumer");
            this.clientConfigure = Validate.notNull(builder.clientConfigure, "clientConfigure");
            this.shouldContainPreSignedUrl = builder.shouldContainPreSignedUrl;
            this.expectedDestinationRegion = builder.expectedDestinationRegion;
            this.signingClockOverride = builder.signingClockOverride;
            this.expectedUri = builder.expectedUri;
        }
    }

    static class TestCaseBuilder {
        private String name;
        private Consumer<DocDbClientBuilder> clientConfigure;
        private Consumer<DocDbClient> clientConsumer;
        private Boolean shouldContainPreSignedUrl;
        private String expectedDestinationRegion;
        private Clock signingClockOverride;
        private String expectedUri;

        private TestCaseBuilder name(String name) {
            this.name = name;
            return this;
        }

        private TestCaseBuilder clientConfigure(Consumer<DocDbClientBuilder> clientConfigure) {
            this.clientConfigure = clientConfigure;
            return this;
        }

        private TestCaseBuilder clientConsumer(Consumer<DocDbClient> clientConsumer) {
            this.clientConsumer = clientConsumer;
            return this;
        }

        private TestCaseBuilder shouldContainPreSignedUrl(Boolean value) {
            this.shouldContainPreSignedUrl = value;
            return this;
        }

        private TestCaseBuilder expectedDestinationRegion(String value) {
            this.expectedDestinationRegion = value;
            return this;
        }

        public TestCaseBuilder signingClockOverride(Clock signingClockOverride) {
            this.signingClockOverride = signingClockOverride;
            return this;
        }

        public TestCaseBuilder expectedUri(String expectedUri) {
            this.expectedUri = expectedUri;
            return this;
        }

        public TestCase build() {
            return new TestCase(this);
        }
    }

    static class CapturingInterceptor implements ExecutionInterceptor {
        private static final RuntimeException BOOM = new RuntimeException("boom!");
        private Context.BeforeTransmission context;
        private ExecutionAttributes executionAttributes;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            this.context = context;
            this.executionAttributes = executionAttributes;
            throw BOOM;
        }

        public ExecutionAttributes executionAttributes() {
            return executionAttributes;
        }

        public SdkHttpRequest httpRequest() {
            return context.httpRequest();
        }
    }
}
