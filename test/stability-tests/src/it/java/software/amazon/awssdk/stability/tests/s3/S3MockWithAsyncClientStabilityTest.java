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

package software.amazon.awssdk.stability.tests.s3;

import static software.amazon.awssdk.testutils.service.AwsTestBase.CREDENTIALS_PROVIDER_CHAIN;

import org.junit.jupiter.api.BeforeEach;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.testutils.service.http.MockAsyncHttpClient;

public class S3MockWithAsyncClientStabilityTest extends S3MockStabilityTestBase {

    private static String bucketName = "s3mockwithasyncclientstabilitytests" + System.currentTimeMillis();

    @BeforeEach
    void setup(){
        mockAsyncHttpClient = new MockAsyncHttpClient();
        testClient = S3AsyncClient.builder()
                                  .credentialsProvider(CREDENTIALS_PROVIDER_CHAIN)
                                  .httpClient(mockAsyncHttpClient)
                                  .build();
    }

    @Override
    protected String getTestBucketName() {
        return bucketName;
    }

}
