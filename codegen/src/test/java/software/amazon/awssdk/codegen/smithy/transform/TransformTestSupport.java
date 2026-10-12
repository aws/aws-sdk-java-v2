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

import software.amazon.smithy.build.ProjectionTransformer;
import software.amazon.smithy.build.TransformContext;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.transform.ModelTransformer;

final class TransformTestSupport {

    static final String REST_JSON_PREFIX =
        "$version: \"2.0\"\nnamespace demo\n\n"
        + "use aws.api#service\n"
        + "use aws.auth#sigv4\n"
        + "use aws.protocols#restJson1\n"
        + "@service(sdkId: \"Demo\", arnNamespace: \"demo\")\n"
        + "@sigv4(name: \"demo\")\n"
        + "@restJson1\n";

    private TransformTestSupport() {
    }

    static Model model(String idl) {
        return Model.assembler()
                    .discoverModels(TransformTestSupport.class.getClassLoader())
                    .addUnparsedModel("test.smithy", idl)
                    .assemble()
                    .unwrap();
    }

    /**
     * Applies the transform and validates the result, so a transform that leaves a broken model fails the test.
     */
    static Model apply(ProjectionTransformer transformer, Model model, String settingsJson) {
        Model result = transformer.transform(TransformContext.builder()
                                                            .model(model)
                                                            .transformer(ModelTransformer.create())
                                                            .settings(Node.parse(settingsJson).expectObjectNode())
                                                            .build());
        return Model.assembler()
                    .discoverModels(TransformTestSupport.class.getClassLoader())
                    .addModel(result)
                    .assemble()
                    .unwrap();
    }
}
