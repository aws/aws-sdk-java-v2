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

package software.amazon.awssdk.codegen.emitters;

import static software.amazon.awssdk.codegen.internal.Utils.closeQuietly;

import java.io.IOException;
import java.io.Writer;

/**
 * Simple generator task that writes a string to a file.
 */
public final class SimpleGeneratorTask extends GeneratorTask {

    private final Writer writer;
    private final String fileHeader;
    private String fileName;
    private final String contents;

    public SimpleGeneratorTask(String outputDirectory,
                               String fileName,
                               String fileHeader,
                               String contents) {
        this.fileHeader = fileHeader;
        this.writer = new CodeWriter(outputDirectory, fileName);
        this.fileName = fileName;
        this.contents = contents;
    }

    @Override
    public void compute() {
        try {
            writer.write(fileHeader + "\n");
            writer.write(contents);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating file %s", fileName), e);
        } finally {
            closeQuietly(writer);
        }
    }

}
