/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.core.runtime.adapters.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Test;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.adapter.StringToSdkBytesAdapter;

public class StringToSdkBytesAdapterTest {

    private final StringToSdkBytesAdapter adapter = new StringToSdkBytesAdapter();

    @Test
    public void nullString_ReturnsNullByteBuffer() {
        assertNull(adapter.adapt(null));
    }

    @Test
    public void emptyString_ReturnsEmptyByteBuffer() throws IOException {
        SdkBytes bytes = adapter.adapt("");
        assertEquals(0, bytes.asByteBuffer().remaining());
    }

    @Test
    public void nonEmptyString_ReturnsNonEmptyByteBuffer() throws IOException {
        String source = "foo";
        SdkBytes bytes = adapter.adapt(source);
        assertEquals(new String(bytes.asByteArray()), source);
    }

}
