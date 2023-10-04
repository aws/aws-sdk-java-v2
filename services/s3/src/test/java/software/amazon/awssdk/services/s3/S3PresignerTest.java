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

package software.amazon.awssdk.services.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.internal.AbstractAwsS3V4Signer;
import software.amazon.awssdk.auth.signer.internal.SignerConstant;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.RequestPayer;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedDeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@RunWith(MockitoJUnitRunner.class)
public class S3PresignerTest {
    private static final URI FAKE_URL;
    private static final String BUCKET = "some-bucket";

    private S3Presigner presigner;

    static {
        FAKE_URL = URI.create("https://localhost");
    }

    @Before
    public void setUp() {
        this.presigner = presignerBuilder().build();
    }

    @After
    public void tearDown() {
        this.presigner.close();
    }

    private S3Presigner.Builder presignerBuilder() {
        return S3Presigner.builder()
                          .region(Region.US_WEST_2)
                          .credentialsProvider(() -> AwsBasicCredentials.create("x", "x"));
    }


    private S3Presigner generateMaximal() {
        return S3Presigner.builder()
                          .serviceConfiguration(S3Configuration.builder()
                                .checksumValidationEnabled(false)
                                .build())
                          .credentialsProvider(() -> AwsBasicCredentials.create("x", "x"))
                          .region(Region.US_EAST_1)
                          .endpointOverride(FAKE_URL)
                          .build();
    }

    private S3Presigner generateMinimal() {
        return S3Presigner.builder()
                          .credentialsProvider(() -> AwsBasicCredentials.create("x", "x"))
                          .region(Region.US_EAST_1)
                          .build();
    }

    @Test
    public void build_allProperties() {
        generateMaximal();
    }

    @Test
    public void build_minimalProperties() {
        generateMinimal();
    }

