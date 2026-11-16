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

package software.amazon.awssdk.codegen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.model.config.customization.ShareModelConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Orders clients generated together and finds the client each one shares model classes with, as set by
 * {@code shareModelConfig} in its {@code customization.config}.
 */
@SdkInternalApi
public final class SharedModelTargets {

    private SharedModelTargets() {
    }

    /**
     * Returns the models with the clients that share another client's model classes last.
     */
    public static List<IntermediateModel> sharersLast(List<IntermediateModel> models) {
        List<IntermediateModel> ordered = new ArrayList<>(models);
        ordered.sort(Comparator.comparing(m -> shareModelConfig(m) != null));
        return ordered;
    }

    /**
     * Returns the model whose classes {@code model} shares, or null when it shares none.
     */
    public static IntermediateModel target(IntermediateModel model, List<IntermediateModel> models) {
        ShareModelConfig config = shareModelConfig(model);
        if (config == null) {
            return null;
        }
        String targetName = config.getShareModelWith();
        return models.stream()
                     .filter(m -> StringUtils.lowerCase(m.getMetadata().getServiceName()).equals(targetName))
                     .findFirst()
                     .orElseThrow(() -> new IllegalStateException(
                         model.getMetadata().getServiceName() + " shares model classes with '" + targetName
                         + "', but no client of that name is generated with it"));
    }

    private static ShareModelConfig shareModelConfig(IntermediateModel model) {
        return model.getCustomizationConfig().getShareModelConfig();
    }
}
