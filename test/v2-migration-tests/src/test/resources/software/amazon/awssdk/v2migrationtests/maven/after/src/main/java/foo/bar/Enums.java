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
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import java.util.List;
import java.util.Map;

public class Enums {

    public static void main(String... args) {
        QueueAttributeName qan = QueueAttributeName.DELAY_SECONDS;
        QueueAttributeName qan2 = QueueAttributeName.ALL;
        System.out.println(qan);
        System.out.println(qan2);

        ReceiveMessageRequest v1Request = ReceiveMessageRequest.builder()
            .build();
        List<String> attributes = v1Request.attributeNamesAsStrings();
        System.out.println(attributes);

        SendMessageRequest v2Request = SendMessageRequest.builder().build();
        Map<String, MessageAttributeValue> messageAttributes = v2Request.messageAttributes();
        System.out.println(messageAttributes);
    }
}