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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public class SdkBytesGetterSetter {

    void byteBufferSetter() {
        ByteBuffer buffer = ByteBuffer.wrap("helloworld".getBytes(StandardCharsets.UTF_8));
        MessageAttributeValue messageAttributeValue = MessageAttributeValue.builder()
            .binaryValue(SdkBytes.fromByteBuffer(buffer))
            .build();
    }

    void sdkBytesGetters() {
        MessageAttributeValue messageAttributeValue = MessageAttributeValue.builder()
            .build();

        ByteBuffer binaryValue = messageAttributeValue.binaryValue().asByteBuffer();
        String binaryString = new String(messageAttributeValue.binaryValue().asByteBuffer().array());
    }
}
