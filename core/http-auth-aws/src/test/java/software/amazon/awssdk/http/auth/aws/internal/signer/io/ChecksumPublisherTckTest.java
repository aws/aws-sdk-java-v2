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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.http.auth.aws.TestUtils.TickingClock;
import software.amazon.awssdk.http.auth.aws.eventstream.internal.io.SigV4DataFramePublisher;
import software.amazon.awssdk.http.auth.aws.internal.signer.CredentialScope;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.utils.async.SimplePublisher;

public class ChecksumPublisherTckTest extends PublisherVerification<ByteBuffer> {
    public ChecksumPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long elements) {

        SimplePublisher<ByteBuffer> payload = new SimplePublisher<>();

        Publisher<ByteBuffer> checksumPublisher = new ChecksumPublisher(payload, Collections.emptyList());

        // since this publisher specifically appends an empty element to the end, we need to subtract 1
        // from the number of elements to expected to be "produced" before end-of-stream
        long expectedElements = elements;
        for (int i = 0; i < expectedElements; i++) {
            payload.send(ByteBuffer.wrap(Integer.toString(i).getBytes(StandardCharsets.UTF_8)));
        }
        payload.complete();

        return checksumPublisher;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        SimplePublisher<ByteBuffer> payload = new SimplePublisher<>();

        Publisher<ByteBuffer> checksumPublisher = new ChecksumPublisher(payload, Collections.emptyList());

        payload.error(new RuntimeException("boom!"));
        return checksumPublisher;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 256L;
    }
}
