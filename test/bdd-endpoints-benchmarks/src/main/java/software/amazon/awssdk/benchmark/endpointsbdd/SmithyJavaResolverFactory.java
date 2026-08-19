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

package software.amazon.awssdk.benchmark.endpointsbdd;

import java.net.URL;
import java.util.Map;
import software.amazon.smithy.java.aws.client.core.settings.RegionSetting;
import software.amazon.smithy.java.context.Context;
import software.amazon.smithy.java.endpoints.EndpointResolver;
import software.amazon.smithy.java.endpoints.EndpointResolverParams;
import software.amazon.smithy.java.rulesengine.BytecodeEndpointResolver;
import software.amazon.smithy.java.rulesengine.GeneratedEndpointResolver;
import software.amazon.smithy.java.rulesengine.RulesEngineBuilder;
import software.amazon.smithy.java.rulesengine.RulesEngineSettings;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

/**
 * Builds smithy-java endpoint resolvers from smithy model JSON resources on the classpath.
 *
 * <h2>Compilation</h2>
 * The model's {@code smithy.rules#endpointBdd} trait is used as-is — no re-optimization is
 * applied. Per the smithy-java team's guidance, re-running the sifting/cost/reversal pipeline
 * on an already-optimized trait degrades performance rather than improving it.
 *
 * <p>Two resolver variants are produced from the same {@link software.amazon.smithy.java.rulesengine.Bytecode}:
 * <ul>
 *   <li>{@link BytecodeEndpointResolver} — interpreted bytecode dispatch (the existing approach)</li>
 *   <li>{@link GeneratedEndpointResolver} — code-generated Java class compiled at setup time,
 *       using thread-local field-based state and a {@link GeneratedEndpointResolver.GeneratedParameters}
 *       fast path that avoids {@link EndpointResolverParams} allocation on every call</li>
 * </ul>
 *
 * <h2>Parameter passing</h2>
 * All parameters are supplied via {@link RulesEngineSettings#ADDITIONAL_ENDPOINT_PARAMS} (the
 * "canned" mode from the smithy-java reference benchmarks). No input-shape extraction occurs.
 * {@link NullApiOperation} and {@link NullSerializableStruct} satisfy the non-null requirement
 * of {@link EndpointResolverParams} without contributing to resolution.
 */
public final class SmithyJavaResolverFactory {

    private SmithyJavaResolverFactory() {
    }

    /**
     * Holds both resolver variants built from the same compiled bytecode.
     */
    public static final class Resolvers {
        /** Interpreted bytecode dispatcher. */
        public final BytecodeEndpointResolver bytecode;
        /** Code-generated Java resolver (compiled at setup time via javax.tools). */
        public final EndpointResolver generated;

        private Resolvers(BytecodeEndpointResolver bytecode, EndpointResolver generated) {
            this.bytecode = bytecode;
            this.generated = generated;
        }
    }

    // ----- per-service convenience methods -------------------------------------------------------

    public static Resolvers forConnect() {
        return build("smithy-models/connect_model.json",
                     "com.amazonaws.connect#AmazonConnectService",
                     "GeneratedConnect");
    }

    public static Resolvers forDynamoDb() {
        return build("smithy-models/dynamodb_model.json",
                     "com.amazonaws.dynamodb#DynamoDB_20120810",
                     "GeneratedDynamoDb");
    }

    public static Resolvers forS3() {
        return build("smithy-models/s3_model.json",
                     "com.amazonaws.s3#AmazonS3",
                     "GeneratedS3");
    }

    public static Resolvers forLambda() {
        return build("smithy-models/lambda_model.json",
                     "com.amazonaws.lambda#AWSGirApiService",
                     "GeneratedLambda");
    }

    // ----- param construction helpers ------------------------------------------------------------

    /**
     * Build {@link EndpointResolverParams} for the bytecode resolver path.
     * All resolution parameters come from {@code ADDITIONAL_ENDPOINT_PARAMS}; the operation and
     * input value are no-op stubs.
     *
     * @param region           AWS region string, or {@code null} for no-region cases
     * @param additionalParams rule-set parameter map, e.g.
     *                         {@code Map.of("Region", "us-east-1", "UseFIPS", false, ...)}
     */
    public static EndpointResolverParams params(String region, Map<String, Object> additionalParams) {
        Context ctx = Context.create();
        if (region != null) {
            ctx.put(RegionSetting.REGION, region);
        }
        ctx.put(RulesEngineSettings.ADDITIONAL_ENDPOINT_PARAMS, additionalParams);
        return EndpointResolverParams.builder()
                                     .context(ctx)
                                     .operation(NullApiOperation.INSTANCE)
                                     .inputValue(NullSerializableStruct.INSTANCE)
                                     .build();
    }

    /**
     * Build pre-marshalled {@link GeneratedEndpointResolver.GeneratedParameters} for the generated
     * resolver's fast path. These are constructed once at setup time from the same
     * {@code additionalParams} map and reused across all benchmark invocations, avoiding allocation.
     */
    @SuppressWarnings("unchecked")
    public static GeneratedEndpointResolver.GeneratedParameters generatedParams(
            EndpointResolver generated, Map<String, Object> additionalParams) {
        return ((GeneratedEndpointResolver<?>) generated).createParameters(additionalParams);
    }

    // ----- core build logic ----------------------------------------------------------------------

    private static Resolvers build(String modelResource, String serviceShapeId, String generatedClassName) {
        URL modelUrl = SmithyJavaResolverFactory.class.getClassLoader().getResource(modelResource);
        if (modelUrl == null) {
            throw new IllegalStateException("Model resource not found on classpath: " + modelResource);
        }

        Model model = Model.assembler()
                           .addImport(modelUrl)
                           .putProperty(ModelAssembler.ALLOW_UNKNOWN_TRAITS, true)
                           .disableValidation()
                           .assemble()
                           .unwrap();

        ServiceShape service = model.expectShape(ShapeId.from(serviceShapeId), ServiceShape.class);
        RulesEngineBuilder engine = new RulesEngineBuilder();

        // Take the BDD trait from the model as-is — do NOT re-optimize. The trait was already
        // optimized when the model was generated; re-running sifting/cost/reversal on it hurts
        // performance rather than improving it.
        EndpointBddTrait bddTrait;
        if (service.hasTrait(EndpointBddTrait.class)) {
            bddTrait = service.expectTrait(EndpointBddTrait.class);
        } else {
            throw new IllegalStateException(
                    "Service " + serviceShapeId + " does not have the smithy.rules#endpointBdd trait. "
                    + "The smithy model must be generated with BDD support enabled.");
        }

        var bytecode = engine.compile(bddTrait);
        var bytecodeResolver = new BytecodeEndpointResolver(
                bytecode, engine.getExtensions(), engine.getBuiltinProviders());
        var generatedResolver = GeneratedResolverFactory.create(bytecode, generatedClassName);

        return new Resolvers(bytecodeResolver, generatedResolver);
    }
}
