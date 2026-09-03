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

package software.amazon.awssdk.codegen.smithy;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.build.model.ProjectionConfig;
import software.amazon.smithy.build.model.SmithyBuildConfig;
import software.amazon.smithy.build.model.TransformConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.transform.ModelTransformer;

/**
 * Applies declarative Smithy transforms to a model before it reaches
 * {@link SmithyIntermediateModelBuilder}.
 *
 * <p>This is the Smithy-path counterpart to the C2J {@code preprocess} hook. Where C2J mutates a
 * {@code ServiceModel} in place, a Smithy {@link Model} is immutable, so customizations are applied
 * by transforming one model into another. The transformed model is what gets translated, so the
 * translator never has to know that customizations exist.
 *
 * <p>Transforms are read from a {@value #TRANSFORM_CONFIG_FILE} sitting beside the model, using
 * Smithy's own config loader. The file is a genuine smithy-build projection file, not a
 * bespoke format:
 *
 * <pre>{@code
 * {
 *   "version": "1.0",
 *   "projections": {
 *     "sdk": {
 *       "transforms": [
 *         { "name": "renameShapes", "args": { "renamed": { "ns#Old": "ns#New" } } }
 *       ]
 *     }
 *   }
 * }
 * }</pre>
 *
 * <p>Transform implementations are resolved through the
 * {@code software.amazon.smithy.build.ProjectionTransformer} service loader, so transforms shipped
 * by Smithy and transforms written here are found by the same lookup and are indistinguishable in
 * the config file apart from their name.
 *
 * <p>The transformed model is not re-validated. A transform that produces a structurally invalid
 * model will surface as a translation failure rather than a validation error.
 */
public final class SmithyTransformChain {

    /**
     * Presence of this file beside the model triggers the transform stage, mirroring how the Smithy
     * path itself is triggered by the presence of a model file.
     */
    public static final String TRANSFORM_CONFIG_FILE = "smithy-build.json";

    /**
     * The single projection the SDK reads. smithy-build supports many projections per file; codegen
     * has no use for more than one.
     */
    public static final String SDK_PROJECTION = "sdk";

    private static final Logger log = LoggerFactory.getLogger(SmithyTransformChain.class);

    private SmithyTransformChain() {
    }

    /**
     * Applies the transforms declared beside the model, or returns the model unchanged when no
     * transform config is present.
     *
     * @param model the assembled model
     * @param modelRoot the directory holding the model, searched for {@value #TRANSFORM_CONFIG_FILE}
     * @param classLoader the loader carrying the transform implementations; callers should pass their
     *                    own class's loader rather than the thread context loader, which does not
     *                    reliably point at the right realm under Maven
     */
    public static Model applyIfPresent(Model model, Path modelRoot, ClassLoader classLoader) {
        Path configFile = modelRoot.resolve(TRANSFORM_CONFIG_FILE);
        if (!Files.isRegularFile(configFile)) {
            return model;
        }

        log.info("Detected {}; applying Smithy transforms before translation.", TRANSFORM_CONFIG_FILE);
        return apply(model, SmithyBuildConfig.load(configFile), SDK_PROJECTION, classLoader);
    }

    /**
     * Applies the named projection's transforms to the model.
     *
     * @throws IllegalArgumentException if the config declares no such projection
     */
    public static Model apply(Model model, SmithyBuildConfig config, String projectionName, ClassLoader classLoader) {
        ProjectionConfig projection = config.getProjections().get(projectionName);
        if (projection == null) {
            throw new IllegalArgumentException(String.format(
                "Smithy transform config declares no projection named '%s'. Found: %s",
                projectionName, config.getProjections().keySet()));
        }
        return apply(model, projection.getTransforms(), projectionName, classLoader);
    }

    /**
     * Runs a transform chain, feeding each transform's output into the next as input.
     *
     * <p>An unresolvable transform name fails rather than being skipped, so a typo in the config
     * breaks the build instead of silently dropping a customization.
     */
    public static Model apply(Model model,
                              List<TransformConfig> transforms,
                              String projectionName,
                              ClassLoader classLoader) {
        if (transforms.isEmpty()) {
            return model;
        }

        Function<String, Optional<ProjectionTransformer>> factory =
            ProjectionTransformer.createServiceFactory(classLoader);
        // createWithServiceProviders, not create, so the cleanup plugins that fix up dangling
        // references after a rename or removal are loaded.
        ModelTransformer transformer = ModelTransformer.createWithServiceProviders(classLoader);

        Model original = model;
        Model current = model;

        for (TransformConfig transform : transforms) {
            String name = transform.getName();
            ProjectionTransformer implementation = factory.apply(name).orElseThrow(
                () -> new IllegalArgumentException(String.format(
                    "Unknown Smithy transform '%s'. It is neither shipped by Smithy nor registered "
                    + "under META-INF/services/software.amazon.smithy.build.ProjectionTransformer.", name)));

            current = implementation.transform(TransformContext.builder()
                                                               .model(current)
                                                               .originalModel(original)
                                                               .settings(transform.getArgs())
                                                               .transformer(transformer)
                                                               .projectionName(projectionName)
                                                               .build());

            log.info("Applied Smithy transform '{}'.", name);
        }

        return current;
    }
}
