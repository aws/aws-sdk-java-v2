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

package software.amazon.awssdk.core.internal.http.async;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;

/**
 * TCK verification test for {@link SimpleHttpContentPublisher}.
 */
public class SimpleRequestProviderTckTest extends PublisherVerification<ByteBuffer> {
    private static final byte[] CONTENT = new byte[4906];
    public SimpleRequestProviderTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<ByteBuffer> createPublisher(long l) {
        return new SimpleHttpContentPublisher(makeFullRequest());
    }

    @Override
    public long maxElementsFromPublisher() {
        // SimpleRequestProvider is a one shot publisher
        return 1;
    }

    @Override
    public Publisher<ByteBuffer> createFailedPublisher() {
        return null;
    }

    private static SdkHttpFullRequest makeFullRequest() {
        return SdkHttpFullRequest.builder()
                                 .uri(URI.create("https://aws.amazon.com"))
                                 .method(SdkHttpMethod.PUT)
                                 .contentStreamProvider(() -> new ByteArrayInputStream(CONTENT))
                                 .build();
    }
}
