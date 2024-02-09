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

package software.amazon.awssdk.services.s3.internal.plugins;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SelectedAuthScheme;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.spi.scheme.AuthSchemeOption;
import software.amazon.awssdk.http.auth.spi.signer.SignerProperty;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.utils.Validate;

class S3SignerPropertiesPluginsTest {
    private static final String PUT_BODY = "put body";
    private static String DEFAULT_BUCKET = "bucket";
    private static String DEFAULT_KEY = "key";
    private static final AwsCredentialsProvider CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    @ParameterizedTest
    @MethodSource("testCases")
    void validateTestCase(TestCase testCase) {
        CapturingInterceptor capturingInterceptor = new CapturingInterceptor();
        S3ClientBuilder clientBuilder = getS3ClientBuilder(capturingInterceptor);
        testCase.configureClient().accept(clientBuilder);
        S3Client client = clientBuilder.build();

        assertThatThrownBy(() -> testCase.useClient().accept(client))
            .hasMessageContaining("boom")
            .as(testCase.name() + " - Expected exception");

        AuthSchemeOption expectedValues = testCase.expectedSignerProperties();
        Map<SignerProperty<?>, Object> expectedProperties = signerProperties(expectedValues);

        assertThat(
            selectSignerProperties(
                signerProperties(capturingInterceptor.authSchemeOption()),
                expectedProperties.keySet()))
            .isEqualTo(expectedProperties)
            .as(testCase.name() + " - Expected Properties");

        assertThat(
            selectSignerProperties(
                signerProperties(capturingInterceptor.authSchemeOption()),
                testCase.unsetProperties()))
            .isEqualTo(Collections.emptyMap())
            .as(testCase.name() + " - Expected Unset Properties");
    }

    static Map<SignerProperty<?>, Object> signerProperties(AuthSchemeOption option) {
        return SignerPropertiesBuilder.from(option).build();
    }

    static Map<SignerProperty<?>, Object> selectSignerProperties(
        Map<SignerProperty<?>, Object> signerProperties,
        Collection<SignerProperty<?>> keys
    ) {
        Map<SignerProperty<?>, Object> result = new HashMap<>();
        for (SignerProperty<?> key : keys) {
            if (signerProperties.containsKey(key)) {
                result.put(key, signerProperties.get(key));
            }
        }
        return result;
    }

    public static Collection<TestCase> testCases() {
        return Arrays.asList(
            // S3DisableChunkEncodingIfConfiguredPlugin, honors
            // S#Configuration.enableChunkEncoding(false)
            testUploadPartEnablesChunkEncodingByDefault(),
            testUploadPartDisablesChunkEncodingWhenConfigured(),
            testPutObjectEnablesChunkEncodingByDefault(),
            testPutObjectDisablesChunkEncodingWhenConfigured(),
            testGetObjectDoesNotSetChunkEncoding(),
            testGetObjectDoesNotSetChunkEncodingIfNotConfigured(),
            testGetObjectDoesNotSetChunkEncodingIfConfigured(),
            // S3OverrideAuthSchemePropertiesPlugin.enablePayloadSigningPlugin()
            testUploadPartDisablesPayloadSigningByDefault(),
            testUploadPartEnablesPayloadSigningUsingPlugin(),
            // S3OverrideAuthSchemePropertiesPlugin.disableChunkEncoding()
            testUploadPartDisablesChunkEncodingUsingPlugin(),
            testPutObjectDisablesChunkEncodingUsingPlugin(),
            testGetObjectDoesNotDisablesChunkEncodingUsingPlugin()
        );
    }

