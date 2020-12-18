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

package software.amazon.awssdk.authcrt.signer;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.authcrt.signer.internal.SigningUtils.SIGNING_CLOCK;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.internal.CrtHttpRequestConverter;
import software.amazon.awssdk.authcrt.signer.internal.SigningUtils;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningUtils;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;

public class SignerTestUtils {

    private static final String TEST_ACCESS_KEY_ID = "AKIDEXAMPLE";
    private static final String TEST_SECRET_ACCESS_KEY = "wJalrXUtnFEMI/K7MDENG+bPxRfiCYEXAMPLEKEY";
    private static final String TEST_VERIFICATION_PUB_X = "b6618f6a65740a99e650b33b6b4b5bd0d43b176d721a3edfea7e7d2d56d936b1";
    private static final String TEST_VERIFICATION_PUB_Y = "865ed22a7eadc9c5cb9d2cbaca1b3699139fedc5043dc6661864218330c8e518";

    private static final String AUTH_SIGNED_HEADER_KEY = "SignedHeaders=";
    private static final String AUTH_SIGNATURE_KEY = "Signature=";

    public static boolean verifyEcdsaSignature(SdkHttpFullRequest request,
                                 String expectedCanonicalRequest,
                                 AwsSigningConfig signingConfig,
                                 String signatureValue) {
        CrtHttpRequestConverter requestConverter = new CrtHttpRequestConverter();
        HttpRequest crtRequest = requestConverter.requestToCrt(SigningUtils.sanitizeSdkRequestForCrtSigning(request));

        return AwsSigningUtils.verifySigv4aEcdsaSignature(crtRequest, expectedCanonicalRequest, signingConfig,
                                                          signatureValue.getBytes(), TEST_VERIFICATION_PUB_X, TEST_VERIFICATION_PUB_Y);

    }

    public static AwsBasicCredentials createCredentials() {
        return AwsBasicCredentials.create(TEST_ACCESS_KEY_ID, TEST_SECRET_ACCESS_KEY);
    }

