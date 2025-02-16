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

package software.amazon.awssdk.http.auth.aws.crt;

import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.REGION_SET;
import static software.amazon.awssdk.http.auth.aws.signer.AwsV4aHttpSigner.SERVICE_SIGNING_NAME;
import static software.amazon.awssdk.http.auth.aws.TestUtils.TickingClock;
import static software.amazon.awssdk.http.auth.aws.crt.internal.util.CrtHttpRequestConverter.toRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.util.CrtUtils.sanitizeRequest;
import static software.amazon.awssdk.http.auth.aws.crt.internal.util.CrtUtils.toCredentials;
import static software.amazon.awssdk.http.auth.spi.signer.HttpSigner.SIGNING_CLOCK;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Instant;
import java.util.function.Consumer;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningUtils;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.RegionSet;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;

public final class TestUtils {

    private static final String TEST_VERIFICATION_PUB_X = "b6618f6a65740a99e650b33b6b4b5bd0d43b176d721a3edfea7e7d2d56d936b1";
    private static final String TEST_VERIFICATION_PUB_Y = "865ed22a7eadc9c5cb9d2cbaca1b3699139fedc5043dc6661864218330c8e518";

    private TestUtils() {
    }

    // Helpers for generating test requests
    public static <T extends AwsCredentialsIdentity> SignRequest<T> generateBasicRequest(
        T credentials,
        Consumer<? super SdkHttpRequest.Builder> requestOverrides,
        Consumer<? super SignRequest.Builder<T>> signRequestOverrides
    ) {
        return SignRequest.builder(credentials)
                          .request(SdkHttpRequest.builder()
                                                     .method(SdkHttpMethod.POST)
                                                     .putHeader("x-amz-archive-description", "test  test")
                                                     .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                                     .encodedPath("/")
                                                     .uri(URI.create("https://demo.us-east-1.amazonaws.com"))
                                                     .build()
                                                     .copy(requestOverrides))
                          .payload(() -> new ByteArrayInputStream("Hello world".getBytes()))
                          .putProperty(REGION_SET, RegionSet.create("aws-global"))
                          .putProperty(SERVICE_SIGNING_NAME, "demo")
                          .putProperty(SIGNING_CLOCK, new TickingClock(Instant.ofEpochMilli(1596476903000L)))
                          .build()
                          .copy(signRequestOverrides);
    }

    public static AwsSigningConfig generateBasicSigningConfig(AwsCredentialsIdentity credentials) {
        try (AwsSigningConfig signingConfig = new AwsSigningConfig()) {
            signingConfig.setCredentials(toCredentials(credentials));
            signingConfig.setService("demo");
            signingConfig.setRegion("aws-global");
            signingConfig.setAlgorithm(AwsSigningConfig.AwsSigningAlgorithm.SIGV4_ASYMMETRIC);
            signingConfig.setTime(1596476903000L);
            signingConfig.setUseDoubleUriEncode(true);
            signingConfig.setShouldNormalizeUriPath(true);
            return signingConfig;
        }
    }

    public static boolean verifyEcdsaSignature(SdkHttpRequest request,
                                               ContentStreamProvider payload,
                                               String expectedCanonicalRequest,
                                               AwsSigningConfig signingConfig,
                                               String signatureValue) {
        HttpRequest crtRequest = toRequest(sanitizeRequest(request), payload);

        return AwsSigningUtils.verifySigv4aEcdsaSignature(crtRequest, expectedCanonicalRequest, signingConfig,
                                                          signatureValue.getBytes(), TEST_VERIFICATION_PUB_X,
                                                          TEST_VERIFICATION_PUB_Y);
    }
}
