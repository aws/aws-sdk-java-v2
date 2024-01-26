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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

class S3DisableChunkEncodingIfConfiguredPluginTest {
    private static final String DEFAULT_BUCKET = "bucket";
    private static final String DEFAULT_KEY = "key";
    private static final String PUT_BODY = "Hello from Java SDK";

    private static final AwsCredentialsProvider CREDENTIALS_PROVIDER =
        StaticCredentialsProvider.create(AwsBasicCredentials.create("akid", "skid"));

    CapturingInterceptor capturingInterceptor = null;

    @BeforeEach
    void setup() {
        capturingInterceptor = new CapturingInterceptor();
    }

    @Test
    void testUploadPartEnablesChunkEncodingByDefault() {
        S3Client syncClient = getS3ClientBuilder()
            .build();
        UploadPartRequest.Builder requestBuilder =
            UploadPartRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY).partNumber(0).uploadId("test");
        assertThatThrownBy(() -> syncClient.uploadPart(requestBuilder.build(), RequestBody.fromString(PUT_BODY)))
            .hasMessageContaining("boom");

        AuthSchemeOption expectedValues = defaultExpectedAuthSchemeOptionBuilder()
            .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, true)
            .build();
        Map<SignerProperty<?>, Object> expectedProperties = signerProperties(expectedValues);
        assertThat(selectSignerProperties(signerProperties(authSchemeOption()), expectedProperties.keySet()))
            .isEqualTo(expectedProperties);
    }

    @Test
    void testUploadPartDisablesChunkEncodingWhenConfigured() {
        S3Client syncClient = getS3ClientBuilder()
            .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
            .build();
        UploadPartRequest.Builder requestBuilder =
            UploadPartRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY).partNumber(0).uploadId("test");
        assertThatThrownBy(() -> syncClient.uploadPart(requestBuilder.build(), RequestBody.fromString(PUT_BODY)))
            .hasMessageContaining("boom");

        AuthSchemeOption expectedValues = defaultExpectedAuthSchemeOptionBuilder()
            .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, false)
            .build();
        Map<SignerProperty<?>, Object> expectedProperties = signerProperties(expectedValues);
        assertThat(selectSignerProperties(signerProperties(authSchemeOption()), expectedProperties.keySet()))
            .isEqualTo(expectedProperties);
    }

    @Test
    void testPutObjectEnablesChunkEncodingByDefault() {
        S3Client syncClient = getS3ClientBuilder()
            .build();
        PutObjectRequest.Builder requestBuilder =
            PutObjectRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY);
        assertThatThrownBy(() -> syncClient.putObject(requestBuilder.build(), RequestBody.fromString(PUT_BODY)))
            .hasMessageContaining("boom");

        AuthSchemeOption expectedValues = defaultExpectedAuthSchemeOptionBuilder()
            .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, true)
            .build();
        Map<SignerProperty<?>, Object> expectedProperties = signerProperties(expectedValues);
        assertThat(selectSignerProperties(signerProperties(authSchemeOption()), expectedProperties.keySet()))
            .isEqualTo(expectedProperties);
    }

    @Test
    void testPutObjectDisablesChunkEncodingWhenConfigured() {
        S3Client syncClient = getS3ClientBuilder()
            .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
            .build();
        PutObjectRequest.Builder requestBuilder =
            PutObjectRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY);
        assertThatThrownBy(() -> syncClient.putObject(requestBuilder.build(), RequestBody.fromString(PUT_BODY)))
            .hasMessageContaining("boom");

        AuthSchemeOption expectedValues = defaultExpectedAuthSchemeOptionBuilder()
            .putSignerProperty(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED, false)
            .build();
        Map<SignerProperty<?>, Object> expectedProperties = signerProperties(expectedValues);
        assertThat(selectSignerProperties(signerProperties(authSchemeOption()), expectedProperties.keySet()))
            .isEqualTo(expectedProperties);
    }

    @Test
    void testGetObjectDoesNotSetChunkEncoding() {
        S3Client syncClient = getS3ClientBuilder()
            .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(true).build())
            .build();
        GetObjectRequest.Builder requestBuilder =
            GetObjectRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY);
        assertThatThrownBy(() -> syncClient.getObject(requestBuilder.build()))
            .hasMessageContaining("boom");

        AuthSchemeOption expectedValues = defaultExpectedAuthSchemeOptionBuilder()
            .build();
        Map<SignerProperty<?>, Object> expectedProperties = signerProperties(expectedValues);
        Map<SignerProperty<?>, Object> givenProperties = signerProperties(authSchemeOption());
        assertThat(selectSignerProperties(givenProperties, expectedProperties.keySet()))
            .isEqualTo(expectedProperties);
        assertThat(givenProperties.get(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED)).isNull();
    }

    @Test
    void testGetObjectDoesNotSetChunkEncodingIfNotConfigured() {
        S3Client syncClient = getS3ClientBuilder()
            .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(true).build())
            .build();
        GetObjectRequest.Builder requestBuilder =
            GetObjectRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY);
        assertThatThrownBy(() -> syncClient.getObject(requestBuilder.build()))
            .hasMessageContaining("boom");

        AuthSchemeOption expectedValues = defaultExpectedAuthSchemeOptionBuilder()
            .build();
        Map<SignerProperty<?>, Object> expectedProperties = signerProperties(expectedValues);
        Map<SignerProperty<?>, Object> givenProperties = signerProperties(authSchemeOption());
        assertThat(selectSignerProperties(givenProperties, expectedProperties.keySet()))
            .isEqualTo(expectedProperties);
        assertThat(givenProperties.get(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED)).isNull();
    }

    @Test
    void testGetObjectDoesNotSetChunkEncodingIfConfiguredAsEnabled() {
        S3Client syncClient = getS3ClientBuilder()
            .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(true).build())
            .build();
        GetObjectRequest.Builder requestBuilder =
            GetObjectRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY);
        assertThatThrownBy(() -> syncClient.getObject(requestBuilder.build()))
            .hasMessageContaining("boom");

        AuthSchemeOption expectedValues = defaultExpectedAuthSchemeOptionBuilder()
            .build();
        Map<SignerProperty<?>, Object> expectedProperties = signerProperties(expectedValues);
        Map<SignerProperty<?>, Object> givenProperties = signerProperties(authSchemeOption());
        assertThat(selectSignerProperties(givenProperties, expectedProperties.keySet()))
            .isEqualTo(expectedProperties);
        assertThat(givenProperties.get(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED)).isNull();
    }

    @Test
    void testGetObjectDoesNotSetChunkEncodingIfConfiguredAsDisabled() {
        S3Client syncClient = getS3ClientBuilder()
            .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
            .build();
        GetObjectRequest.Builder requestBuilder =
            GetObjectRequest.builder().bucket(DEFAULT_BUCKET).key(DEFAULT_KEY);
        assertThatThrownBy(() -> syncClient.getObject(requestBuilder.build()))
            .hasMessageContaining("boom");

        AuthSchemeOption expectedValues = defaultExpectedAuthSchemeOptionBuilder()
            .build();
        Map<SignerProperty<?>, Object> expectedProperties = signerProperties(expectedValues);
        Map<SignerProperty<?>, Object> givenProperties = signerProperties(authSchemeOption());
        assertThat(selectSignerProperties(givenProperties, expectedProperties.keySet()))
            .isEqualTo(expectedProperties);
        assertThat(givenProperties.get(AwsV4FamilyHttpSigner.CHUNK_ENCODING_ENABLED)).isNull();
    }

    private AuthSchemeOption authSchemeOption() {
        return capturingInterceptor.authSchemeOption();
    }

    AuthSchemeOption.Builder defaultExpectedAuthSchemeOptionBuilder() {
        return AuthSchemeOption.builder()
                               .schemeId(AwsV4AuthScheme.SCHEME_ID)
                               // The following properties are always set
                               .putSignerProperty(AwsV4FamilyHttpSigner.NORMALIZE_PATH, false)
                               .putSignerProperty(AwsV4FamilyHttpSigner.DOUBLE_URL_ENCODE, false)
                               .putSignerProperty(AwsV4FamilyHttpSigner.PAYLOAD_SIGNING_ENABLED, false);
    }

    Map<SignerProperty<?>, Object> signerProperties(AuthSchemeOption option) {
        return SignerPropertiesBuilder.from(option).build();
    }

    Map<SignerProperty<?>, Object> selectSignerProperties(
        Map<SignerProperty<?>, Object> signerProperties,
        Collection<SignerProperty<?>> keys) {
        Map<SignerProperty<?>, Object> result = new HashMap<>();
        for (SignerProperty<?> key : keys) {
            if (signerProperties.containsKey(key)) {
                result.put(key, signerProperties.get(key));
            }
        }
        return result;
    }

    S3ClientBuilder getS3ClientBuilder() {
        return S3Client.builder()
                       .region(Region.US_EAST_1)
                       .overrideConfiguration(c -> c.addExecutionInterceptor(capturingInterceptor))
                       .credentialsProvider(CREDENTIALS_PROVIDER);
    }


    static class SignerPropertiesBuilder {
        Map<SignerProperty<?>, Object> map = new HashMap<>();

        static SignerPropertiesBuilder from(AuthSchemeOption option) {
            SignerPropertiesBuilder builder = new SignerPropertiesBuilder();
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
