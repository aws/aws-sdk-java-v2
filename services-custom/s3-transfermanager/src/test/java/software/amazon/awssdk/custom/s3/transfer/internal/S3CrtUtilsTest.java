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

package software.amazon.awssdk.custom.s3.transfer.internal;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.s3.model.GetObjectOutput;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.signer.AwsS3V4Signer;
import software.amazon.awssdk.awscore.AwsRequestOverrideConfiguration;
import software.amazon.awssdk.core.ApiName;
import software.amazon.awssdk.core.util.SdkUserAgent;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;
import software.amazon.awssdk.crt.http.HttpHeader;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.metrics.LoggingMetricPublisher;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

public class S3CrtUtilsTest {

    private static final String ACCESS_KEY = "accessKey";
    private static final String SECRET_ACCESS_KEY = "secretAccessKey";
    private static final String SESSION_TOKEN = "sessionToken";

    @Test
    public void createCrtCredentialsProviderTest() throws ExecutionException, InterruptedException {
        AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider
            .create(AwsSessionCredentials.create(ACCESS_KEY, SECRET_ACCESS_KEY, SESSION_TOKEN));
        CredentialsProvider crtCredentialsProvider = S3CrtUtils.createCrtCredentialsProvider(awsCredentialsProvider);

        Credentials credentials = crtCredentialsProvider.getCredentials().get();

        assertThat(ACCESS_KEY.getBytes(StandardCharsets.UTF_8)).isEqualTo(credentials.getAccessKeyId());
        assertThat(SECRET_ACCESS_KEY.getBytes(StandardCharsets.UTF_8)).isEqualTo(credentials.getSecretAccessKey());
        assertThat(SESSION_TOKEN.getBytes(StandardCharsets.UTF_8)).isEqualTo(credentials.getSessionToken());
    }

    @Test
    public void adaptGetObjectOutput() {
        String expectedRequestId = "123456";
        GetObjectOutput output = GetObjectOutput.builder().build();
        SdkHttpResponse response = SdkHttpResponse.builder()
                                                  .statusCode(200)
                                                  .appendHeader("x-amz-request-id", expectedRequestId)
                                                  .build();


        GetObjectResponse getObjectResponse = S3CrtUtils.adaptGetObjectOutput(output, response);
        assertThat(output).isEqualToIgnoringGivenFields(getObjectResponse, "body",
                                                        "sSECustomerAlgorithm",
                                                        "sSECustomerKeyMD5",
                                                        "sSEKMSKeyId",
                                                        "metadata");

        assertThat(getObjectResponse.sdkHttpResponse()).isEqualTo(response);
        assertThat(getObjectResponse.responseMetadata().requestId()).isEqualTo(expectedRequestId);
    }

    @Test
    public void toCrtPutObjectRequest_shouldAddUserAgent() {

        PutObjectRequest sdkRequest = PutObjectRequest.builder()
                                                      .build();

        com.amazonaws.s3.model.PutObjectRequest crtRequest = S3CrtUtils.toCrtPutObjectRequest(sdkRequest);
        HttpHeader[] headers = crtRequest.customHeaders();
        verifyUserAgent(headers);
    }

    @Test
    public void toCrtPutObjectRequest_withCustomHeaders_shouldAttach() {

        AwsRequestOverrideConfiguration requestOverrideConfiguration = requestOverrideConfigWithCustomHeaders();

        PutObjectRequest sdkRequest = PutObjectRequest.builder()
                                                      .overrideConfiguration(requestOverrideConfiguration)
                                                      .build();

        com.amazonaws.s3.model.PutObjectRequest crtRequest = S3CrtUtils.toCrtPutObjectRequest(sdkRequest);
        HttpHeader[] headers = crtRequest.customHeaders();
        verifyHeaders(headers);
        assertThat(crtRequest.customQueryParameters()).isEqualTo("?hello1=world1&hello2=world2");
    }

    @Test
    public void toCrtGetObjectRequest_shouldAddUserAgent() {
        GetObjectRequest sdkRequest = GetObjectRequest.builder()
                                                      .build();

        com.amazonaws.s3.model.GetObjectRequest crtRequest = S3CrtUtils.toCrtGetObjectRequest(sdkRequest);

        HttpHeader[] headers = crtRequest.customHeaders();
        verifyUserAgent(headers);
    }

    @Test
    public void toCrtGetObjectRequest_withCustomHeaders_shouldAttach() {
        AwsRequestOverrideConfiguration requestOverrideConfiguration = requestOverrideConfigWithCustomHeaders();

        GetObjectRequest sdkRequest = GetObjectRequest.builder()
                                                      .overrideConfiguration(requestOverrideConfiguration)
                                                      .build();

        com.amazonaws.s3.model.GetObjectRequest crtRequest = S3CrtUtils.toCrtGetObjectRequest(sdkRequest);

        HttpHeader[] headers = crtRequest.customHeaders();
        verifyHeaders(headers);
        assertThat(crtRequest.customQueryParameters()).isEqualTo("?hello1=world1&hello2=world2");
    }

    @Test
    public void toCrtPutObjectRequest_withUnsupportedConfigs_shouldThrowException() {
        assertThatThrownBy(() -> S3CrtUtils.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.apiCallAttemptTimeout(Duration.ofMinutes(1)))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtUtils.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(1)))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtUtils.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.credentialsProvider(() ->
                                                                                                                                        AwsBasicCredentials.create("", "")))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtUtils.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.addApiName(ApiName.builder()
                                                                                                                                  .name("test")
                                                                                                                                  .version("1")
                                                                                                                                  .build()))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtUtils.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.addMetricPublisher(LoggingMetricPublisher.create()))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> S3CrtUtils.toCrtPutObjectRequest(PutObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.signer(AwsS3V4Signer.create()))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void toCrtGetObjectRequest_withUnsupportedConfigs_shouldThrowException() {
        assertThatThrownBy(() -> S3CrtUtils.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.apiCallAttemptTimeout(Duration.ofMinutes(1)))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtUtils.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.apiCallTimeout(Duration.ofMinutes(1)))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtUtils.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.credentialsProvider(() ->
                                                                                                                                        AwsBasicCredentials.create("", "")))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtUtils.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.addApiName(ApiName.builder()
                                                                                                                                  .name("test")
                                                                                                                                  .version("1")
                                                                                                                                  .build()))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtUtils.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.addMetricPublisher(LoggingMetricPublisher.create()))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> S3CrtUtils.toCrtGetObjectRequest(GetObjectRequest.builder()
                                                                                  .overrideConfiguration(b -> b.signer(AwsS3V4Signer.create()))
                                                                                  .build())).isExactlyInstanceOf(UnsupportedOperationException.class);
    }

    private AwsRequestOverrideConfiguration requestOverrideConfigWithCustomHeaders() {
        return AwsRequestOverrideConfiguration.builder()
                                              .putHeader("foo", "bar")
                                              .putRawQueryParameter("hello1", "world1")
                                              .putRawQueryParameter("hello2", "world2")
                                              .build();
    }

    private void verifyHeaders(HttpHeader[] headers) {
        assertThat(headers).hasSize(2);
        verifyUserAgent(headers);
        assertThat(headers[1].getName()).isEqualTo("foo");
        assertThat(headers[1].getValue()).isEqualTo("bar");
    }

    private void verifyUserAgent(HttpHeader[] headers) {
        assertThat(headers[0].getName()).isEqualTo("User-Agent");
        assertThat(headers[0].getValue()).contains("ft/s3-transfer");
        assertThat(headers[0].getValue()).contains(SdkUserAgent.create().userAgent());
    }
}