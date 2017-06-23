/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.io.File;
import java.util.function.Consumer;
import software.amazon.awssdk.codegen.emitters.CodeWriter;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;

/**
 * This validates that services with the {@link CustomizationConfig#shareModelsWith} attribute specified are being generated
 * after the service they are attempting to share models with. This ensures that the services are kept together in the same
 * module and allows us to verify (in {@link CodeWriter}) that their models are compatible with each other.
 */
public class SharedModelsTaskParamsValidator implements Consumer<GeneratorTaskParams> {
    @Override
    public void accept(GeneratorTaskParams params) {
        String sharedModelService = params.getModel().getCustomizationConfig().getShareModelsWith();

        if (sharedModelService != null) {
            // Validate the service we're sharing models with has been generated already.
            File modelPackageDirectory = new File(params.getPathProvider().getModelDirectory());

            if (!modelPackageDirectory.exists()) {
                String error = String.format("Unable to share models with '%s', because that service's models haven't been "
                                             + "generated yet ('%s' doesn't exist). You must generate that service before "
                                             + "generating '%s'.",
                                             sharedModelService,
                                             modelPackageDirectory,
                                             params.getModel().getMetadata().getServiceName());
                throw new IllegalStateException(error);
            }
        }
    }
}
