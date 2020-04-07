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
import java.time.Duration;
import org.assertj.core.data.Offset;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.signer.NoOpSigner;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.RequestPayer;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
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

        assertThat(presigned.url().toString()).startsWith("http://foo.com/foo34343434/bar?");
        assertThat(presigned.isBrowserExecutable()).isTrue();
        assertThat(presigned.signedHeaders().get("host")).containsExactly("foo.com");
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

        System.out.println(presignedWithClientCredentials.url());

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

        assertThat(presigned.url().toString()).startsWith("http://foo.com/foo34343434/bar?");
        assertThat(presigned.isBrowserExecutable()).isFalse();
        assertThat(presigned.signedHeaders().get("host")).containsExactly("foo.com");
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

        System.out.println(presignedWithClientCredentials.url());

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
    public void getObject_S3ConfigurationCanBeOverriddenToLeverageTransferAcceleration() {
        S3Presigner presigner = presignerBuilder().serviceConfiguration(S3Configuration.builder()
                    .accelerateModeEnabled(true)
                    .build())
                .build();

        PresignedGetObjectRequest presignedRequest =
                presigner.presignGetObject(r -> r.signatureDuration(Duration.ofMinutes(5))
                        .getObjectRequest(go -> go.bucket("foo34343434")
                                .key("bar")));


        System.out.println(presignedRequest.url());

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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("region");
    }
}