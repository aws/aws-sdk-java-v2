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

package software.amazon.awssdk.modulepath.tests.mocktests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonAsyncClient;
import software.amazon.awssdk.services.protocolrestjson.ProtocolRestJsonClient;

/**
 * Protocol tests for json protocol
 */
public class JsonProtocolApiCall extends BaseMockApiCall {

    private static final Logger logger = LoggerFactory.getLogger(JsonProtocolApiCall.class);
    private ProtocolRestJsonClient client;
    private ProtocolRestJsonAsyncClient asyncClient;

    public JsonProtocolApiCall() {
        super("json");
        this.client = ProtocolRestJsonClient.builder()
                                            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                                "akid", "skid")))
                                            .region(Region.US_EAST_1)
                                            .httpClient(mockHttpClient)
                                            .build();
        this.asyncClient = ProtocolRestJsonAsyncClient.builder()
                                                      .credentialsProvider(
                                                          StaticCredentialsProvider.create(AwsBasicCredentials.create(
                                                          "akid", "skid")))
                                                      .region(Region.US_EAST_1)
                                                      .httpClient(mockAyncHttpClient)
                                                      .build();
    }

    @Override
    Runnable runnable() {
        return () -> client.allTypes();
    }

    @Override
    Runnable asyncRunnable() {
        return () -> asyncClient.allTypes().join();
    }

}
