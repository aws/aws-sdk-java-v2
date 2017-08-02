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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import software.amazon.awssdk.SdkBaseException;
import software.amazon.awssdk.auth.AwsCredentials;
import software.amazon.awssdk.auth.StaticCredentialsProvider;
import software.amazon.awssdk.global.handlers.TestGlobalExecutionInterceptor;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.model.ListTablesRequest;

public class GlobalRequestHandlerTest {

    @Before
    public void setup() {
        TestGlobalExecutionInterceptor.reset();
    }

    @Test
    @Ignore // FIXME: Fails with "region cannot be null"
    public void clientCreatedWithConstructor_RegistersGlobalHandlers() {
        assertFalse(TestGlobalExecutionInterceptor.wasCalled());
        DynamoDBClient client = DynamoDBClient.builder()
                .credentialsProvider(new StaticCredentialsProvider(new AwsCredentials("akid", "skid")))
                .region(Region.US_WEST_2)
                .build();
        callApi(client);
        assertTrue(TestGlobalExecutionInterceptor.wasCalled());
    }

    @Test
    @Ignore // FIXME: Fails with "region cannot be null"
    public void clientCreatedWithBuilder_RegistersGlobalHandlers() {
        assertFalse(TestGlobalExecutionInterceptor.wasCalled());
        DynamoDBClient client = DynamoDBClient.builder()
                .credentialsProvider(new StaticCredentialsProvider(new AwsCredentials("akid", "skid")))
                .region(Region.US_WEST_2)
                .build();
        callApi(client);
        assertTrue(TestGlobalExecutionInterceptor.wasCalled());
    }

    private void callApi(DynamoDBClient client) {
        try {
            client.listTables(ListTablesRequest.builder().build());
        } catch (SdkBaseException expected) {
            // Ignored or expected.
        }
    }
}
