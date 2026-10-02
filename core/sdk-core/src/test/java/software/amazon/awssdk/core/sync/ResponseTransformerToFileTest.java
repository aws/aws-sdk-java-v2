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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.http.AbortableInputStream;


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
}
