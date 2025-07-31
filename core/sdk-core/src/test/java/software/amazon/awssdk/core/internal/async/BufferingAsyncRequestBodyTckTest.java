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

package software.amazon.awssdk.core.internal.async;


import java.nio.ByteBuffer;
import org.apache.commons.lang3.RandomStringUtils;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.SdkBytes;

public class BufferingAsyncRequestBodyTckTest extends org.reactivestreams.tck.PublisherVerification<ByteBuffer> {
    public BufferingAsyncRequestBodyTckTest() {
        super(new TestEnvironment(true));
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {
        BufferingAsyncRequestBody bufferingAsyncRequestBody = new BufferingAsyncRequestBody(1024 * elements);
        for (int i = 0; i < elements; i++) {
            bufferingAsyncRequestBody.send(SdkBytes.fromUtf8String(RandomStringUtils.randomAscii(1024)).asByteBuffer());
        }

        bufferingAsyncRequestBody.complete();
        return bufferingAsyncRequestBody;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        BufferingAsyncRequestBody bufferingAsyncRequestBody = new BufferingAsyncRequestBody(1024L);
        bufferingAsyncRequestBody.close();
        return null;
    }

    public long maxElementsFromPublisher() {
        return 100;
    }

}
