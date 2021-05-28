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

        assertThat(ACCESS_KEY.getBytes(StandardCharsets.UTF_8)).isEqualTo(credentials.getAccessKeyId());
        assertThat(SECRET_ACCESS_KEY.getBytes(StandardCharsets.UTF_8)).isEqualTo(credentials.getSecretAccessKey());
        assertThat(SESSION_TOKEN.getBytes(StandardCharsets.UTF_8)).isEqualTo(credentials.getSessionToken());
    }

}