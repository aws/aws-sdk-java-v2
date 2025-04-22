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

package software.amazon.awssdk.services.s3;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Event;
import software.amazon.awssdk.services.s3.model.LambdaFunctionConfiguration;
import software.amazon.awssdk.services.s3.model.NotificationConfiguration;
import software.amazon.awssdk.services.s3.model.QueueConfiguration;
import software.amazon.awssdk.services.s3.model.TopicConfiguration;

public class test1 {
    public static void main(String[] args) {
        S3Client s3 = S3Client.builder().build();
        NotificationConfiguration notificationConfig = NotificationConfiguration.builder()
                                                                                .lambdaFunctionConfigurations(
                                                                                    LambdaFunctionConfiguration.builder()
                                                                                                               .lambdaFunctionArn("arn:aws:lambda:function")
                                                                                                               .events(Event.valueOf("s3:ObjectCreated:"))
                                                                                                               .build())
                                                                                .topicConfigurations(
                                                                                    TopicConfiguration.builder()
                                                                                                      .topicArn("arn:aws:sns:topic")
                                                                                                      .events(Event.valueOf("s3:ObjectRemoved:"))
                                                                                                      .build())
                                                                                .queueConfigurations(
                                                                                    QueueConfiguration.builder()
                                                                                                      .queueArn("arn:aws:sqs:queue")
                                                                                                      .events(Event.valueOf("s3"
                                                                                                                            + ":ObjectRestore:*"))
                                                                                                      .build())
                                                                                .build();

        s3.putBucketNotificationConfiguration(req -> req
            .bucket("bucket")
            .notificationConfiguration(notificationConfig));
    }
}
