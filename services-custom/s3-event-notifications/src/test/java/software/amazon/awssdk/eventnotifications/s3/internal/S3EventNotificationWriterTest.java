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

package software.amazon.awssdk.eventnotifications.s3.internal;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.eventnotifications.s3.model.RequestParameters;
import software.amazon.awssdk.eventnotifications.s3.model.ResponseElements;
import software.amazon.awssdk.eventnotifications.s3.model.S3;
import software.amazon.awssdk.eventnotifications.s3.model.S3Bucket;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotificationRecord;
import software.amazon.awssdk.eventnotifications.s3.model.S3Object;
import software.amazon.awssdk.eventnotifications.s3.model.UserIdentity;

public class S3EventNotificationWriterTest {

    @Test
    void test() {
        S3EventNotification event = new S3EventNotification(
            Arrays.asList(new S3EventNotificationRecord(
                "us-west-2",
                "ObjectCreated:Put",
                "aws:s3",
                "1970-01-01T00:00:00.000Z",
                "2.1",
                new RequestParameters("127.0.0.1"),
                new ResponseElements(
                    "FMyUVURIY8/IgAtTv8xRjskZQpcIZ9KG4V5Wp6S7S/JRWeUWerMUE5JgHvANOjpD", "C3D13FE58DE4C810"),
                new S3(
                    "testConfigRule",
                    new S3Bucket(
                        "mybucket",
                        new UserIdentity("A3NL1KOZZKExample"),
                        "arn:aws:s3:::mybucket"),
                    new S3Object(
                        "HappyFace.jpg",
                        1024L,
                        "d41d8cd98f00b204e9800998ecf8427e",
                        "096fKKXTRTtl3on89fVO.nfljtsv6qko",
                        "0055AED6DCD90281E5"),
                    "1.0"
                ),
                new UserIdentity("AIDAJDPLRKLG7UEXAMPLE")
            ))
        );

    }
}
