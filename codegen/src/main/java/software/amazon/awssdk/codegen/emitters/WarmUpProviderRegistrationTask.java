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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Appends a generated {@code SdkWarmUpProvider} implementation's fully-qualified class name to the shared
 * {@code META-INF/services/software.amazon.awssdk.core.crac.SdkWarmUpProvider} resource file.
 *
 * <p>This task writes a plain resource file rather than a {@code .java} file, so it does not use
 * {@link SimpleGeneratorTask}. It creates the file once and appends to it for each subsequent service, and
 * re-registering the same class name is a no-op.
 */
public final class WarmUpProviderRegistrationTask extends GeneratorTask {

    private static final String SPI_RESOURCE_PATH =
        "META-INF/services/software.amazon.awssdk.core.crac.SdkWarmUpProvider";

    private final String resourcesDirectory;
    private final String providerClassName;

    public WarmUpProviderRegistrationTask(String resourcesDirectory, String providerClassName) {
        this.resourcesDirectory = resourcesDirectory;
        this.providerClassName = providerClassName;
    }

    @Override
    public void compute() {
        try {
            Path file = Paths.get(resourcesDirectory, SPI_RESOURCE_PATH);
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            if (Files.exists(file)) {
                List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
                if (lines.contains(providerClassName)) {
                    return;
                }
            }

            Files.write(file,
                        (providerClassName + "\n").getBytes(StandardCharsets.UTF_8),
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to register warm up provider " + providerClassName, e);
        }
    }
}
