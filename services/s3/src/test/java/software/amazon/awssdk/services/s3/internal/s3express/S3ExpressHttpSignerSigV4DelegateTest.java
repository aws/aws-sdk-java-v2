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

package software.amazon.awssdk.services.s3.internal.s3express;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner.REGION_NAME;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.services.s3.s3express.S3ExpressSessionCredentials;
import software.amazon.awssdk.utils.StringInputStream;

class S3ExpressHttpSignerSigV4DelegateTest {

    private HttpSigner<S3ExpressSessionCredentials> signer;

    @BeforeEach
    void init() {
        signer = DefaultS3ExpressHttpSigner.create(AwsV4HttpSigner.create());
    }

    @Test
    void signSync_shouldSucceed() {
        ContentStreamProvider payload = () -> new StringInputStream("this-is\nthe-sync-full-request-body");
        SignRequest<S3ExpressSessionCredentials> signRequest =
            SignRequest.builder(S3ExpressSessionCredentials
                                    .create("access-key", "secret", "session-token"))
                       .request(SdkHttpFullRequest.builder()
                                                  .protocol("https")
                                                  .host("sync.dummy.host")
                                                  .method(SdkHttpMethod.GET)
                                                  .contentStreamProvider(payload)
                                                  .build())
                       .payload(payload)
                       .putProperty(REGION_NAME, "us-west-2")
                       .putProperty(SERVICE_SIGNING_NAME, "test-dummy")
                       .build();
        SignedRequest signedRequest = signer.sign(signRequest);
        assertThat(signedRequest.request().firstMatchingHeader("x-amz-s3session-token"))
            .isNotEmpty()
            .contains("session-token");
        assertThat(signedRequest.payload()).isPresent();
        signedRequest.payload().ifPresent(
            c -> assertThat(c.newStream()).hasSameContentAs(new StringInputStream("this-is\nthe-sync-full-request-body")));

        SdkHttpFullRequest httpRequest = (SdkHttpFullRequest) signedRequest.request();
        assertThat(httpRequest.protocol()).isEqualTo("https");
        assertThat(httpRequest.host()).isEqualTo("sync.dummy.host");
        assertThat(httpRequest.method()).isEqualTo(SdkHttpMethod.GET);
        assertThat(httpRequest.contentStreamProvider()).isPresent();
        httpRequest.contentStreamProvider().ifPresent(
            c -> assertThat(c.newStream()).hasSameContentAs(new StringInputStream("this-is\nthe-sync-full-request-body")));
    }

    @Test
    void signAsync_missingProperty_shouldThrowException() {
        ContentStreamProvider payload = () -> new StringInputStream("this-is\nthe-sync-full-request-body");
        SignRequest<S3ExpressSessionCredentials> signRequest =
            SignRequest.builder(S3ExpressSessionCredentials
                                    .create("access-key", "secret", "session-token"))
                       .request(SdkHttpFullRequest.builder()
                                                  .protocol("https")
                                                  .host("sync.dummy.host")
                                                  .method(SdkHttpMethod.GET)
                                                  .contentStreamProvider(payload)
                                                  .build())
                       .payload(payload)
                       // missing REGION_NAME property
                       .putProperty(SERVICE_SIGNING_NAME, "test-dummy")
                       .build();
        assertThatThrownBy(() -> signer.sign(signRequest))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("RegionName");
    }
}
