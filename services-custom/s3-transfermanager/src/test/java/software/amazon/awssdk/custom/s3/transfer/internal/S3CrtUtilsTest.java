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

import com.amazonaws.s3.model.GetObjectOutput;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.testutils.smoketest.ReflectionUtils;

public class S3CrtUtilsTest {

    private final String ACCESS_KEY = "accessKey";
    private final String SECRET_ACCESS_KEY = "secretAccessKey";
    private final String SESSION_TOKEN = "sessionToken";

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
}