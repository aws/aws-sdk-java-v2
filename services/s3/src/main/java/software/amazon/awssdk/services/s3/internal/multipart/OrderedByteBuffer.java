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

package software.amazon.awssdk.services.s3.internal.multipart;

import java.nio.ByteBuffer;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 *
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
@SdkInternalApi
public class OrderedByteBuffer {
    private final int position;
    private final ByteBuffer buffer;

    public OrderedByteBuffer(int position, Long contentLength) {
        this.position = position;
        this.buffer = ByteBuffer.allocate(contentLength.intValue());
    }

    public void put(ByteBuffer body) {
        this.buffer.put(body);
    }

    public int position() {
        return position;
    }

    public ByteBuffer buffer() {
        return buffer;
    }

}
