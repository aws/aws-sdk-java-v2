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
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClient;
import com.amazonaws.services.sqs.AmazonSQSClient;

public final class SdkClientsDependencyFactory {

    private SdkClientsDependencyFactory() {
    }

    public static AmazonSQS defaultSqsClient() {
        return new AmazonSQSClient();
    }

    public static AmazonSQS sqsClientWithAllSettings() {
        return AmazonSQSClient.builder()
                              .withRegion(Regions.US_WEST_2)
                              .withCredentials(CredentialsDependencyFactory.defaultCredentialsProviderChain())
                              .build();
    }

    public static AmazonSQSAsync defaultSqsAsyncClient() {
        return new AmazonSQSAsyncClient();
    }

    public static AmazonSQSAsync defaultSqsAsyncClientWithAllSettings() {
        return AmazonSQSAsyncClient.asyncBuilder()
                                   .withRegion(Regions.US_WEST_2)
                                   .withCredentials(CredentialsDependencyFactory.defaultCredentialsProviderChain())
                                   .build();
    }
}
