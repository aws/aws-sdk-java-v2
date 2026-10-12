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

package software.amazon.awssdk.codegen.smithy.transform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.codegen.smithy.transform.TransformTestSupport.REST_JSON_PREFIX;
import static software.amazon.awssdk.codegen.smithy.transform.TransformTestSupport.apply;
import static software.amazon.awssdk.codegen.smithy.transform.TransformTestSupport.model;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;

class AddShapesTransformerTest {

    private static final Model MODEL = model(
        REST_JSON_PREFIX
        + "service DemoService { version: \"2024-01-01\", operations: [Get] }\n"
        + "@http(method: \"POST\", uri: \"/get\")\n"
        + "operation Get { input: GetRequest, output: GetResponse }\n"
        + "structure GetRequest {}\n"
        + "structure GetResponse {}\n"
        + "string BucketName\n");

    private static final String SDK_PART_TYPE =
        "\"demo#SdkPartType\": {\"type\": \"enum\", \"members\": {"
        + "\"DEFAULT\": {\"target\": \"smithy.api#Unit\", \"traits\": {\"smithy.api#enumValue\": \"DEFAULT\"}},"
        + "\"LAST\": {\"target\": \"smithy.api#Unit\", \"traits\": {\"smithy.api#enumValue\": \"LAST\"}}}}";

    private final AddShapesTransformer transformer = new AddShapesTransformer();

    @Test
    void name_isAddShapes() {
        assertThat(transformer.getName()).isEqualTo("addShapes");
    }

    @Test
    void enumShape_isAdded() {
        Model result = apply(transformer, MODEL, "{\"shapes\": {" + SDK_PART_TYPE + "}}");

        EnumShape added = result.expectShape(ShapeId.from("demo#SdkPartType"), EnumShape.class);
        assertThat(added.getEnumValues()).containsOnlyKeys("DEFAULT", "LAST");
    }

    @Test
    void structureReferencingExistingShapes_isAdded() {
        Model result = apply(transformer, MODEL,
                             "{\"shapes\": {\"demo#Source\": {\"type\": \"structure\", \"members\": {"
                             + "\"Bucket\": {\"target\": \"demo#BucketName\"}}}}}");

        StructureShape added = result.expectShape(ShapeId.from("demo#Source"), StructureShape.class);
        assertThat(added.getMember("Bucket").get().getTarget()).isEqualTo(ShapeId.from("demo#BucketName"));
    }

    @Test
    void existingShape_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"shapes\": {\"demo#BucketName\": {\"type\": \"string\"}}}"))
            .hasMessageContaining("shape 'demo#BucketName' already exists");
    }

    @Test
    void preludeNamespace_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"shapes\": {\"smithy.api#Mine\": {\"type\": \"string\"}}}"))
            .hasMessageContaining("prelude namespace");
    }

    @Test
    void malformedDefinition_fails() {
        assertThatThrownBy(() -> apply(transformer, MODEL,
                                       "{\"shapes\": {\"demo#Broken\": {\"type\": \"notAType\"}}}"))
            .hasMessageContaining("addShapes");
    }
}
