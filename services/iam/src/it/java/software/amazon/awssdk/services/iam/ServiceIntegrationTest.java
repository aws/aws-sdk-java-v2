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

package software.amazon.awssdk.services.iam;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class ServiceIntegrationTest extends AwsTestBase {
    protected IamClient iam;

    @Before
    public void setUp() {
        iam = IamClient.builder()
                       .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                       .overrideConfiguration(c -> c.retryPolicy(RetryPolicy.builder().numRetries(50).build()))
                       .region(Region.AWS_GLOBAL)
                       .build();
    }

    @Test
    public void smokeTest() {
        assertThat(iam.listUsers().users()).isNotNull();
    }
}
