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

package software.amazon.awssdk.enhanced.dynamodb.functionaltests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

public class LocalDynamoDbAsyncTestBase extends LocalDynamoDbTestBase {
    private DynamoDbAsyncClient dynamoDbAsyncClient = localDynamoDb().createAsyncClient();

    protected DynamoDbAsyncClient getDynamoDbAsyncClient() {
        return dynamoDbAsyncClient;
    }

    protected static <T> List<T> drainPublisher(SdkPublisher<T> publisher, int expectedNumberOfResults) {
        BufferingSubscriber<T> subscriber = new BufferingSubscriber<>();
        publisher.subscribe(subscriber);
        subscriber.waitForCompletion(1000L);

        assertThat(subscriber.isCompleted(), is(true));
        assertThat(subscriber.bufferedError(), is(nullValue()));
        assertThat(subscriber.bufferedItems().size(), is(expectedNumberOfResults));

        return subscriber.bufferedItems();
    }
}
