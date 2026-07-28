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

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.IntermediateModelShapeProcessor;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeType;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Integration test for the four Smithy shape processors —
 * {@link AddSmithyInputShapes}, {@link AddSmithyOutputShapes},
 * {@link AddSmithyExceptionShapes}, {@link AddSmithyModelShapes}. Runs
 * them in the same order PR 5's builder will run them and asserts on the
 * combined {@code Map<String, ShapeModel>} output.
 */
class AddSmithyProcessorsTest {

    private static final String IDL_HEADER =
        "$version: \"2.0\"\n"
        + "namespace demo\n\n"
        + "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#restJson1\n\n"
        + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
        + "@restJson1\n"
        + "@sigv4(name: \"demo\")\n";

    private static Model loadModel(String body) {
        return Model.assembler()
                    .discoverModels(Model.class.getClassLoader())
                    .addUnparsedModel("test.smithy", IDL_HEADER + body)
                    .assemble()
                    .unwrap();
    }

    private static Map<String, ShapeModel> runProcessorChain(Model model, String protocol) {
        ServiceShape service = model.getServiceShapes().iterator().next();
        NamingStrategy naming = new DefaultSmithyNamingStrategy(model, service, CustomizationConfig.create());
        TypeUtils typeUtils = new TypeUtils(naming);
        CustomizationConfig cc = CustomizationConfig.create();

        IntermediateModelShapeProcessor[] processors = {
            new AddSmithyInputShapes(model, service, naming, cc, protocol, typeUtils),
            new AddSmithyOutputShapes(model, service, naming, cc, protocol, typeUtils),
            new AddSmithyExceptionShapes(model, service, naming, cc, protocol, typeUtils),
            new AddSmithyModelShapes(model, service, naming, cc, protocol, typeUtils),
        };

        Map<String, ShapeModel> shapes = new HashMap<>();
        Map<String, OperationModel> ops = emptyMap();
        for (IntermediateModelShapeProcessor p : processors) {
            shapes.putAll(p.process(ops, Collections.unmodifiableMap(shapes)));
        }
        return shapes;
    }

    // ---- Full pipeline coverage on a small service ------------------------

    @Test
    void fourProcessors_produceRequestResponseExceptionAndModelShapes() {
        Model model = loadModel(
            "service DemoService { version: \"2024-01-01\", operations: [PutThing, GetThing] }\n"
            + "@http(method: \"PUT\", uri: \"/things/{id}\")\n"
            + "operation PutThing { input: PutThingInput, output: PutThingOutput, errors: [ConflictException] }\n"
            + "@http(method: \"GET\", uri: \"/things/{id}\")\n"
            + "operation GetThing { input: GetThingInput, output: GetThingOutput, errors: [ConflictException] }\n"
            + "\n"
            + "structure PutThingInput {\n"
            + "    @required @httpLabel id: String\n"
            + "    payload: ThingPayload\n"
            + "}\n"
            + "structure PutThingOutput { thing: Thing }\n"
            + "structure GetThingInput { @required @httpLabel id: String }\n"
            + "structure GetThingOutput { thing: Thing }\n"
            + "structure ThingPayload { data: String }\n"
            + "structure Thing { id: String, name: String, status: Status }\n"
            + "enum Status { ACTIVE, INACTIVE }\n"
            + "@error(\"client\") @httpError(409) structure ConflictException { message: String }\n");

        Map<String, ShapeModel> shapes = runProcessorChain(model, "rest-json");

        // Requests
        assertThat(shapes).containsKeys("PutThingRequest", "GetThingRequest");
        assertThat(shapes.get("PutThingRequest").getType()).isEqualTo(ShapeType.Request.getValue());
        assertThat(shapes.get("PutThingRequest").getMarshaller()).isNotNull();
        assertThat(shapes.get("PutThingRequest").getMarshaller().getVerb()).isEqualTo("PUT");
        assertThat(shapes.get("PutThingRequest").getMarshaller().getRequestUri()).isEqualTo("/things/{id}");

        // Responses
        assertThat(shapes).containsKeys("PutThingResponse", "GetThingResponse");
        assertThat(shapes.get("PutThingResponse").getType()).isEqualTo(ShapeType.Response.getValue());
        assertThat(shapes.get("PutThingResponse").getUnmarshaller()).isNotNull();

        // Exception (shared across operations — one entry, not two).
        assertThat(shapes).containsKey("ConflictException");
        assertThat(shapes.get("ConflictException").getType()).isEqualTo(ShapeType.Exception.getValue());
        assertThat(shapes.get("ConflictException").getHttpStatusCode()).isEqualTo(409);

        // Model shapes (Thing, ThingPayload, Status).
        assertThat(shapes).containsKeys("Thing", "ThingPayload", "Status");
        assertThat(shapes.get("Thing").getType()).isEqualTo(ShapeType.Model.getValue());
        assertThat(shapes.get("Status").getType()).isEqualTo(ShapeType.Enum.getValue());
    }

