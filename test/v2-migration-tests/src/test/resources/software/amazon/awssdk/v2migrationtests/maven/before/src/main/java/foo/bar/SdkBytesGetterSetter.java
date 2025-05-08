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

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class SdkBytesGetterSetter {

    void byteBufferSetter() {
        ByteBuffer buffer = ByteBuffer.wrap("helloworld".getBytes(StandardCharsets.UTF_8));
        MessageAttributeValue messageAttributeValue = new MessageAttributeValue()
            .withBinaryValue(buffer);
    }

    void sdkBytesGetters() {
        MessageAttributeValue messageAttributeValue = new MessageAttributeValue();

        ByteBuffer binaryValue = messageAttributeValue.getBinaryValue();
        String binaryString = new String(messageAttributeValue.getBinaryValue().array());
    }
}
