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

package software.amazon.awssdk.core.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.ByteBuffer;
import org.junit.Test;
import software.amazon.awssdk.core.util.Mimetypes;
import software.amazon.awssdk.core.util.StringInputStream;
import software.amazon.awssdk.utils.IoUtils;


public class RequestBodyTest {

    @Test
    public void stringConstructorUsesUTF8ByteLength() {
        // U+03A9 U+03C9
        final String multibyteChars = "Ωω";
        RequestBody rb = RequestBody.of(multibyteChars);
        assertThat(rb.contentLength()).isEqualTo(4L);
    }

    @Test
    public void stringConstructorHasCorrectContentType() {
        RequestBody requestBody = RequestBody.of("hello world");
        assertThat(requestBody.contentType()).isEqualTo(Mimetypes.MIMETYPE_TEXT_PLAIN);
    }

    @Test
    public void streamConstructorHasCorrectContentType() {
        StringInputStream inputStream = new StringInputStream("hello world");
        RequestBody requestBody = RequestBody.of(inputStream, 11);
        assertThat(requestBody.contentType()).isEqualTo(Mimetypes.MIMETYPE_OCTET_STREAM);
        IoUtils.closeQuietly(inputStream, null);
    }

    @Test
    public void bytesArrayConstructorHasCorrectContentType() {
        RequestBody requestBody = RequestBody.of("hello world".getBytes());
        assertThat(requestBody.contentType()).isEqualTo(Mimetypes.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void bytesBufferConstructorHasCorrectContentType() {
        ByteBuffer byteBuffer = ByteBuffer.wrap("hello world".getBytes());
        RequestBody requestBody = RequestBody.of(byteBuffer);
        assertThat(requestBody.contentType()).isEqualTo(Mimetypes.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void emptyBytesConstructorHasCorrectContentType() {
        RequestBody requestBody = RequestBody.empty();
        assertThat(requestBody.contentType()).isEqualTo(Mimetypes.MIMETYPE_OCTET_STREAM);
    }
}