    // ---- Empty request / response synthesis -------------------------------

    @Test
    void unitInputAndOutput_synthesizeEmptyRequestAndResponse() {
        Model model = loadModel(
            "service DemoService { version: \"2024-01-01\", operations: [Ping] }\n"
            + "@http(method: \"GET\", uri: \"/ping\")\n"
            + "operation Ping { input: Unit, output: Unit }\n");

        Map<String, ShapeModel> shapes = runProcessorChain(model, "rest-json");

        assertThat(shapes).containsKey("PingRequest");
        assertThat(shapes.get("PingRequest").getMembers()).isEmpty();
        assertThat(shapes.get("PingRequest").getMarshaller().getIsSynthetic()).isTrue();

        assertThat(shapes).containsKey("PingResponse");
        assertThat(shapes.get("PingResponse").getMembers()).isEmpty();
        assertThat(shapes.get("PingResponse").getUnmarshaller()).isNotNull();
    }

    // ---- JSON RPC marshaller target ---------------------------------------

    @Test
    void awsJsonProtocol_defaultsVerbToPostAndUriToSlashAndSetsTarget() {
        // Full standalone model with only awsJson1_1.
        Model model = Model.assembler()
                           .discoverModels(Model.class.getClassLoader())
                           .addUnparsedModel("rpc.smithy",
                               "$version: \"2.0\"\nnamespace demo\n\n"
                               + "use aws.api#service\n"
                               + "use aws.auth#sigv4\n"
                               + "use aws.protocols#awsJson1_1\n"
                               + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
                               + "@awsJson1_1\n"
                               + "@sigv4(name: \"demo\")\n"
                               + "service Demo { version: \"2024-01-01\", operations: [Compute] }\n"
                               + "operation Compute { input: In, output: Out }\n"
                               + "structure In { x: Integer }\n"
                               + "structure Out { y: Integer }\n")
                           .assemble()
                           .unwrap();

        Map<String, ShapeModel> shapes = runProcessorChain(model, "json");

        assertThat(shapes.get("ComputeRequest").getMarshaller().getVerb()).isEqualTo("POST");
        assertThat(shapes.get("ComputeRequest").getMarshaller().getRequestUri()).isEqualTo("/");
        // For the awsJson family the target is <targetPrefix>.<operationName>, where the target
        // prefix equals the Smithy service shape name (C2J's ServiceMetadata.getTargetPrefix()).
        assertThat(shapes.get("ComputeRequest").getMarshaller().getTarget()).isEqualTo("Demo.Compute");
    }

    // ---- query/ec2 target: operation identifier but no target prefix ------

