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

package software.amazon.awssdk.http.auth.aws.internal.signer.io;

import io.reactivex.subscribers.TestSubscriber;
import java.nio.ByteBuffer;
import java.util.Collections;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.checksums.DefaultChecksumAlgorithm;
import software.amazon.awssdk.checksums.SdkChecksum;

public class UnbufferedChecksumSubscriberTckTest extends SubscriberBlackboxVerification<ByteBuffer> {

    public UnbufferedChecksumSubscriberTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Subscriber<ByteBuffer> createSubscriber() {
        return new UnbufferedChecksumSubscriber(
            Collections.singletonList(SdkChecksum.forAlgorithm(DefaultChecksumAlgorithm.CRC32)),
            new TestSubscriber<>());
    }

    @Override
    public ByteBuffer createElement(int element) {
        return ByteBuffer.wrap(String.valueOf(element).getBytes());
    }
}