    // S3OverrideAuthSchemePropertiesPlugin.enablePayloadSigningPlugin()
    private static TestCase testUploadPartDisablesPayloadSigningByDefault() {
        return forUploadPart("Disables PayloadSigning By Default")
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder()
                                          .putSignerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false)
                                          .build())
            .build();
    }

    private static TestCase testUploadPartEnablesPayloadSigningUsingPlugin() {
        return forUploadPart("Enables PayloadSigning Using Plugin")
            .configureClient(c -> c.addPlugin(S3OverrideAuthSchemePropertiesPlugin.enablePayloadSigningPlugin()))
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder()
                                          .putSignerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, true)
                                          .build())
            .build();

    }

    // S3OverrideAuthSchemePropertiesPlugin.disableChunkEncoding()
    private static TestCase testUploadPartDisablesChunkEncodingUsingPlugin() {
        return forUploadPart("Disables ChunkEncoding Using Plugin")
            .configureClient(c -> c.addPlugin(S3OverrideAuthSchemePropertiesPlugin.disableChunkEncodingPlugin()))
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder()
                                          .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, false)
                                          .build())
            .build();

    }

    static TestCase testPutObjectDisablesChunkEncodingUsingPlugin() {
        return forPutObject("Disables ChunkEncoding Using Plugin")
            .configureClient(c -> c.addPlugin(S3OverrideAuthSchemePropertiesPlugin.disableChunkEncodingPlugin()))
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder()
                                          .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, false)
                                          .build())
            .build();
    }

    static TestCase testGetObjectDoesNotDisablesChunkEncodingUsingPlugin() {
        return forGetObject("Does Not Disable ChunkEncoding Using Plugin")
            .configureClient(c -> c.addPlugin(S3OverrideAuthSchemePropertiesPlugin.disableChunkEncodingPlugin()))
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder()
                                          .build())
            .addExpectedUnsetProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED)
            .build();
    }

    // S3DisableChunkEncodingIfConfiguredPlugin
    static TestCase testUploadPartEnablesChunkEncodingByDefault() {
        return forUploadPart("Enables ChunkEncoding By Default")
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder()
                                          .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, true)
                                          .build())
            .build();
    }

    static TestCase testUploadPartDisablesChunkEncodingWhenConfigured() {
        return forUploadPart("Disables ChunkEncoding When Configured")
            .configureClient(c -> c.serviceConfiguration(S3Configuration.builder()
                                                                        .chunkedEncodingEnabled(false)
                                                                        .build()))
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder()
                                          .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, false)
                                          .build())
            .build();
    }

    static TestCase testPutObjectEnablesChunkEncodingByDefault() {
        return forPutObject("Enables ChunkEncoding By Default")
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder()
                                          .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, true)
                                          .build())
            .build();
    }

    static TestCase testPutObjectDisablesChunkEncodingWhenConfigured() {
        return forPutObject("Disables ChunkEncoding When Configured")
            .configureClient(c -> c.serviceConfiguration(S3Configuration.builder()
                                                                        .chunkedEncodingEnabled(false)
                                                                        .build()))
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder()
                                          .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, false)
                                          .build())
            .build();
    }

    static TestCase testGetObjectDoesNotSetChunkEncoding() {
        return forGetObject("Does Not Set ChunkEncoding")
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder().build())
            .addExpectedUnsetProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED)
            .build();
    }

    static TestCase testGetObjectDoesNotSetChunkEncodingIfNotConfigured() {
        return forGetObject("Does Not Set ChunkEncoding If Not Configured")
            .configureClient(c -> c.serviceConfiguration(S3Configuration.builder()
                                                                        .chunkedEncodingEnabled(true)
                                                                        .build()))
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder().build())
            .addExpectedUnsetProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED)
            .build();
    }

    static TestCase testGetObjectDoesNotSetChunkEncodingIfConfigured() {
        return forGetObject("Does Not Set ChunkEncoding If Configured")
            .configureClient(c -> c.serviceConfiguration(S3Configuration.builder()
                                                                        .chunkedEncodingEnabled(false)
                                                                        .build()))
            .expectedSignerProperties(defaultExpectedAuthSchemeOptionBuilder().build())
            .addExpectedUnsetProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED)
            .build();
    }

    // End of tests, utils next
    static TestCaseBuilder forUploadPart(String name) {
        return testCaseBuilder("UploadPart " + name)
            .useClient(c -> {
                UploadPartRequest.Builder requestBuilder =
                    UploadPartRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY).partNumber(0).uploadId("test");
                c.uploadPart(requestBuilder.build(), RequestBody.fromString(PUT_BODY));
            });
    }

    static TestCaseBuilder forPutObject(String name) {
        return testCaseBuilder("PutObject " + name)
            .useClient(c -> {
                PutObjectRequest.Builder requestBuilder =
                    PutObjectRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY);
                c.putObject(requestBuilder.build(), RequestBody.fromString(PUT_BODY));
            });
    }

    static TestCaseBuilder forGetObject(String name) {
        return testCaseBuilder("GetObject " + name)
            .useClient(c -> {
                GetObjectRequest.Builder requestBuilder =
                    GetObjectRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY);
                c.getObject(requestBuilder.build());
            });
    }

    public static TestCaseBuilder testCaseBuilder(String name) {
        return new TestCaseBuilder(name);
    }

    static AuthSchemeOption.Builder defaultExpectedAuthSchemeOptionBuilder() {
        return AuthSchemeOption.builder()
                               .schemeId(AwsV4AuthScheme.SCHEME_ID)
                               .putSignerProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH, false)
                               .putSignerProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE, false)
                               .putSignerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false);
    }

    static S3ClientBuilder getS3ClientBuilder(CapturingInterceptor capturingInterceptor) {
        return S3Client.builder()
                       .region(Region.US_EAST_1)
                       .overrideConfiguration(c -> c.addExecutionInterceptor(capturingInterceptor))
                       .credentialsProvider(CREDENTIALS_PROVIDER);
    }

    public static class TestCaseBuilder {
        private final String name;
        private Consumer<S3ClientBuilder> configureClient = c -> {
        };
        private Consumer<S3Client> useClient;
        private AuthSchemeOption expectedSignerProperties = defaultExpectedAuthSchemeOptionBuilder().build();
        private Set<SignerProperty<?>> unsetProperties = new HashSet<>();

        public TestCaseBuilder(String name) {
            this.name = name;
        }

        public Consumer<S3ClientBuilder> configureClient() {
            return configureClient;
        }

        public TestCaseBuilder configureClient(Consumer<S3ClientBuilder> configureClient) {
            this.configureClient = configureClient;
            return this;
        }

        public Consumer<S3Client> useClient() {
            return useClient;
        }

        public TestCaseBuilder useClient(Consumer<S3Client> useClient) {
            this.useClient = useClient;
            return this;
        }

        public AuthSchemeOption expectedSignerProperties() {
            return expectedSignerProperties;
        }

        public TestCaseBuilder expectedSignerProperties(AuthSchemeOption expectedSignerProperties) {
            this.expectedSignerProperties = expectedSignerProperties;
            return this;
        }

        public Set<SignerProperty<?>> unsetProperties() {
            if (unsetProperties.isEmpty()) {
                return Collections.emptySet();
            }
            return Collections.unmodifiableSet(new HashSet<>(this.unsetProperties));
        }

        public TestCaseBuilder unsetProperties(Set<SignerProperty<?>> unsetProperties) {
            this.unsetProperties.clear();
            this.unsetProperties.addAll(unsetProperties);
            return this;
        }

        public TestCaseBuilder addExpectedUnsetProperty(SignerProperty<?> unsetProperty) {
            this.unsetProperties.add(unsetProperty);
            return this;
        }

        public String name() {
            return name;
        }

        public TestCase build() {
            return new TestCase(this);
        }
    }

    static class TestCase {
        private final String name;
        private final Consumer<S3ClientBuilder> configureClient;
        private final Consumer<S3Client> useClient;
        private final AuthSchemeOption expectedSignerProperties;
        private final Set<SignerProperty<?>> unsetProperties;

        public TestCase(TestCaseBuilder builder) {
            this.name = Validate.paramNotNull(builder.name(), "name");
            this.configureClient = Validate.paramNotNull(builder.configureClient(), "configureClient");
            this.useClient = Validate.paramNotNull(builder.useClient(), "useClient");
            this.expectedSignerProperties = Validate.paramNotNull(builder.expectedSignerProperties(), "expectedSignerProperties");
            this.unsetProperties = Validate.paramNotNull(builder.unsetProperties(), "unsetProperties");
        }

        public String name() {
            return name;
        }

        public Consumer<S3ClientBuilder> configureClient() {
            return configureClient;
        }

        public Consumer<S3Client> useClient() {
            return useClient;
        }

        public AuthSchemeOption expectedSignerProperties() {
            return expectedSignerProperties;
        }

        public Set<SignerProperty<?>> unsetProperties() {
            return unsetProperties;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class SignerPropertiesBuilder {
        Map<SignerProperty<?>, Object> map = new HashMap<>();

        static SignerPropertiesBuilder from(AuthSchemeOption option) {
            SignerPropertiesBuilder builder =
                new SignerPropertiesBuilder();
            option.forEachSignerProperty(builder::putSignerProperty);
            return builder;
        }

        public <T> void putSignerProperty(SignerProperty<T> key, T value) {
            map.put(key, value);
        }

        public Map<SignerProperty<?>, Object> build() {
            return map;
        }
    }

    static class CapturingInterceptor implements ExecutionInterceptor {
        private static final RuntimeException RTE = new RuntimeException("boom");
        private SelectedAuthScheme<?> selectedAuthScheme;

        @Override
        public void beforeTransmission(Context.BeforeTransmission context, ExecutionAttributes executionAttributes) {
            selectedAuthScheme = executionAttributes.getAttribute(SdkInternalExecutionAttribute.SELECTED_AUTH_SCHEME);
            throw RTE;
        }

        public AuthSchemeOption authSchemeOption() {
            if (selectedAuthScheme == null) {
                return null;
            }
            return selectedAuthScheme.authSchemeOption();
        }
    }
}
