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

package foo.bar;

import com.amazonaws.regions.Regions;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.retry.RetryMode;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

public final class SdkClientsDependencyFactory {

    private SdkClientsDependencyFactory() {
    }

    public static AmazonSQS defaultSqsClient() {
        return new AmazonSQSClient();
    }

    public static ClientConfiguration customClientConfiguration() {
        ClientConfiguration clientConfiguration = new ClientConfiguration()
            .withRetryMode(RetryMode.STANDARD)
            .withClientExecutionTimeout(1000)
            .withRequestTimeout(1001)
            .withMaxConnections(1002)
            .withConnectionTimeout(1003)
            .withTcpKeepAlive(true)
            .withSocketTimeout(1004)
            .withConnectionTTL(1005)
            .withConnectionMaxIdleMillis(1006)
            .withHeader("foo", "bar");

        return clientConfiguration;
    }

    public static AmazonSQS sqsClientWithAllSettings() {
        return AmazonSQSClient.builder()
                              .withRegion("us-west-2")
                              .withClientConfiguration(customClientConfiguration())
                              .withCredentials(CredentialsDependencyFactory.defaultCredentialsProviderChain())
                              .build();
    }

    public static AmazonSQSClientBuilder sqsClientBuilder() {
        ClientConfiguration configuration = new ClientConfiguration().withMaxConnections(500);
        return AmazonSQSClient.builder()
                              .withClientConfiguration(configuration);
    }

    public static AmazonSQSAsync defaultSqsAsyncClient() {
        return new AmazonSQSAsyncClient();
    }

    public static AmazonSQSAsync sqsAsyncClientWithAllSettings() {

        ClientConfiguration clientConfiguration = new ClientConfiguration()
            .withRetryMode(RetryMode.STANDARD)
            .withClientExecutionTimeout(2001)
            .withRequestTimeout(2002)
            .withConnectionTimeout(2004)
            .withHeader("hello", "world");

        return AmazonSQSAsyncClient.asyncBuilder()
                                   .withRegion(Regions.US_WEST_2)
                                   .withCredentials(CredentialsDependencyFactory.defaultCredentialsProviderChain())
                                   .withClientConfiguration(clientConfiguration)
                                   .build();
    }
}
