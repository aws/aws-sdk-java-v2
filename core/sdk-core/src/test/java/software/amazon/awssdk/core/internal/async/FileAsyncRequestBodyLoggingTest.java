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

package software.amazon.awssdk.core.internal.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.testutils.LogCaptor;

class FileAsyncRequestBodyLoggingTest {

    private static final String HOSTILE_PATH = "/tmp/q3\nFORGED\r";

    @Test
    void build_whenStartAttributesCannotBeRead_logsEscapedPathAndThrowable() throws IOException {
        Path path = hostilePathThatDisappearsAfterFirstRead();

        try (LogCaptor logCaptor = LogCaptor.create(Level.DEBUG)) {
            FileAsyncRequestBody.builder().path(path).build();

            List<LogEvent> events = logCaptor.loggedEvents()
                                             .stream()
                                             .filter(e -> FileAsyncRequestBody.class.getName().equals(e.getLoggerName()))
                                             .collect(Collectors.toList());

            assertThat(events).hasSize(2);
            for (LogEvent event : events) {
                String message = event.getMessage().getFormattedMessage();
                assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                assertThat(message).contains("/tmp/q3\\nFORGED\\r").doesNotContain("\n", "\r");
                assertThat(event.getThrown()).isNotNull();
                assertThat(event.getThrown().toString())
                    .contains("/tmp/q3\\nFORGED\\r")
                    .doesNotContain("\n", "\r");
            }
        }
    }

    private static Path hostilePathThatDisappearsAfterFirstRead() throws IOException {
        Path path = mock(Path.class);
        FileSystem fileSystem = mock(FileSystem.class);
        FileSystemProvider provider = mock(FileSystemProvider.class);
        BasicFileAttributes attributes = mock(BasicFileAttributes.class);
        when(attributes.size()).thenReturn(10L);
        when(path.getFileSystem()).thenReturn(fileSystem);
        when(fileSystem.provider()).thenReturn(provider);
        when(provider.readAttributes(path, BasicFileAttributes.class))
            .thenReturn(attributes)
            .thenThrow(new NoSuchFileException(HOSTILE_PATH));
        when(path.toString()).thenReturn(HOSTILE_PATH);
        return path;
    }
}
