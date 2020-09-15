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

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.authcrt.signer.internal.CrtHttpUtils;
import software.amazon.awssdk.authcrt.signer.internal.SigningUtils;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.crt.auth.signing.AwsSigningConfig;
import software.amazon.awssdk.crt.auth.signing.AwsSigningUtils;
import software.amazon.awssdk.crt.http.HttpRequest;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.regions.Region;


/**
 * Unit tests for the {@link Aws4aSigner}.
 */
@RunWith(MockitoJUnitRunner.class)
public class Aws4aSignerTest {

    class Sigv4aSigningTestCase {
        public SdkHttpFullRequest.Builder requestBuilder;
        public String expectedCanonicalRequest;

        public String signingName;
        public String regionSet;
        public Instant signingTime;
        public AwsBasicCredentials credentials;
    };

    private Aws4aSigner v4aSigner = Aws4aSigner.create();

    boolean verifyEcdsaSignature(SdkHttpFullRequest request, String expectedCanonicalRequest, AwsSigningConfig signingConfig, String signatureValue) {
        HttpRequest crtRequest = CrtHttpUtils.createCrtRequest(SigningUtils.sanitizeSdkRequestForCrtSigning(request));

        return AwsSigningUtils.verifySigv4aEcdsaSignature(crtRequest, expectedCanonicalRequest, signingConfig, signatureValue);
    }

    private Sigv4aSigningTestCase createBasicHeaderSigningTestCase() {
        Sigv4aSigningTestCase testCase = new Sigv4aSigningTestCase();

        testCase.requestBuilder = SdkHttpFullRequest.builder()
                .contentStreamProvider(() -> new ByteArrayInputStream("{\"TableName\": \"foo\"}".getBytes()))
                .method(SdkHttpMethod.POST)
                .putHeader("Host", "demo.us-east-1.amazonaws.com")
                .putHeader("x-amz-archive-description", "test  test")
                .encodedPath("/")
                .uri(URI.create("http://demo.us-east-1.amazonaws.com"));

        testCase.signingName = "demo";
        testCase.regionSet = "aws-global";
        testCase.signingTime = Instant.ofEpochSecond(1596476903);
        testCase.credentials = AwsBasicCredentials.create("access", "secret");
        testCase.expectedCanonicalRequest = "POST\n" +
                "/\n" +
                "\n" +
                "host:demo.us-east-1.amazonaws.com\n" +
                "x-amz-archive-description:test test\n" +
                "x-amz-content-sha256:a15c8292b1d12abbbbe4148605f7872fbdf645618fee5ab0e8072a7b34f155e2\n" +
                "x-amz-date:20200803T174823Z\n" +
                "x-amz-region-set:aws-global\n" +
                "\n" +
                "host;x-amz-archive-description;x-amz-content-sha256;x-amz-date;x-amz-region-set\n" +
                "a15c8292b1d12abbbbe4148605f7872fbdf645618fee5ab0e8072a7b34f155e2";

        return testCase;
    }

    private ExecutionAttributes buildBasicExecutionAttributes(Sigv4aSigningTestCase testCase) {
        ExecutionAttributes executionAttributes = new ExecutionAttributes();
        executionAttributes.putAttribute(Aws4aSigner.SIGNING_CLOCK, Clock.fixed(testCase.signingTime, ZoneId.systemDefault()));
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, testCase.signingName);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, Region.AWS_GLOBAL);
        executionAttributes.putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, testCase.credentials);

        return executionAttributes;
    }

    @Test
    public void testHeaderSigning() {

        Sigv4aSigningTestCase testCase = createBasicHeaderSigningTestCase();

        ExecutionAttributes executionAttributes = buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest request = testCase.requestBuilder.build();
        SdkHttpFullRequest signed = v4aSigner.sign(request, executionAttributes);

        String authHeader = signed.firstMatchingHeader("Authorization").get();
        String signatureKey = "Signature=";
        String signatureValue = authHeader.substring(authHeader.indexOf(signatureKey) + signatureKey.length());

        AwsSigningConfig signingConfig = v4aSigner.createCrtSigningConfig(request, executionAttributes);

        assertTrue(verifyEcdsaSignature(request, testCase.expectedCanonicalRequest, signingConfig, signatureValue));
    }

    private Sigv4aSigningTestCase createBasicQuerySigningTestCase() {
        Sigv4aSigningTestCase testCase = new Sigv4aSigningTestCase();

        testCase.requestBuilder = SdkHttpFullRequest.builder()
                .method(SdkHttpMethod.GET)
                .putHeader("Host", "testing.us-east-1.amazonaws.com")
                .encodedPath("/test%20path/help")
                .uri(URI.create("http://testing.us-east-1.amazonaws.com"));
        testCase.signingName = "testing";
        testCase.regionSet = "aws-global";
        testCase.signingTime = Instant.ofEpochSecond(1596476801);
        testCase.credentials = AwsBasicCredentials.create("QueryAccess", "QuerySecret");
        testCase.expectedCanonicalRequest = "GET\n" +
                "/test%2520path/help\n" +
                "X-Amz-Algorithm=AWS4-ECDSA-P256-SHA256&X-Amz-Credential=QueryAccess%2F20200803%2Ftesting%2Faws4_request&X-Amz-Date=20200803T174641Z&X-Amz-Expires=604800&X-Amz-Region-Set=aws-global&X-Amz-SignedHeaders=host%3Bx-amz-content-sha256\n" +
                "host:testing.us-east-1.amazonaws.com\n" +
                "x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\n" +
                "\n" +
                "host;x-amz-content-sha256\n" +
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        return testCase;
    }

    @Test
    public void testQuerySigning() {

        Sigv4aSigningTestCase testCase = createBasicQuerySigningTestCase();

        ExecutionAttributes executionAttributes = buildBasicExecutionAttributes(testCase);

        SdkHttpFullRequest request = testCase.requestBuilder.build();
        SdkHttpFullRequest signed = v4aSigner.presign(request, executionAttributes);

        List<String> signatureValues = signed.rawQueryParameters().get("X-Amz-Signature");
        String signatureValue = signatureValues.get(0);

        AwsSigningConfig signingConfig = v4aSigner.createCrtPreSigningConfig(request, executionAttributes);

        assertTrue(verifyEcdsaSignature(request, testCase.expectedCanonicalRequest, signingConfig, signatureValue));
    }
}
