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

package software.amazon.awssdk.core.internal.sync;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for {@link FileContentStreamProvider}.
 */
public class FileContentStreamProviderTest {
    private static FileSystem testFs;
    private static Path testFile;

    @BeforeClass
    public static void setup() throws IOException {
        testFs = Jimfs.newFileSystem("FileContentStreamProviderTest");
        testFile = testFs.getPath("test_file.dat");

        try (OutputStream os = Files.newOutputStream(testFile)) {
            os.write("test".getBytes(StandardCharsets.UTF_8));
        }
    }

    @AfterClass
    public static void teardown() throws IOException {
        testFs.close();
    }

    @Test
    public void newStreamClosesPreviousStream() {
        FileContentStreamProvider provider = new FileContentStreamProvider(testFile);

        InputStream oldStream = provider.newStream();
        provider.newStream();
        assertThatThrownBy(oldStream::read).hasMessage("stream is closed");
    }
}
