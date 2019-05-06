/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.services.iam;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.service.AwsIntegrationTestBase;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class ServiceIntegrationTest extends AwsIntegrationTestBase {
    protected IamClient iam;

    protected IamAsyncClient iamAsync;

    @Before
    public void setUp() {
        iam = IamClient.builder()
                       .credentialsProvider(getCredentialsProvider())
                       .overrideConfiguration(c -> c.retryPolicy(RetryPolicy.builder().numRetries(50).build()))
                       .region(Region.AWS_GLOBAL)
                       .build();

        iamAsync = IamAsyncClient.builder()
                       .credentialsProvider(getCredentialsProvider())
                       .overrideConfiguration(c -> c.retryPolicy(RetryPolicy.builder().numRetries(50).build()))
                       .region(Region.AWS_GLOBAL)
                       .build();
    }

    @Test
    public void listUsers_ReturnsSuccess() {
        assertThat(iam.listUsers().users()).isNotNull();
    }

    @Test
    public void listUsers_Async_ReturnsSuccess() {
        assertThat(iamAsync.listUsers().join().users()).isNotNull();
    }
}
