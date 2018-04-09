/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.core.AwsRequest;
import software.amazon.awssdk.core.AwsRequestOverrideConfig;;
import software.amazon.awssdk.core.auth.AwsCredentials;
import software.amazon.awssdk.core.auth.AwsCredentialsProvider;
import software.amazon.awssdk.core.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.core.auth.StaticCredentialsProvider;
import software.amazon.awssdk.core.http.NoopTestAwsRequest;

public class CredentialUtilsTest {

    private static final AwsCredentialsProvider CLIENT_CREDENTIALS = DefaultCredentialsProvider.create();

    @Test
    public void request_credentials_takes_precendence_over_client_credentials() {
        final String awsAccessKeyId = "foo";
        final String awsSecretAccessKey = "bar";
        final AwsCredentials reqCredentials = AwsCredentials.create(awsAccessKeyId,
                                                                 awsSecretAccessKey);
        AwsRequest req = NoopTestAwsRequest.builder()
                .requestOverrideConfig(AwsRequestOverrideConfig.builder()
                        .credentialsProvider(StaticCredentialsProvider.create(reqCredentials))
                        .build())
                .build();
        AwsCredentialsProvider actual = CredentialUtils.getCredentialsProvider(req,
                                                                               null);

        Assert.assertThat(actual, Matchers.instanceOf(StaticCredentialsProvider.class));
        assertEquals(awsAccessKeyId, actual.getCredentials().accessKeyId());
        assertEquals(awsSecretAccessKey, actual.getCredentials().secretAccessKey());
    }

    @Test
    public void base_credentials_returned_when_no_request_credentials_is_present() {
        final String awsAccessKeyId = "foo";
        final String awsSecretAccessKey = "bar";
        final AwsCredentialsProvider base = StaticCredentialsProvider.create(AwsCredentials.create(awsAccessKeyId,
                                                                                                   awsSecretAccessKey));

        AwsCredentialsProvider actual = CredentialUtils
                .getCredentialsProvider((AwsRequest) null, base);
        Assert.assertThat(actual, Matchers.instanceOf(StaticCredentialsProvider.class));
        assertEquals(awsAccessKeyId, actual.getCredentials().accessKeyId());
        assertEquals(awsSecretAccessKey, actual.getCredentials().secretAccessKey());
    }

    @Test
    public void requestCredentialsInRequestConfig_TakesPrecedenceOverClientCredentials() {
        AwsCredentialsProvider requestCredentials = mock(AwsCredentialsProvider.class);
        AwsRequestOverrideConfig requestConfig = AwsRequestOverrideConfig.builder()
                .credentialsProvider(requestCredentials)
                .build();
        AwsCredentialsProvider actual = CredentialUtils
                .getCredentialsProvider(requestConfig, CLIENT_CREDENTIALS);
        assertEquals(requestCredentials, actual);
    }

    @Test
    public void requestCredentialsNotSetInRequestConfig_ReturnsClientCredentials() {
        AwsRequestOverrideConfig requestConfig = AwsRequestOverrideConfig.builder().build();
        AwsCredentialsProvider actual = CredentialUtils
                .getCredentialsProvider(requestConfig, CLIENT_CREDENTIALS);
        assertEquals(CLIENT_CREDENTIALS, actual);
    }
}
