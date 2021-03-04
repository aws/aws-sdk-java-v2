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

package software.amazon.awssdk.services.s3.internal;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import org.junit.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.crt.auth.credentials.Credentials;
import software.amazon.awssdk.crt.auth.credentials.CredentialsProvider;

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

        assertThat(ACCESS_KEY, equalTo(new String(credentials.getAccessKeyId(), StandardCharsets.UTF_8)));
        assertThat(SECRET_ACCESS_KEY, equalTo(new String(credentials.getSecretAccessKey(), StandardCharsets.UTF_8)));
        assertThat(SESSION_TOKEN, equalTo(new String(credentials.getSessionToken(), StandardCharsets.UTF_8)));
    }

}