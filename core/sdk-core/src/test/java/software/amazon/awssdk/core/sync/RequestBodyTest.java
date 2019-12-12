/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.awssdk.core.internal.util.Mimetype;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.StringInputStream;


public class RequestBodyTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void stringConstructorUsesUTF8ByteLength() {
        // U+03A9 U+03C9
        final String multibyteChars = "Ωω";
        RequestBody rb = RequestBody.fromString(multibyteChars);
        assertThat(rb.contentLength()).isEqualTo(4L);
    }

    @Test
    public void stringConstructorHasCorrectContentType() {
        RequestBody requestBody = RequestBody.fromString("hello world");
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_TEXT_PLAIN);
    }

    @Test
    public void fileConstructorHasCorrectContentType() throws IOException {
        FileSystem fs = Jimfs.newFileSystem(Configuration.unix());
        Path path = fs.getPath("./test");
        Files.write(path, "hello world".getBytes());
        RequestBody requestBody = RequestBody.fromFile(path);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void streamConstructorHasCorrectContentType() {
        StringInputStream inputStream = new StringInputStream("hello world");
        RequestBody requestBody = RequestBody.fromInputStream(inputStream, 11);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
        IoUtils.closeQuietly(inputStream, null);
    }


    @Test
    public void nonMarkSupportedInputStreamContentType() throws IOException {
        File file = folder.newFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("hello world");
        }
        InputStream inputStream = Files.newInputStream(file.toPath());
        RequestBody requestBody = RequestBody.fromInputStream(inputStream, 11);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
        assertThat(requestBody.contentStreamProvider().newStream()).isNotNull();
        IoUtils.closeQuietly(inputStream, null);
    }

    @Test
    public void bytesArrayConstructorHasCorrectContentType() {
        RequestBody requestBody = RequestBody.fromBytes("hello world".getBytes());
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void bytesBufferConstructorHasCorrectContentType() {
        ByteBuffer byteBuffer = ByteBuffer.wrap("hello world".getBytes());
        RequestBody requestBody = RequestBody.fromByteBuffer(byteBuffer);
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }

    @Test
    public void emptyBytesConstructorHasCorrectContentType() {
        RequestBody requestBody = RequestBody.empty();
        assertThat(requestBody.contentType()).isEqualTo(Mimetype.MIMETYPE_OCTET_STREAM);
    }
}
