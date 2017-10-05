/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import software.amazon.awssdk.AmazonWebServiceRequest;
import software.amazon.awssdk.RequestConfig;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.AwsCredentialsProvider;
import software.amazon.awssdk.auth.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import utils.model.EmptyAmazonWebServiceRequest;

public class CredentialUtilsTest {

    private static final AwsCredentialsProvider CLIENT_CREDENTIALS = new DefaultCredentialsProvider();

    @Test
    public void request_credentials_takes_precendence_over_client_credentials() {
        final String awsAccessKeyId = "foo";
        final String awsSecretAccessKey = "bar";
        final AwsCredentials reqCredentials = new AwsCredentials(awsAccessKeyId,
                                                                 awsSecretAccessKey);
        EmptyAmazonWebServiceRequest req = new EmptyAmazonWebServiceRequest();
        req.setRequestCredentials(reqCredentials);
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
        final AwsCredentialsProvider base = new StaticCredentialsProvider(new AwsCredentials(awsAccessKeyId,
                                                                                             awsSecretAccessKey));

        AwsCredentialsProvider actual = CredentialUtils
                .getCredentialsProvider((AmazonWebServiceRequest) null, base);
        Assert.assertThat(actual, Matchers.instanceOf(StaticCredentialsProvider.class));
        assertEquals(awsAccessKeyId, actual.getCredentials().accessKeyId());
        assertEquals(awsSecretAccessKey, actual.getCredentials().secretAccessKey());
    }

    @Test
    public void requestCredentialsInRequestConfig_TakesPrecedenceOverClientCredentials() {
        AwsCredentialsProvider requestCredentials = mock(AwsCredentialsProvider.class);
        RequestConfig requestConfig = mock(RequestConfig.class);
        when(requestConfig.getCredentialsProvider()).thenReturn(requestCredentials);
        AwsCredentialsProvider actual = CredentialUtils
                .getCredentialsProvider(requestConfig, CLIENT_CREDENTIALS);
        assertEquals(requestCredentials, actual);
    }

    @Test
    public void requestCredentialsNotSetInRequestConfig_ReturnsClientCredentials() {
        RequestConfig requestConfig = mock(RequestConfig.class);
        AwsCredentialsProvider actual = CredentialUtils
                .getCredentialsProvider(requestConfig, CLIENT_CREDENTIALS);
        assertEquals(CLIENT_CREDENTIALS, actual);
    }
}
