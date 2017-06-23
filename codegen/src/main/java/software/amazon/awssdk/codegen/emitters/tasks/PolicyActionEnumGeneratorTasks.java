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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.emitters.CodeWriter;
import software.amazon.awssdk.codegen.emitters.FreemarkerGeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTask;
import software.amazon.awssdk.codegen.emitters.GeneratorTaskParams;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.config.customization.AuthPolicyActions;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.util.ImmutableMapParameter;

public class PolicyActionEnumGeneratorTasks extends BaseGeneratorTasks {

    private final String policyEnumClassDir;
    private final AuthPolicyActions policyActions;
    private final Metadata metadata;

    public PolicyActionEnumGeneratorTasks(GeneratorTaskParams dependencies) {
        super(dependencies);
        this.policyEnumClassDir = dependencies.getPathProvider().getPolicyEnumDirectory();
        this.policyActions = model.getCustomizationConfig().getAuthPolicyActions();
        this.metadata = model.getMetadata();
    }

    @Override
    protected boolean hasTasks() {
        final AuthPolicyActions policyActions = model.getCustomizationConfig().getAuthPolicyActions();
        return policyActions == null || !policyActions.isSkip();
    }

    @Override
    protected List<GeneratorTask> createTasks() throws Exception {
        info("Emitting policy action enum class");

        String serviceName = getPolicyActionServiceName();
        String actionPrefix = getEnumActionPrefix();

        Map<String, Object> dataModel = ImmutableMapParameter.of(
                "fileHeader", model.getFileHeader(),
                "operations", model.getOperations().keySet(),
                "metadata", model.getMetadata(),
                "serviceName", serviceName,
                "actionPrefix", actionPrefix);

        return Collections.singletonList(
                new FreemarkerGeneratorTask(new CodeWriter(policyEnumClassDir, serviceName + "Actions"),
                                            freemarker.getPolicyActionClassTemplate(),
                                            dataModel));
    }

    private String getPolicyActionServiceName() {

        // This is to support the file naming for exiting/legacy clients.
        // The files don't follow the standard naming conventions.
        // To avoid breaking changes, the fileNamePrefix contains the name of
        // the file to be used.
        if (policyActions != null && policyActions.getFileNamePrefix() != null) {
            return Utils.capitialize(policyActions.getFileNamePrefix());
        }

        return Utils.capitialize(metadata.getEndpointPrefix());
    }

    private String getEnumActionPrefix() {

        if (policyActions != null && policyActions.getActionPrefix() != null) {
            return policyActions.getActionPrefix();
        }
        return metadata.getEndpointPrefix();
    }

}