    @Test
    public void getObject_SignatureIsUrlCompatible() {
        PresignedGetObjectRequest presigned =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(go -> go.bucket("foo34343434")
                                                                       .key("bar")
                                                                       .responseContentType("text/plain")));
        assertThat(presigned.isBrowserExecutable()).isTrue();
        assertThat(presigned.signedHeaders().keySet()).containsExactly("host");
        assertThat(presigned.signedPayload()).isEmpty();
    }

    @Test
    public void getObject_RequesterPaysIsNotUrlCompatible() {
        PresignedGetObjectRequest presigned =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(go -> go.bucket("foo34343434")
                                                                       .key("bar")
                                                                       .requestPayer(RequestPayer.REQUESTER)));
        assertThat(presigned.isBrowserExecutable()).isFalse();
        assertThat(presigned.signedHeaders().keySet()).containsExactlyInAnyOrder("host", "x-amz-request-payer");
        assertThat(presigned.signedPayload()).isEmpty();
    }

    @Test
    public void getObject_EndpointOverrideIsIncludedInPresignedUrl() {
        S3Presigner presigner = presignerBuilder().endpointOverride(URI.create("http://foo.com")).build();
        PresignedGetObjectRequest presigned =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(go -> go.bucket("foo34343434")
                                                                       .key("bar")));

        assertThat(presigned.url().toString()).startsWith("http://foo34343434.foo.com/bar?");
        assertThat(presigned.isBrowserExecutable()).isTrue();
        assertThat(presigned.signedHeaders().get("host")).containsExactly("foo34343434.foo.com");
        assertThat(presigned.signedPayload()).isEmpty();
    }

    @Test
    public void getObject_CredentialsCanBeOverriddenAtTheRequestLevel() {
        AwsCredentials clientCredentials = AwsBasicCredentials.create("a", "a");
        AwsCredentials requestCredentials = AwsBasicCredentials.create("b", "b");

        S3Presigner presigner = presignerBuilder().credentialsProvider(() -> clientCredentials).build();


        AwsRequestOverrideConfiguration overrideConfiguration =
            AwsRequestOverrideConfiguration.builder()
                                           .credentialsProvider(() -> requestCredentials)
                                           .build();

        PresignedGetObjectRequest presignedWithClientCredentials =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(go -> go.bucket("foo34343434")
                                                                       .key("bar")));

        PresignedGetObjectRequest presignedWithRequestCredentials =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(go -> go.bucket("foo34343434")
                                                                       .key("bar")
                                                                       .overrideConfiguration(overrideConfiguration)));


        assertThat(presignedWithClientCredentials.httpRequest().rawQueryParameters().get("X-Amz-Credential").get(0))
            .startsWith("a");
        assertThat(presignedWithRequestCredentials.httpRequest().rawQueryParameters().get("X-Amz-Credential").get(0))
            .startsWith("b");
    }

    @Test
    public void getObject_AdditionalHeadersAndQueryStringsCanBeAdded() {
        AwsRequestOverrideConfiguration override =
            AwsRequestOverrideConfiguration.builder()
                                           .putHeader("X-Amz-AdditionalHeader", "foo1")
                                           .putRawQueryParameter("additionalQueryParam", "foo2")
                                           .build();

        PresignedGetObjectRequest presigned =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(go -> go.bucket("foo34343434")
                                                                       .key("bar")
                                                                       .overrideConfiguration(override)));

        assertThat(presigned.isBrowserExecutable()).isFalse();
        assertThat(presigned.signedHeaders()).containsOnlyKeys("host", "x-amz-additionalheader");
        assertThat(presigned.signedHeaders().get("x-amz-additionalheader")).containsExactly("foo1");
        assertThat(presigned.httpRequest().headers()).containsKeys("x-amz-additionalheader");
        assertThat(presigned.httpRequest().rawQueryParameters().get("additionalQueryParam").get(0)).isEqualTo("foo2");
    }

    @Test
    public void getObject_NonSigV4SignersRaisesException() {
        AwsRequestOverrideConfiguration override =
            AwsRequestOverrideConfiguration.builder()
                                           .signer(new NoOpSigner())
                                           .build();

        assertThatThrownBy(() -> presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                                                  .getObjectRequest(go -> go.bucket("foo34343434")
                                                                                            .key("bar")
                                                                                            .overrideConfiguration(override))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NoOpSigner");
    }

    @Test
    public void getObject_Sigv4PresignerHonorsSignatureDuration() {
        AwsRequestOverrideConfiguration override =
            AwsRequestOverrideConfiguration.builder()
                                           .signer(AwsS3V4Signer.create())
                                           .build();

        PresignedGetObjectRequest presigned =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofSeconds(1234))
                                             .getObjectRequest(gor -> gor.bucket("a")
                                                                         .key("b")
                                                                         .overrideConfiguration(override)));

        assertThat(presigned.httpRequest().rawQueryParameters().get("X-Amz-Expires").get(0)).satisfies(expires -> {
            assertThat(expires).containsOnlyDigits();
            assertThat(Integer.parseInt(expires)).isCloseTo(1234, Offset.offset(2));
        });
    }

    @Test
    public void putObject_IsNotUrlCompatible() {
        PresignedPutObjectRequest presigned =
            presigner.presignPutObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .putObjectRequest(go -> go.bucket("foo34343434")
                                                                       .key("bar")));
        assertThat(presigned.isBrowserExecutable()).isFalse();
        assertThat(presigned.signedHeaders().keySet()).containsExactlyInAnyOrder("host");
        assertThat(presigned.signedPayload()).isEmpty();
    }

    @Test
    public void putObject_EndpointOverrideIsIncludedInPresignedUrl() {
        S3Presigner presigner = presignerBuilder().endpointOverride(URI.create("http://foo.com")).build();
        PresignedPutObjectRequest presigned =
            presigner.presignPutObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .putObjectRequest(go -> go.bucket("foo34343434")
                                                                       .key("bar")));

        assertThat(presigned.url().toString()).startsWith("http://foo34343434.foo.com/bar?");
        assertThat(presigned.isBrowserExecutable()).isFalse();
        assertThat(presigned.signedHeaders().get("host")).containsExactly("foo34343434.foo.com");
        assertThat(presigned.signedPayload()).isEmpty();
    }

    @Test
    public void putObject_CredentialsCanBeOverriddenAtTheRequestLevel() {
        AwsCredentials clientCredentials = AwsBasicCredentials.create("a", "a");
        AwsCredentials requestCredentials = AwsBasicCredentials.create("b", "b");

        S3Presigner presigner = presignerBuilder().credentialsProvider(() -> clientCredentials).build();


        AwsRequestOverrideConfiguration overrideConfiguration =
            AwsRequestOverrideConfiguration.builder()
                                           .credentialsProvider(() -> requestCredentials)
                                           .build();

        PresignedPutObjectRequest presignedWithClientCredentials =
            presigner.presignPutObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .putObjectRequest(go -> go.bucket("foo34343434")
                                                                       .key("bar")));

        PresignedPutObjectRequest presignedWithRequestCredentials =
            presigner.presignPutObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .putObjectRequest(go -> go.bucket("foo34343434")
                                                                       .key("bar")
                                                                       .overrideConfiguration(overrideConfiguration)));

        assertThat(presignedWithClientCredentials.httpRequest().rawQueryParameters().get("X-Amz-Credential").get(0))
            .startsWith("a");
        assertThat(presignedWithRequestCredentials.httpRequest().rawQueryParameters().get("X-Amz-Credential").get(0))
            .startsWith("b");
    }

    @Test
    public void putObject_AdditionalHeadersAndQueryStringsCanBeAdded() {
        AwsRequestOverrideConfiguration override =
            AwsRequestOverrideConfiguration.builder()
                                           .putHeader("X-Amz-AdditionalHeader", "foo1")
                                           .putRawQueryParameter("additionalQueryParam", "foo2")
                                           .build();

        PresignedPutObjectRequest presigned =
            presigner.presignPutObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .putObjectRequest(go -> go.bucket("foo34343434")
                                                                       .key("bar")
                                                                       .overrideConfiguration(override)));

        assertThat(presigned.isBrowserExecutable()).isFalse();
        assertThat(presigned.signedHeaders()).containsOnlyKeys("host", "x-amz-additionalheader");
        assertThat(presigned.signedHeaders().get("x-amz-additionalheader")).containsExactly("foo1");
        assertThat(presigned.httpRequest().headers()).containsKeys("x-amz-additionalheader");
        assertThat(presigned.httpRequest().rawQueryParameters().get("additionalQueryParam").get(0)).isEqualTo("foo2");
    }

    @Test
    public void putObject_NonSigV4SignersRaisesException() {
        AwsRequestOverrideConfiguration override =
            AwsRequestOverrideConfiguration.builder()
                                           .signer(new NoOpSigner())
                                           .build();

        assertThatThrownBy(() -> presigner.presignPutObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                                                  .putObjectRequest(go -> go.bucket("foo34343434")
                                                                                            .key("bar")
                                                                                            .overrideConfiguration(override))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NoOpSigner");
    }

    @Test
    public void putObject_Sigv4PresignerHonorsSignatureDuration() {
        AwsRequestOverrideConfiguration override =
            AwsRequestOverrideConfiguration.builder()
                                           .signer(AwsS3V4Signer.create())
                                           .build();

        PresignedPutObjectRequest presigned =
            presigner.presignPutObject(r -> r.signatureDuration(Duration.ofSeconds(1234))
                                             .putObjectRequest(gor -> gor.bucket("a")
                                                                         .key("b")
                                                                         .overrideConfiguration(override)));

        assertThat(presigned.httpRequest().rawQueryParameters().get("X-Amz-Expires").get(0)).satisfies(expires -> {
            assertThat(expires).containsOnlyDigits();
            assertThat(Integer.parseInt(expires)).isCloseTo(1234, Offset.offset(2));
        });
    }

    @Test
    public void deleteObject_IsNotUrlCompatible() {
        PresignedDeleteObjectRequest presigned =
            presigner.presignDeleteObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                                .deleteObjectRequest(delo -> delo.bucket("foo34343434")
                                                                                 .key("bar")));
        assertThat(presigned.isBrowserExecutable()).isFalse();
        assertThat(presigned.signedHeaders().keySet()).containsExactlyInAnyOrder("host");
        assertThat(presigned.signedPayload()).isEmpty();
    }

    @Test
    public void deleteObject_EndpointOverrideIsIncludedInPresignedUrl() {
        S3Presigner presigner = presignerBuilder().endpointOverride(URI.create("http://foo.com")).build();
        PresignedDeleteObjectRequest presigned =
            presigner.presignDeleteObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                                .deleteObjectRequest(delo -> delo.bucket("foo34343434")
                                                                                 .key("bar")));

        assertThat(presigned.url().toString()).startsWith("http://foo34343434.foo.com/bar?");
        assertThat(presigned.signedHeaders().get("host")).containsExactly("foo34343434.foo.com");
        assertThat(presigned.signedPayload()).isEmpty();
    }

    @Test
    public void deleteObject_CredentialsCanBeOverriddenAtTheRequestLevel() {
        AwsCredentials clientCredentials = AwsBasicCredentials.create("a", "a");
        AwsCredentials requestCredentials = AwsBasicCredentials.create("b", "b");

        S3Presigner presigner = presignerBuilder().credentialsProvider(() -> clientCredentials).build();


        AwsRequestOverrideConfiguration overrideConfiguration =
            AwsRequestOverrideConfiguration.builder()
                                           .credentialsProvider(() -> requestCredentials)
                                           .build();

        PresignedDeleteObjectRequest presignedWithClientCredentials =
            presigner.presignDeleteObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                                .deleteObjectRequest(delo -> delo.bucket("foo34343434")
                                                                               .key("bar")));

        PresignedDeleteObjectRequest presignedWithRequestCredentials =
            presigner.presignDeleteObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                                .deleteObjectRequest(delo -> delo.bucket("foo34343434")
                                                                               .key("bar")
                                                                               .overrideConfiguration(overrideConfiguration)));


        assertThat(presignedWithClientCredentials.httpRequest().rawQueryParameters().get("X-Amz-Credential").get(0))
            .startsWith("a");
        assertThat(presignedWithRequestCredentials.httpRequest().rawQueryParameters().get("X-Amz-Credential").get(0))
            .startsWith("b");
    }

    @Test
    public void deleteObject_AdditionalHeadersAndQueryStringsCanBeAdded() {
        AwsRequestOverrideConfiguration override =
            AwsRequestOverrideConfiguration.builder()
                                           .putHeader("X-Amz-AdditionalHeader", "foo1")
                                           .putRawQueryParameter("additionalQueryParam", "foo2")
                                           .build();

        PresignedDeleteObjectRequest presigned =
            presigner.presignDeleteObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                                .deleteObjectRequest(delo -> delo.bucket("foo34343434")
                                                                               .key("bar")
                                                                               .overrideConfiguration(override)));

        assertThat(presigned.isBrowserExecutable()).isFalse();
        assertThat(presigned.signedHeaders()).containsOnlyKeys("host", "x-amz-additionalheader");
        assertThat(presigned.signedHeaders().get("x-amz-additionalheader")).containsExactly("foo1");
        assertThat(presigned.httpRequest().headers()).containsKeys("x-amz-additionalheader");
        assertThat(presigned.httpRequest().rawQueryParameters().get("additionalQueryParam").get(0)).isEqualTo("foo2");
    }

    @Test
    public void deleteObject_NonSigV4SignersRaisesException() {
        AwsRequestOverrideConfiguration override =
            AwsRequestOverrideConfiguration.builder()
                                           .signer(new NoOpSigner())
                                           .build();

        assertThatThrownBy(() -> presigner.presignDeleteObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                                                     .deleteObjectRequest(delo -> delo.bucket("foo34343434")
                                                                                                    .key("bar")
                                                                                                    .overrideConfiguration(override))))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("NoOpSigner");
    }

    @Test
    public void deleteObject_Sigv4PresignerHonorsSignatureDuration() {
        AwsRequestOverrideConfiguration override =
            AwsRequestOverrideConfiguration.builder()
                                           .signer(AwsS3V4Signer.create())
                                           .build();

        PresignedDeleteObjectRequest presigned =
            presigner.presignDeleteObject(r -> r.signatureDuration(Duration.ofSeconds(1234))
                                                .deleteObjectRequest(delo -> delo.bucket("a")
                                                                               .key("b")
                                                                               .overrideConfiguration(override)));

        assertThat(presigned.httpRequest().rawQueryParameters().get("X-Amz-Expires").get(0)).satisfies(expires -> {
            assertThat(expires).containsOnlyDigits();
            assertThat(Integer.parseInt(expires)).isCloseTo(1234, Offset.offset(2));
        });
    }

    @Test
    public void getObject_S3ConfigurationCanBeOverriddenToLeverageTransferAcceleration() {
        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                    .accelerateModeEnabled(true)
                    .build())
                .build();

        PresignedGetObjectRequest presignedRequest =
                presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(go -> go.bucket("foo34343434")
                                .key("bar")));

        assertThat(presignedRequest.httpRequest().host()).contains(".s3-accelerate.");
    }


    @Test
    public void accelerateEnabled_UsesVirtualAddressingWithAccelerateEndpoint() {
        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                .accelerateModeEnabled(true)
                .build())
                .build();

        PresignedGetObjectRequest presignedRequest =
                presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(go -> go.bucket(BUCKET)
                                .key("bar")));

        assertThat(presignedRequest.httpRequest().host()).isEqualTo(String.format("%s.s3-accelerate.amazonaws.com", BUCKET));
    }

    /**
     * Dualstack uses regional endpoints that support virtual addressing.
     */
    @Test
    public void dualstackEnabled_UsesVirtualAddressingWithDualstackEndpoint() throws Exception {
        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                .dualstackEnabled(true)
                .build())
                .build();

        PresignedGetObjectRequest presignedRequest =
                presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(go -> go.bucket(BUCKET)
                                .key("bar")));

        assertThat(presignedRequest.httpRequest().host()).contains(String.format("%s.s3.dualstack.us-west-2.amazonaws.com", BUCKET));
    }

    /**
     * Dualstack uses regional endpoints that support virtual addressing.
     */
    @Test
    public void dualstackEnabledViaBuilder_UsesVirtualAddressingWithDualstackEndpoint() throws Exception {
        S3Presigner presigner = presignerBuilder().dualstackEnabled(true).build();

        PresignedGetObjectRequest presignedRequest =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(go -> go.bucket(BUCKET)
                                                                       .key("bar")));

        assertThat(presignedRequest.httpRequest().host()).contains(String.format("%s.s3.dualstack.us-west-2.amazonaws.com", BUCKET));
    }

    /**
     * Dualstack also supports path style endpoints just like the normal endpoints.
     */
    @Test
    public void dualstackAndPathStyleEnabled_UsesPathStyleAddressingWithDualstackEndpoint() throws Exception {
        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                .dualstackEnabled(true)
                .pathStyleAccessEnabled(true)
                .build())
                .build();

        PresignedGetObjectRequest presignedRequest =
                presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(go -> go.bucket(BUCKET)
                                .key("bar")));

        assertThat(presignedRequest.httpRequest().host()).isEqualTo("s3.dualstack.us-west-2.amazonaws.com");
        assertThat(presignedRequest.url().toString()).startsWith(String.format("https://s3.dualstack.us-west-2.amazonaws.com/%s/%s?", BUCKET, "bar"));
    }

    /**
     * When dualstack and accelerate are both enabled there is a special, global dualstack endpoint we must use.
     */
    @Test
    public void dualstackAndAccelerateEnabled_UsesDualstackAccelerateEndpoint() throws Exception {
        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                .dualstackEnabled(true)
                .accelerateModeEnabled(true)
                .build())
                .build();

        PresignedGetObjectRequest presignedRequest =
                presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(go -> go.bucket(BUCKET)
                                .key("bar")));

        assertThat(presignedRequest.httpRequest().host()).isEqualTo(String.format("%s.s3-accelerate.dualstack.amazonaws.com", BUCKET));
    }

    @Test
    public void accessPointArn_differentRegion_useArnRegionTrue() throws Exception {
        String customEndpoint = "https://foobar-12345678910.s3-accesspoint.us-west-2.amazonaws.com";
        String accessPointArn = "arn:aws:s3:us-west-2:12345678910:accesspoint:foobar";

        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                .useArnRegionEnabled(true)
                .build())
                .build();

        PresignedGetObjectRequest presignedRequest =
                presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(go -> go.bucket(accessPointArn)
                                .key("bar")));

        assertThat(presignedRequest.url().toString()).startsWith(customEndpoint);
        assertThat(presignedRequest.httpRequest().rawQueryParameters().get("X-Amz-Algorithm").get(0))
            .isEqualTo(SignerConstant.AWS4_SIGNING_ALGORITHM);
    }

    @Test
    public void accessPointArn_differentRegion_useArnRegionFalse_throwsIllegalArgumentException() throws Exception {
        String accessPointArn = "arn:aws:s3:us-east-1:12345678910:accesspoint:foobar";

        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                .useArnRegionEnabled(false)
                .build())
                .build();

        assertThatThrownBy(() -> presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(go -> go.bucket(accessPointArn).key("bar"))))
                .isInstanceOf(SdkClientException.class)
                .hasMessageContaining("Invalid configuration: region from ARN `us-east-1` does not match client region `us-west-2` and UseArnRegion is `false`");
    }

    @Test
    public void accessPointArn_outpost_correctEndpoint() {
        String customEndpoint = "https://myaccesspoint-123456789012.op-01234567890123456.s3-outposts.us-west-2.amazonaws.com";
        String accessPointArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";

        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                       .useArnRegionEnabled(true)
                                                                                       .build())
                                                  .build();

        PresignedGetObjectRequest presignedRequest =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(go -> go.bucket(accessPointArn)
                                                                       .key("bar")));

        assertThat(presignedRequest.url().toString()).startsWith(customEndpoint);
    }

    @Test
    public void accessPointArn_outpost_differentRegion_useArnRegionTrue_correctEndpoint() {
        String customEndpoint = "https://myaccesspoint-123456789012.op-01234567890123456.s3-outposts.us-east-1.amazonaws.com";
        String accessPointArn = "arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";

        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                       .useArnRegionEnabled(true)
                                                                                       .build())
                                                  .build();

        PresignedGetObjectRequest presignedRequest =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(go -> go.bucket(accessPointArn)
                                                                       .key("bar")));

        assertThat(presignedRequest.url().toString()).startsWith(customEndpoint);
    }

    @Test
    public void accessPointArn_outpost_differentRegion_useArnRegionFalse_throwsIllegalArgumentException() {
        String accessPointArn = "arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";

        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                       .useArnRegionEnabled(false)
                                                                                       .build())
                                                  .build();

        assertThatThrownBy(() -> presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                                                  .getObjectRequest(go -> go.bucket(accessPointArn)
                                                                                            .key("bar"))))
            .isInstanceOf(SdkClientException.class)
            .hasMessageContaining("Invalid configuration: region from ARN `us-east-1` does not match client region `us-west-2` and UseArnRegion is `false`");
    }

    @Test
    public void accessPointArn_multiRegion_useArnRegionTrue_correctEndpointAndSigner() {
        String customEndpoint = "https://mfzwi23gnjvgw.mrap.accesspoint.s3-global.amazonaws.com";
        String accessPointArn = "arn:aws:s3::12345678910:accesspoint:mfzwi23gnjvgw.mrap";

        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                                                                                       .useArnRegionEnabled(true)
                                                                                       .build())
                                                  .build();

        PresignedGetObjectRequest presignedRequest =
            presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                                             .getObjectRequest(go -> go.bucket(accessPointArn)
                                                                       .key("bar")));

        assertThat(presignedRequest.httpRequest().rawQueryParameters().get("X-Amz-Algorithm").get(0))
            .isEqualTo("AWS4-ECDSA-P256-SHA256");
        assertThat(presignedRequest.url().toString()).startsWith(customEndpoint);
    }

    @Test
    public void outpostArn_usWest_calculatesCorrectSignature() {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(
            "ACCESS_KEY_ID", "SECRET_ACCESS_KEY"));

        String outpostArn = "arn:aws:s3-outposts:us-west-2:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";

        S3Presigner presigner = S3Presigner.builder()
                                           .region(Region.of("us-west-2"))
                                           .credentialsProvider(credentials)
                                           .serviceConfiguration(S3Configuration.builder()
                                                                                .useArnRegionEnabled(true)
                                                                                // Explicitly disable this because not doing so
                                                                                // will add a new header to the signature
                                                                                // calculation and it won't match the expected
                                                                                // signature
                                                                                .checksumValidationEnabled(false)
                                                                                .build())
                                           .build();

        Duration urlDuration = Duration.ofSeconds(900);
        ZonedDateTime signingDate = ZonedDateTime.of(2021, 8, 27, 0, 0, 0, 0, ZoneId.of("UTC"));
        Clock signingClock = Clock.fixed(signingDate.toInstant(), ZoneId.of("UTC"));
        Instant expirationTime = signingDate.toInstant().plus(urlDuration);

        GetObjectRequest getObject = GetObjectRequest.builder()
                                                     .bucket(outpostArn)
                                                     .key("obj")
                                                     .overrideConfiguration(o -> o.signer(new TestS3V4Signer(signingClock, expirationTime)))
                                                     .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(r -> r.getObjectRequest(getObject)
                                                                               // doesn't really do anything in this case since
                                                                               // we set it in TestSigner
                                                                               .signatureDuration(urlDuration));

        String expectedSignature = "a944fbe2bfbae429f922746546d1c6f890649c88ba7826bd1d258ac13f327e09";
        assertThat(presigned.url().toString()).contains("X-Amz-Signature=" + expectedSignature);
    }

    @Test
    public void outpostArn_usEast_calculatesCorrectSignature() {
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(AwsBasicCredentials.create(
            "ACCESS_KEY_ID", "SECRET_ACCESS_KEY"));

        String outpostArn = "arn:aws:s3-outposts:us-east-1:123456789012:outpost:op-01234567890123456:accesspoint:myaccesspoint";

        S3Presigner presigner = S3Presigner.builder()
                                           .region(Region.of("us-west-2"))
                                           .credentialsProvider(credentials)
                                           .serviceConfiguration(S3Configuration.builder()
                                                                                .useArnRegionEnabled(true)
                                                                                // Explicitly disable this because not doing so
                                                                                // will add a new header to the signature
                                                                                // calculation and it won't match the expected
                                                                                // signature
                                                                                .checksumValidationEnabled(false)
                                                                                .build())
                                           .build();

        Duration urlDuration = Duration.ofSeconds(900);
        ZonedDateTime signingDate = ZonedDateTime.of(2021, 8, 27, 0, 0, 0, 0, ZoneId.of("UTC"));
        Clock signingClock = Clock.fixed(signingDate.toInstant(), ZoneId.of("UTC"));
        Instant expirationTime = signingDate.toInstant().plus(urlDuration);

        GetObjectRequest getObject = GetObjectRequest.builder()
                                                     .bucket(outpostArn)
                                                     .key("obj")
                                                     .overrideConfiguration(o -> o.signer(new TestS3V4Signer(signingClock, expirationTime)))
                                                     .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(r -> r.getObjectRequest(getObject)
                                                                               // doesn't really do anything in this case since
                                                                               // we set it in TestSigner
                                                                               .signatureDuration(urlDuration));

        String expectedSignature = "7f93df0b81f80e590d95442d579bd6cf749a35ff4bbdc6373fa669b89c7fce4e";
        assertThat(presigned.url().toString()).contains("X-Amz-Signature=" + expectedSignature);
    }

    @Test(expected = IllegalStateException.class)
    public void dualstackInConfigAndPresignerBuilder_throwsException() throws Exception {
        presignerBuilder().serviceConfiguration(S3Configuration.builder()
                                                               .dualstackEnabled(true)
                                                               .build())
                          .dualstackEnabled(true)
                          .build();
    }

    @Test
    public void getObject_useEast1_regionalEndpointDisabled_usesGlobalEndpoint() {
        String settingSaveValue = System.getProperty(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property());
        System.setProperty(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property(), "global");
        try {
            S3Presigner usEast1Presigner = presignerBuilder().region(Region.US_EAST_1).build();
            URL presigned =
                usEast1Presigner.presignGetObject(r -> r.getObjectRequest(getRequest -> getRequest.bucket("foo").key("bar"))
                                                        .signatureDuration(Duration.ofHours(1)))
                                .url();
            assertThat(presigned.getHost()).isEqualTo("foo.s3.amazonaws.com");
        } finally {
            if (settingSaveValue != null) {
                System.setProperty(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property(), settingSaveValue);
            } else {
                System.clearProperty(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property());
            }
        }
    }

    @Test
    public void getObject_useEast1_regionalEndpointEnabled_usesRegionalEndpoint() {
        String settingSaveValue = System.getProperty(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property());
        System.setProperty(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property(), "regional");
        try {
            S3Presigner usEast1Presigner = presignerBuilder().region(Region.US_EAST_1).build();
            URL presigned =
                usEast1Presigner.presignGetObject(r -> r.getObjectRequest(getRequest -> getRequest.bucket("foo").key("bar"))
                                                        .signatureDuration(Duration.ofHours(1)))
                                .url();
            assertThat(presigned.getHost()).isEqualTo("foo.s3.us-east-1.amazonaws.com");
        } finally {
            if (settingSaveValue != null) {
                System.setProperty(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property(), settingSaveValue);
            } else {
                System.clearProperty(SdkSystemSetting.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property());
            }
        }
    }


    // Variant of AwsS3V4Signer that allows for changing the signing clock and expiration time
    public static class TestS3V4Signer extends AbstractAwsS3V4Signer {
        private final Clock signingClock;
        private final Instant expirationTime;

        public TestS3V4Signer(Clock signingClock, Instant expirationTime) {
            this.signingClock = signingClock;
            this.expirationTime = expirationTime;
        }

        // Intercept the presign() method so we can change the expiration
        @Override
        public SdkHttpFullRequest presign(SdkHttpFullRequest request, Aws4PresignerParams signingParams) {
            Aws4PresignerParams.Builder newSignerParamsBuilder = Aws4PresignerParams.builder();

            newSignerParamsBuilder.expirationTime(expirationTime);
            newSignerParamsBuilder.signingClockOverride(signingClock);
            newSignerParamsBuilder.signingRegion(signingParams.signingRegion());
            newSignerParamsBuilder.signingName(signingParams.signingName());
            newSignerParamsBuilder.awsCredentials(signingParams.awsCredentials());
            newSignerParamsBuilder.doubleUrlEncode(signingParams.doubleUrlEncode());

            return super.presign(request, newSignerParamsBuilder.build());
        }
    }
}