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

package software.amazon.awssdk.codegen.emitters.tasks;

import java.util.Collections;
import java.util.List;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.emitters.SimpleGeneratorTask;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;

/**
 * Emits the package-info.java for the base service package. Includes the service
 * level documentation.
 */
public final class PackageInfoGeneratorTasks extends BaseGeneratorTasks {

    private final String baseDirectory;

    PackageInfoGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.baseDirectory = dependencies.getPathProvider().getClientDirectory();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        Metadata metadata = model.getMetadata();
        String packageInfoContents =
            String.format("/**%n"
                          + " * %s%n"
                          + "*/%n"
                          + "package %s;",
                          metadata.getDocumentation(),
                          metadata.getFullClientPackageName());
        return Collections.singletonList(new SimpleGeneratorTask(baseDirectory,
                                                                 "package-info.java",
                                                                 model.getFileHeader(),
                                                                 packageInfoContents));
    }

}
