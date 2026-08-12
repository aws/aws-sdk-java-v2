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
import software.amazon.smithy.java.endpoints.EndpointResolverParams;
import software.amazon.smithy.java.rulesengine.BytecodeEndpointResolver;
import software.amazon.smithy.java.rulesengine.RulesEngineBuilder;
import software.amazon.smithy.java.rulesengine.RulesEngineSettings;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.ModelAssembler;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.rulesengine.logic.bdd.CostOptimization;
import software.amazon.smithy.rulesengine.logic.bdd.NodeReversal;
import software.amazon.smithy.rulesengine.logic.bdd.SiftingOptimization;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;

/**
 * Compiles a smithy-java {@link BytecodeEndpointResolver} from a smithy model JSON resource.
 *
 * <p>The BDD is compiled through the same optimization pipeline used in the smithy-java reference
 * benchmarks: sifting → cost → node-reversal. Compilation happens once at trial setup so it is
 * excluded from measurements.
 *
 * <h2>Parameter passing</h2>
 * All endpoint parameters are supplied via
 * {@link RulesEngineSettings#ADDITIONAL_ENDPOINT_PARAMS} on the {@link Context}, matching the
 * "canned" param mode from the smithy-java reference benchmarks. This means the resolver reads
 * directly from the pre-built params map and performs no extraction from the input shape — the
 * {@link NullApiOperation} and {@link NullSerializableStruct} stubs satisfy the
 * {@link EndpointResolverParams} non-null requirement without contributing anything to resolution.
 */
public final class SmithyJavaResolverFactory {

    private SmithyJavaResolverFactory() {
    }

    // ----- per-service convenience methods -------------------------------------------------------

    public static BytecodeEndpointResolver forConnect() {
        return compile("smithy-models/connect_model.json",
                       "com.amazonaws.connect#AmazonConnectService");
    }

    public static BytecodeEndpointResolver forDynamoDb() {
        return compile("smithy-models/dynamodb_model.json",
                       "com.amazonaws.dynamodb#DynamoDB_20120810");
    }

    public static BytecodeEndpointResolver forS3() {
        return compile("smithy-models/s3_model.json",
                       "com.amazonaws.s3#AmazonS3");
    }

    public static BytecodeEndpointResolver forLambda() {
        return compile("smithy-models/lambda_model.json",
                       "com.amazonaws.lambda#AWSGirApiService");
    }

    // ----- param construction helper -------------------------------------------------------------

    /**
     * Build an {@link EndpointResolverParams} for the smithy-java resolver.
     *
     * <p>All resolution parameters are supplied via {@code ADDITIONAL_ENDPOINT_PARAMS} (using the
     * exact parameter names from the service rule set, e.g. {@code "Region"}, {@code "UseFIPS"}).
     * {@link RegionSetting#REGION} is also set on the context so the resolver's built-in region
     * provider can read it. The operation and input value are no-op stubs.
     *
     * @param region           the AWS region string, or {@code null} for no-region cases
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

    // ----- BDD compilation -----------------------------------------------------------------------

    private static BytecodeEndpointResolver compile(String modelResource, String serviceShapeId) {
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

        // Use the pre-built BDD trait when present; fall back to deriving from the rule set.
        EndpointBddTrait bddTrait;
        if (service.hasTrait(EndpointBddTrait.class)) {
            bddTrait = service.expectTrait(EndpointBddTrait.class);
        } else {
            var ruleSet = service.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet();
            bddTrait = EndpointBddTrait.from(Cfg.from(ruleSet));
        }

        // Full optimization pipeline: sifting → cost → node-reversal, matching the reference benchmarks.
        // Fall back to the pre-built BDD trait without re-optimization if sifting fails (e.g., S3's
        // large BDD can trigger size-mismatch errors in the sifting optimizer).
        EndpointBddTrait optimizedTrait;
        try {
            var cfg = Cfg.from(service.expectTrait(EndpointRuleSetTrait.class).getEndpointRuleSet());
            var sifted = SiftingOptimization.builder().cfg(cfg).build().apply(bddTrait);
            var costOpt = CostOptimization.builder().cfg(cfg).build().apply(sifted);
            optimizedTrait = new NodeReversal().apply(costOpt);
        } catch (Exception e) {
            // Optimization failed (e.g., sifting size mismatch on large rule sets like S3).
            // Fall back to compiling the pre-built BDD trait as-is.
            System.out.println("[SmithyJavaResolverFactory] Optimization pipeline failed for "
                               + serviceShapeId + ", using pre-built BDD trait: " + e.getMessage());
            optimizedTrait = bddTrait;
        }

        return new BytecodeEndpointResolver(
                engine.compile(optimizedTrait),
                engine.getExtensions(),
                engine.getBuiltinProviders());
    }
}
