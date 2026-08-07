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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.internal.TypeUtils;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.naming.DefaultSmithyNamingStrategy;
import software.amazon.awssdk.codegen.naming.NamingStrategy;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Tests for the awsQuery response {@code unmarshaller.resultWrapper}, which only awsQuery uses.
 * Surfaced by the sts fixture.
 */
class AddSmithyOutputShapesTest {

    private static Model modelOf(String protocolUse, String serviceTraits) {
        String src =
            "$version: \"2.0\"\nnamespace demo\n\n"
            + "use aws.api#service\n"
            + "use aws.auth#sigv4\n"
            + protocolUse
            + "\n"
            + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
            + "@sigv4(name: \"demo\")\n"
            + serviceTraits
            + "service DemoService { version: \"2024-01-01\", operations: [Op] }\n\n"
            + "@http(method: \"POST\", uri: \"/op\")\n"
            + "operation Op { input: OpRequest, output: OpResponse }\n"
            + "structure OpRequest {}\n"
            + "structure OpResponse { name: String }\n";
        return Model.assembler()
                    .discoverModels(Model.class.getClassLoader())
                    .addUnparsedModel("test.smithy", src)
                    .assemble()
                    .unwrap();
    }

    private static Map<String, ShapeModel> outputs(Model model, String protocol) {
        ServiceShape service = model.getServiceShapes().iterator().next();
        NamingStrategy naming = new DefaultSmithyNamingStrategy(model, service, CustomizationConfig.create());
        TypeUtils typeUtils = new TypeUtils(naming);
        return new AddSmithyOutputShapes(model, service, naming, CustomizationConfig.create(), protocol, typeUtils)
            .process(Collections.emptyMap(), Collections.emptyMap());
    }

    @Test
    void awsQuery_setsResultWrapperToOperationNamePlusResult() {
        Model model = modelOf(
            "use aws.protocols#awsQuery\n",
            "@awsQuery\n@xmlNamespace(uri: \"https://demo.amazonaws.com/doc/2024-01-01/\")\n");
        assertThat(outputs(model, "query").get("OpResponse").getUnmarshaller().getResultWrapper())
            .isEqualTo("OpResult");
    }

    @Test
    void ec2Query_hasNoResultWrapper() {
        // ec2Query responses are not result-wrapped; the ec2 C2J models carry no resultWrapper.
        Model model = modelOf(
            "use aws.protocols#ec2Query\n",
            "@ec2Query\n@xmlNamespace(uri: \"https://demo.amazonaws.com/doc/2024-01-01/\")\n");
        assertThat(outputs(model, "ec2").get("OpResponse").getUnmarshaller().getResultWrapper())
            .isNull();
    }

    @Test
    void restJson_hasNoResultWrapper() {
        Model model = modelOf("use aws.protocols#restJson1\n", "@restJson1\n");
        assertThat(outputs(model, "rest-json").get("OpResponse").getUnmarshaller().getResultWrapper())
            .isNull();
    }
}
