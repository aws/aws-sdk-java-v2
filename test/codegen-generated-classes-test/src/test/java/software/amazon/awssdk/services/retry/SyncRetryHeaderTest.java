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

package software.amazon.awssdk.services.retry;

import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;
import software.amazon.awssdk.testutils.service.http.MockSyncHttpClient;

public class SyncRetryHeaderTest extends RetryHeaderTestSuite<MockSyncHttpClient> {
    private final ProtocolRestJsonClient client;

    public SyncRetryHeaderTest() {
        super(new MockSyncHttpClient());
        client = ProtocolRestJsonClient.builder()
            .overrideConfiguration(c -> c.retryStrategy(RetryMode.STANDARD))
                                         .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("akid",
                                                                                                                          "skid")))
                                         .region(Region.US_EAST_1)
                                         .endpointOverride(URI.create("http://localhost"))
                                         .httpClient(mockHttpClient)
                                         .build();
    }

    @Override
    protected void callAllTypesOperation() {
        client.allTypes();
    }
}
