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

package software.amazon.awssdk.codegen.smithy.customization.processors;

import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationProcessor;
import software.amazon.awssdk.utils.Logger;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Base class for Category C processors that support both old (deprecated) C2J config
 * fields and new Smithy-native config fields. Implements the dual-config resolution
 * pattern: detects conflicts (both set), logs deprecation warnings (old only),
 * converts old to new, and delegates to Smithy-native logic.
 *
 * @param <O> the old (deprecated) config type
 * @param <N> the new (Smithy-native) config type
 */
public abstract class AbstractDualConfigProcessor<O, N> implements SmithyCustomizationProcessor {

    private static final Logger log = Logger.loggerFor(AbstractDualConfigProcessor.class);

    private final O oldConfig;
    private final N newConfig;
    private final String oldFieldName;
    private final String newFieldName;

    protected AbstractDualConfigProcessor(O oldConfig, N newConfig,
                                          String oldFieldName, String newFieldName) {
        this.oldConfig = oldConfig;
        this.newConfig = newConfig;
        this.oldFieldName = oldFieldName;
        this.newFieldName = newFieldName;
    }

    @Override
    public Model preprocess(Model model, ServiceShape service) {
        N resolved = resolveConfig(model, service);
        if (resolved == null) {
            return model;
        }
        return applySmithyLogic(model, service, resolved);
    }

    @Override
    public void postprocess(IntermediateModel intermediateModel) {
        // Default no-op; subclasses override if needed
    }

    /**
     * Resolves which config to use. Throws if both are set, warns if old is used,
     * returns null if neither is set.
     */
    protected N resolveConfig(Model model, ServiceShape service) {
        boolean oldSet = isSet(oldConfig);
        boolean newSet = isSet(newConfig);

        if (oldSet && newSet) {
            throw new IllegalStateException(
                String.format("Both '%s' (deprecated) and '%s' (Smithy-native) are set "
                              + "in customization config. Use only '%s'.",
                              oldFieldName, newFieldName, newFieldName));
        }

        if (oldSet) {
            log.warn(() -> String.format("Customization config field '%s' is deprecated. "
                                         + "Use '%s' instead.", oldFieldName, newFieldName));
            return convertOldToNew(oldConfig, model, service);
        }

        if (newSet) {
            return newConfig;
        }

        return null;
    }

    /**
     * Check if a config value is considered "set" (non-null, non-empty).
     */
    protected abstract boolean isSet(Object config);

    /**
     * Convert old C2J config to new Smithy-native config.
     */
    protected abstract N convertOldToNew(O oldConfig, Model model, ServiceShape service);

    /**
     * Public accessor for {@link #convertOldToNew} that allows the
     * {@link software.amazon.awssdk.codegen.smithy.customization.SmithyCustomizationConfigTranslator}
     * to invoke the conversion logic without triggering the full processor lifecycle.
     */
    public N convertForTranslation(O oldConfig, Model model, ServiceShape service) {
        return convertOldToNew(oldConfig, model, service);
    }

    /**
     * Apply the Smithy-native logic using the resolved config.
     */
    protected abstract Model applySmithyLogic(Model model, ServiceShape service, N config);
}
