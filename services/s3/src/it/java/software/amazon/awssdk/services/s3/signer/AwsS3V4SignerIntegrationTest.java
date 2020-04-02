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

package software.amazon.awssdk.services.s3.signer;

import static org.junit.Assert.assertEquals;
import static software.amazon.awssdk.core.client.config.SdkAdvancedClientOption.SIGNER;
import static software.amazon.awssdk.testutils.service.S3BucketUtils.temporaryBucketName;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.signer.Aws4Signer;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.auth.signer.AwsSignerExecutionAttribute;
import software.amazon.awssdk.auth.signer.params.Aws4PresignerParams;
import software.amazon.awssdk.auth.signer.params.AwsS3V4SignerParams;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.HttpExecuteResponse;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3IntegrationTestBase;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.utils.IoUtils;

public class AwsS3V4SignerIntegrationTest extends S3IntegrationTestBase {

    private static final AwsCredentials awsCredentials = CREDENTIALS_PROVIDER_CHAIN.resolveCredentials();
    private static final String SIGNING_NAME = "s3";
    private static final String BUCKET_NAME = temporaryBucketName("s3-signer-integ-test");
    private static final String KEY = "test-key";
    private static final String CONTENT = "Hello world";

    @BeforeClass
    public static void setup() {
        createBucket(BUCKET_NAME);
        s3.putObject(PutObjectRequest.builder()
                                     .bucket(BUCKET_NAME)
                                     .key(KEY)
                                     .build(),
                     RequestBody.fromString(CONTENT));
    }

    @AfterClass
    public static void cleanUp() {
        deleteBucketAndAllContents(BUCKET_NAME);
    }

    @Test
    public void testGetObject() {
        String response = s3.getObjectAsBytes(req -> req.bucket(BUCKET_NAME).key(KEY))
                            .asString(StandardCharsets.UTF_8);

        assertEquals(CONTENT, response);
    }

    @Test (expected = S3Exception.class)
    public void test_UsingSdkClient_WithIncorrectSigner_SetInConfig() {
        S3Client customClient = getClientBuilder()
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                              .putAdvancedOption(SIGNER, Aws4Signer.create())
                                                              .build())
            .build();

       customClient.getObjectAsBytes(req -> req.bucket(BUCKET_NAME).key(KEY))
                   .asString(StandardCharsets.UTF_8);
    }

    @Test
    public void test_UsingSdkClient_WithCorrectSigner_SetInConfig() {
        S3Client customClient = getClientBuilder()
            .overrideConfiguration(ClientOverrideConfiguration.builder()
                                                              .putAdvancedOption(SIGNER, AwsS3V4Signer.create())
                                                              .build())
            .build();

        String response = customClient.getObjectAsBytes(req -> req.bucket(BUCKET_NAME).key(KEY))
                                      .asString(StandardCharsets.UTF_8);

        assertEquals(CONTENT, response);
    }

    @Test
    public void test_SignMethod_WithModeledParam_And_WithoutUsingSdkClient() throws Exception {
        AwsS3V4Signer signer = AwsS3V4Signer.create();
        SdkHttpFullRequest httpFullRequest = generateBasicGetRequest();

        // sign the request
        SdkHttpFullRequest signedRequest = signer.sign(httpFullRequest, constructSignerParams());

        SdkHttpClient httpClient = ApacheHttpClient.builder().build();

        HttpExecuteResponse response = httpClient.prepareRequest(HttpExecuteRequest.builder().request(signedRequest).build())
                                                 .call();

        assertEquals("Non success http status code", 200, response.httpResponse().statusCode());

        String actualResult = IoUtils.toUtf8String(response.responseBody().get());
        assertEquals(CONTENT, actualResult);
    }

    @Test
    public void test_SignMethod_WithExecutionAttributes_And_WithoutUsingSdkClient() throws Exception {
        AwsS3V4Signer signer = AwsS3V4Signer.create();
        SdkHttpFullRequest httpFullRequest = generateBasicGetRequest();

        // sign the request
        SdkHttpFullRequest signedRequest = signer.sign(httpFullRequest, constructExecutionAttributes());

        SdkHttpClient httpClient = ApacheHttpClient.builder().build();

        HttpExecuteResponse response = httpClient.prepareRequest(HttpExecuteRequest.builder().request(signedRequest).build())
                                                 .call();

        assertEquals("Non success http status code", 200, response.httpResponse().statusCode());

        String actualResult = IoUtils.toUtf8String(response.responseBody().get());
        assertEquals(CONTENT, actualResult);
    }


    @Test
    public void testPresigning() throws MalformedURLException {
        AwsS3V4Signer signer = AwsS3V4Signer.create();
        SdkHttpFullRequest httpFullRequest = generateBasicGetRequest();

        SdkHttpFullRequest signedRequest = signer.presign(httpFullRequest, constructPresignerParams());
        URL presignedUri = signedRequest.getUri().toURL();

        assertEquals(CONTENT, getContentFromPresignedUrl(presignedUri));
    }

    private String getContentFromPresignedUrl(URL url) {
        HttpURLConnection httpConn = null;
        try {
            httpConn = (HttpURLConnection) url.openConnection();
            int responseCode = httpConn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = httpConn.getInputStream();
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            } else {
                throw new RuntimeException("No file to download. Server replied HTTP code: " + responseCode);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    private SdkHttpFullRequest generateBasicGetRequest() {
        return SdkHttpFullRequest.builder()
                                 .method(SdkHttpMethod.GET)
                                 .protocol("https")
                                 .host(getHost())
                                 .encodedPath(getPath())
                                 .build();
    }

    private String getHost() {
        return String.format("%s.s3-%s.amazonaws.com", BUCKET_NAME, DEFAULT_REGION.id());
    }

    private String getPath() {
        return String.format("/%s", KEY);
    }

    private AwsS3V4SignerParams constructSignerParams() {
        return AwsS3V4SignerParams.builder()
                                  .doubleUrlEncode(Boolean.FALSE)
                                  .awsCredentials(awsCredentials)
                                  .signingName(SIGNING_NAME)
                                  .signingRegion(DEFAULT_REGION)
                                  .build();
    }

    private Aws4PresignerParams constructPresignerParams() {
        return Aws4PresignerParams.builder()
                                  .doubleUrlEncode(Boolean.FALSE)
                                  .awsCredentials(awsCredentials)
                                  .signingName(SIGNING_NAME)
                                  .signingRegion(DEFAULT_REGION)
                                  .build();
    }

    private ExecutionAttributes constructExecutionAttributes() {
        return new ExecutionAttributes()
            .putAttribute(AwsSignerExecutionAttribute.AWS_CREDENTIALS, awsCredentials)
            .putAttribute(AwsSignerExecutionAttribute.SERVICE_SIGNING_NAME, SIGNING_NAME)
            .putAttribute(AwsSignerExecutionAttribute.SIGNING_REGION, DEFAULT_REGION);
    }

    private static S3ClientBuilder getClientBuilder() {
        return S3Client.builder()
                       .region(DEFAULT_REGION)
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN);
    }
}
