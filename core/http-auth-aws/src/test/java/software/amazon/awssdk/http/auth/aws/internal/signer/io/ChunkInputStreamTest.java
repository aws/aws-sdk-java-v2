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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.http.auth.aws.internal.signer.chunkedencoding.ChunkInputStream;

public class ChunkInputStreamTest {

    @Test
    public void close_shouldDrainChunk() throws IOException {
        ByteArrayInputStream backingStream = new ByteArrayInputStream(new byte[] {'a', 'b', 'c', 'd', 'e', 'f', 'g'});
        ChunkInputStream in = new ChunkInputStream(backingStream, 5);

        in.close();

        assertEquals(0, in.remaining());
        assertEquals(2, backingStream.available());
    }
}
