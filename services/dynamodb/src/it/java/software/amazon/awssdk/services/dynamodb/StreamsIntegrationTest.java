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

package software.amazon.awssdk.services.dynamodb;

import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.dynamodb.model.ListStreamsRequest;
import software.amazon.awssdk.services.dynamodbstreams.DynamoDBStreamsClient;
import software.amazon.awssdk.testutils.service.AwsTestBase;

public class StreamsIntegrationTest extends AwsTestBase {

    private DynamoDBStreamsClient streams;

    @Before
    public void setup() throws Exception {
        setUpCredentials();
        streams = DynamoDBStreamsClient.builder().credentialsProvider(CREDENTIALS_PROVIDER_CHAIN).build();
    }

    @Test
    public void testDefaultEndpoint() {
        streams.listStreams(ListStreamsRequest.builder().tableName("foo").build());
    }
}
