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

package software.amazon.awssdk.core.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.jimfs.Jimfs;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.testutils.LogCaptor;


class ResponseTransformerToFileTest {

    @TempDir
    Path tempDir;

    @Test
    void toFile_parentDirectoryDoesNotExist_throwsWithHelpfulMessage() throws Exception {
        Path fileWithMissingParent = tempDir.resolve("nonexistent-parent/subdir/output.wav");
        ResponseTransformer<String, String> transformer = ResponseTransformer.toFile(fileWithMissingParent);

        AbortableInputStream inputStream = AbortableInputStream.create(
            new ByteArrayInputStream("test-content".getBytes(StandardCharsets.UTF_8))
        );

        assertThatThrownBy(() -> transformer.transform("response", inputStream))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Verify that the file's parent directories exist")
            .hasMessageContaining("The SDK will not auto-create them")
            .hasCauseInstanceOf(NoSuchFileException.class);
    }

    @Test
    void toFile_whenDeleteAfterFailedCopyFails_logsEscapedPathAndThrowable() throws Exception {
        try (FileSystem fileSystem = Jimfs.newFileSystem()) {
            Path destination = fileSystem.getPath("name\nFORGED LINE\n");
            ResponseTransformer<String, String> transformer = ResponseTransformer.toFile(destination);
            AbortableInputStream inputStream =
                AbortableInputStream.create(new ReplaceDestinationThenFailStream(destination));

            try (LogCaptor logCaptor = LogCaptor.create(Level.ERROR)) {
                assertThatThrownBy(() -> transformer.transform("response", inputStream))
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("could not be cleaned up");

                List<LogEvent> errors = logCaptor.loggedEvents().stream()
                                                  .filter(e -> e.getLevel() == Level.ERROR)
                                                  .filter(e -> e.getLoggerName().equals(ResponseTransformer.class.getName()))
                                                  .collect(Collectors.toList());
                assertThat(errors).hasSize(1);
                LogEvent event = errors.get(0);
                assertThat(event.getMessage().getFormattedMessage())
                    .contains("name\\nFORGED LINE\\n")
                    .doesNotContain("\n", "\r");
                assertThat(event.getThrown()).isNotNull();
                assertThat(event.getThrown().toString()).doesNotContain("\n", "\r");
            }
        }
    }

    private static final class ReplaceDestinationThenFailStream extends InputStream {
        private final Path destination;
        private boolean firstRead = true;

        private ReplaceDestinationThenFailStream(Path destination) {
            this.destination = destination;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int read = read(one, 0, 1);
            return read < 0 ? -1 : one[0] & 0xff;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            if (firstRead) {
                firstRead = false;
                int bytesToRead = Math.min(length, 8);
                for (int i = 0; i < bytesToRead; i++) {
                    bytes[offset + i] = (byte) 'A';
                }
                return bytesToRead;
            }
            Files.deleteIfExists(destination);
            Files.createDirectory(destination);
            Files.write(destination.resolve("child"), new byte[] {1});
            throw new IOException("connection reset");
        }
    }
}