    @Test
    void queryProtocol_setsBareOperationNameAsTarget() {
        Model model = Model.assembler()
                           .discoverModels(Model.class.getClassLoader())
                           .addUnparsedModel("query.smithy",
                               "$version: \"2.0\"\nnamespace demo\n\n"
                               + "use aws.api#service\n"
                               + "use aws.auth#sigv4\n"
                               + "use aws.protocols#awsQuery\n"
                               + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
                               + "@awsQuery\n"
                               + "@xmlNamespace(uri: \"https://demo.amazonaws.com/\")\n"
                               + "@sigv4(name: \"demo\")\n"
                               + "service Demo { version: \"2024-01-01\", operations: [Compute] }\n"
                               + "operation Compute { input: In, output: Out }\n"
                               + "structure In { x: Integer }\n"
                               + "structure Out { y: Integer }\n")
                           .assemble()
                           .unwrap();

        Map<String, ShapeModel> shapes = runProcessorChain(model, "query");

        // query uses an operation identifier but has no target prefix — bare operation name.
        assertThat(shapes.get("ComputeRequest").getMarshaller().getTarget()).isEqualTo("Compute");
    }

    // ---- Exception dedup + errorCode --------------------------------------

    @Test
    void sharedExceptionAcrossOperations_isOnlyProducedOnce() {
        Model model = loadModel(
            "service DemoService { version: \"2024-01-01\", operations: [A, B] }\n"
            + "@http(method: \"GET\", uri: \"/a\")\n"
            + "operation A { input: Unit, output: Unit, errors: [Boom] }\n"
            + "@http(method: \"GET\", uri: \"/b\")\n"
            + "operation B { input: Unit, output: Unit, errors: [Boom] }\n"
            + "@error(\"server\") @httpError(500)\n"
            + "structure Boom { message: String }\n");

        Map<String, ShapeModel> shapes = runProcessorChain(model, "rest-json");

        assertThat(shapes).containsKey("BoomException");
        assertThat(shapes.get("BoomException").getErrorCode()).isEqualTo("Boom");
        assertThat(shapes.get("BoomException").getHttpStatusCode()).isEqualTo(500);
        assertThat(shapes.get("BoomException").isFault()).isTrue();
    }

    // ---- Reachability walker skips unreachable shapes ---------------------

    @Test
    void modelShapesProcessor_skipsShapesUnreferencedByOperations() {
        Model model = loadModel(
            "service DemoService { version: \"2024-01-01\", operations: [GetOne] }\n"
            + "@http(method: \"GET\", uri: \"/one\")\n"
            + "operation GetOne { input: Unit, output: GetOneOutput }\n"
            + "structure GetOneOutput { thing: Thing }\n"
            + "structure Thing { id: String }\n"
            + "structure Orphan { junk: String }\n");

        Map<String, ShapeModel> shapes = runProcessorChain(model, "rest-json");

        assertThat(shapes).containsKey("Thing");
        assertThat(shapes).doesNotContainKey("Orphan");
    }

    // ---- Model-shape processor dedup against currentShapes ----------------

    @Test
    void modelShapesProcessor_doesNotDuplicateAlreadyTranslatedShapes() {
        // A shape that is both a top-level input AND referenced from another
        // operation's output should not appear twice. The input processor
        // won't add it as an input by name (because the operation's input is
        // Unit), but reachability from output should still succeed.
        Model model = loadModel(
            "service DemoService { version: \"2024-01-01\", operations: [Op] }\n"
            + "@http(method: \"POST\", uri: \"/things\")\n"
            + "operation Op { input: OpInput, output: OpOutput }\n"
            + "structure OpInput { thing: Thing }\n"
            + "structure OpOutput { thing: Thing }\n"
            + "structure Thing { id: String }\n");

        Map<String, ShapeModel> shapes = runProcessorChain(model, "rest-json");

        // Thing appears once in the model-shape map, not duplicated.
        assertThat(shapes).containsKeys("OpRequest", "OpResponse", "Thing");
        assertThat(shapes.get("Thing").getType()).isEqualTo(ShapeType.Model.getValue());
    }
}
