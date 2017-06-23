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
package software.amazon.awssdk.metrics.internal.cloudwatch;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import software.amazon.awssdk.metrics.AwsSdkMetrics;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

/**
 * Performs the integration testing for SDK Metrics feature.
 * 
 */
public class MetricLoggingIntegrationTest extends MetricIntegrationTest {

    @Test
    public void testSDKMetricUploadToAmazonCloudWatch() {
        // Overrides the default metrics collection system with a simple logger
        AwsSdkMetrics.setMetricCollector(new LoggingMetricCollector());
        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        item.put(dynamoDBFieldName,
                new AttributeValue(String.valueOf((int) (Math.random() * 100))));
        dynamo.putItem(
            new PutItemRequest()
                .withTableName(tableName)
                .withItem(item)
        );
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
        }
    }
}
