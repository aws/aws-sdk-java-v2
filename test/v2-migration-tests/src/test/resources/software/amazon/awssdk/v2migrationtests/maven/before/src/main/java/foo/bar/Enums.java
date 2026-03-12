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

package foo.bar;

import com.amazonaws.services.s3.model.JSONType;
import com.amazonaws.services.s3.model.S3Event;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.sqs.model.QueueAttributeName;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import java.util.List;
import java.util.Map;

public class Enums {

    public static void main(String... args) {
        QueueAttributeName qan = QueueAttributeName.DelaySeconds;
        QueueAttributeName qan2 = QueueAttributeName.All;

        ReceiveMessageRequest v1Request = new ReceiveMessageRequest();
        List<String> attributes = v1Request.getAttributeNames();

        SendMessageRequest v2Request = SendMessageRequest.builder().build();
        Map<String, MessageAttributeValue> messageAttributes = v2Request.messageAttributes();
    }

    public void s3Enums() {
        StorageClass sc1 = StorageClass.StandardInfrequentAccess;
        StorageClass sc2 = StorageClass.OneZoneInfrequentAccess;
        S3Event se = S3Event.ObjectCreated;
        JSONType jsonType = JSONType.DOCUMENT;
    }
}