    public static ExecutionAttributes buildBasicExecutionAttributes(SigningTestCase testCase) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(SIGNING_CLOCK, Clock.fixed(testCase.signingTime, ZoneId.systemDefault()));
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, testCase.signingName);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, Region.AWS_GLOBAL);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, testCase.credentials);

        return executionAttributes;
    }

    public static SigningTestCase createBasicHeaderSigningTestCase() {
        SigningTestCase testCase = new SigningTestCase();

        String data = "{\"TableName\": \"foo\"}";

        testCase.requestBuilder = createHttpPostRequest(data);
        testCase.signingName = "demo";
        testCase.regionSet = "aws-global";
        testCase.signingTime = Instant.ofEpochSecond(1596476903);
        testCase.credentials = createCredentials();
        testCase.expectedCanonicalRequest = "POST\n" +
                                            "/\n" +
                                            "\n" +
                                            "host:demo.us-east-1.amazonaws.com\n" +
                                            "x-amz-archive-description:test test\n" +
                                            "x-amz-date:20200803T174823Z\n" +
                                            "x-amz-region-set:aws-global\n" +
                                            "\n" +
                                            "host;x-amz-archive-description;x-amz-date;x-amz-region-set\n" +
                                            "a15c8292b1d12abbbbe4148605f7872fbdf645618fee5ab0e8072a7b34f155e2";

        return testCase;
    }

    public static SigningTestCase createBasicChunkedSigningTestCase() {
        SigningTestCase testCase = new SigningTestCase();

        String data = "{\"TableName\": \"foo\"}";

        testCase.requestBuilder = createHttpPostRequest(data);
        testCase.signingName = "demo";
        testCase.regionSet = "aws-global";
        testCase.signingTime = Instant.ofEpochSecond(1596476903);
        testCase.credentials = createCredentials();
        testCase.expectedCanonicalRequest = "POST\n" +
                                            "/\n" +
                                            "\n" +
                                            "host:demo.us-east-1.amazonaws.com\n" +
                                            "x-amz-archive-description:test test\n" +
                                            "x-amz-date:20200803T174823Z\n" +
                                            "x-amz-decoded-content-length\n" +
                                            "x-amz-region-set:aws-global\n" +
                                            "\n" +
                                            "host;x-amz-archive-description;x-amz-date;x-amz-decoded-content-length;x-amz-region-set\n" +
                                            "";

        return testCase;
    }

    public static SigningTestCase createBasicQuerySigningTestCase() {
        SigningTestCase testCase = new SigningTestCase();

        testCase.requestBuilder = SdkHttpFullRequest.builder()
                                                    .method(SdkHttpMethod.GET)
                                                    .putHeader("Host", "testing.us-east-1.amazonaws.com")
                                                    .encodedPath("/test%20path/help")
                                                    .uri(URI.create("http://testing.us-east-1.amazonaws.com"));
        testCase.signingName = "testing";
        testCase.regionSet = "aws-global";
        testCase.signingTime = Instant.ofEpochSecond(1596476801);
        testCase.credentials = createCredentials();
        testCase.expectedCanonicalRequest = "GET\n" +
                                            "/test%2520path/help\n" +
                                            "X-Amz-Algorithm=AWS4-ECDSA-P256-SHA256&X-Amz-Credential=AKIDEXAMPLE%2F20200803%2Ftesting%2Faws4_request&X-Amz-Date=20200803T174641Z&X-Amz-Expires=604800&X-Amz-Region-Set=aws-global&X-Amz-SignedHeaders=host\n" +
                                            "host:testing.us-east-1.amazonaws.com\n" +
                                            "\n" +
                                            "host\n" +
                                            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        testCase.expectedS3PresignCanonicalRequest = "GET\n" +
                                                     "/test%2520path/help\n" +
                                                     "X-Amz-Algorithm=AWS4-ECDSA-P256-SHA256&X-Amz-Credential=AKIDEXAMPLE%2F20200803%2Ftesting%2Faws4_request&X-Amz-Date=20200803T174641Z&X-Amz-Expires=604800&X-Amz-Region-Set=aws-global&X-Amz-SignedHeaders=host\n" +
                                                     "host:testing.us-east-1.amazonaws.com\n" +
                                                     "\n" +
                                                     "host\n" +
                                                     "UNSIGNED-PAYLOAD";

        return testCase;
    }

    public static String extractSignatureFromAuthHeader(SdkHttpFullRequest signedRequest) {
        String authHeader = signedRequest.firstMatchingHeader("Authorization").get();
        return authHeader.substring(authHeader.indexOf(AUTH_SIGNATURE_KEY) + AUTH_SIGNATURE_KEY.length());
    }

    public static List<String> extractSignedHeadersFromAuthHeader(SdkHttpFullRequest signedRequest) {
        String authHeader = signedRequest.firstMatchingHeader("Authorization").get();
        String headers = authHeader.substring(authHeader.indexOf(AUTH_SIGNED_HEADER_KEY) + AUTH_SIGNED_HEADER_KEY.length(),
                                              authHeader.indexOf(AUTH_SIGNATURE_KEY) - 1);
        return Arrays.asList(headers.split(";"));
    }

    public static SdkHttpFullRequest.Builder createHttpPostRequest(String data) {
        return SdkHttpFullRequest.builder()
                                 .contentStreamProvider(() -> new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)))
                                 .method(SdkHttpMethod.POST)
                                 .putHeader("x-amz-archive-description", "test  test")
                                 .putHeader("Host", "demo.us-east-1.amazonaws.com")
                                 .encodedPath("/")
                                 .uri(URI.create("https://demo.us-east-1.amazonaws.com"));
    }

    public static SdkHttpFullRequest createSignedHttpRequest(String data) {
        SdkHttpFullRequest.Builder requestBuilder = createHttpPostRequest(data);
        requestBuilder.putHeader("Authorization", "AWS4-ECDSA-P256-SHA256 Credential=AKIDEXAMPLE/20200803/demo/aws4_request, SignedHeaders=host;x-amz-archive-description;x-amz-date;x-amz-region-set, Signature=304502201ee492c60af1667b9c1adfbafb4dfebedca45ed7f9c17711bc73bd2c0ebdbb4b022100e1108c7749acf67bb8c2e5fcf11f751fd86f8fde9bd646a47b4897023ca348d9");
        requestBuilder.putHeader("X-Amz-Date", "20200803T174823Z");
        requestBuilder.putHeader("X-Amz-Region-Set", "aws-global");
        return requestBuilder.build();
    }

    public static SdkHttpFullRequest createSignedPayloadHttpRequest(String data) {
        SdkHttpFullRequest.Builder requestBuilder = createHttpPostRequest(data);
        requestBuilder.putHeader("Authorization", "AWS4-ECDSA-P256-SHA256 Credential=AKIDEXAMPLE/20200803/demo/aws4_request, SignedHeaders=content-length;host;x-amz-archive-description;x-amz-content-sha256;x-amz-date;x-amz-decoded-content-length;x-amz-region-set, Signature=3046022100e3594ebc9ddfe327ca5127bbce72dd2b72965c33df36e529996edff1d7b59811022100e34cb9a2a68e82f6ac86e3359a758c546cdfb59807207dc6ebfedb44abbc4ca7");
        requestBuilder.putHeader("X-Amz-Date", "20200803T174823Z");
        requestBuilder.putHeader("X-Amz-Region-Set", "aws-global");
        requestBuilder.putHeader("x-amz-decoded-content-length", "20");
        requestBuilder.putHeader("Content-Length", "353");
        return requestBuilder.build();
    }

    public static SdkHttpFullRequest createPresignedPayloadHttpRequest(String data) {
        SdkHttpFullRequest.Builder requestBuilder = createHttpPostRequest(data);
        requestBuilder.putRawQueryParameter("X-Amz-Signature", "signature");
        return requestBuilder.build();
    }

}
