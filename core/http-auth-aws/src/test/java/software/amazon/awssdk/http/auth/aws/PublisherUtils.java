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

package software.amazon.awssdk.http.auth.aws;

import io.reactivex.Flowable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.PrimitiveIterator;
import java.util.Random;
import org.reactivestreams.Publisher;

public final class PublisherUtils {
    private static final Random RNG = new Random();

    private PublisherUtils() {
    }

    public static Publisher<ByteBuffer> randomPublisherOfLength(int bytes, int min, int max) {
        List<ByteBuffer> elements = new ArrayList<>();

        PrimitiveIterator.OfInt sizeIter = RNG.ints(min, max).iterator();

        while (bytes > 0) {
            int elementSize = sizeIter.next();
            elementSize = Math.min(elementSize, bytes);

            bytes -= elementSize;

            byte[] elementContent = new byte[elementSize];
            RNG.nextBytes(elementContent);
            elements.add(ByteBuffer.wrap(elementContent));
        }

        return Flowable.fromIterable(elements);
    }
}
