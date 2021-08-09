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

package software.amazon.awssdk.protocols.json.internal.unmarshall.document;

import java.util.LinkedHashMap;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkNumber;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.protocols.json.internal.dom.SdkArrayNode;
import software.amazon.awssdk.protocols.json.internal.dom.SdkJsonNode;
import software.amazon.awssdk.protocols.json.internal.dom.SdkNullNode;
import software.amazon.awssdk.protocols.json.internal.dom.SdkObjectNode;
import software.amazon.awssdk.protocols.json.internal.dom.SdkScalarNode;
import software.amazon.awssdk.protocols.json.internal.visitor.SdkJsonNodeVisitor;

@SdkInternalApi
public class DocumentUnmarshaller implements SdkJsonNodeVisitor<Document> {

    private Document visitMap(SdkJsonNode jsonContent) {
        return Document.fromMap(jsonContent.fields().entrySet()
                .stream().collect(Collectors.toMap(entry -> entry.getKey(), entry -> visit(entry.getValue()),
                        (left, right) -> left,
                        LinkedHashMap::new)));
    }

    private Document visitList(SdkJsonNode jsonContent) {
        return Document.fromList(
                ((SdkArrayNode) jsonContent).items().stream()
                        .map(item -> visit(item)).collect(Collectors.toList()));
    }

    private Document visitScalar(SdkJsonNode jsonContent) {
        SdkScalarNode sdkScalarNode = (SdkScalarNode) jsonContent;

        switch (sdkScalarNode.getNodeType()) {
            case BOOLEAN:
                return Document.fromBoolean(Boolean.valueOf(sdkScalarNode.asText()));
            case NUMBER:
                return Document.fromNumber(SdkNumber.fromString(jsonContent.asText()));
            default:
                return Document.fromString(sdkScalarNode.asText());
        }
    }

    @Override
    public Document visit(SdkJsonNode sdkJsonNode) {

        if (sdkJsonNode instanceof SdkScalarNode) {
            return visitScalar(sdkJsonNode);
        } else if (sdkJsonNode instanceof SdkObjectNode) {
            return visitMap(sdkJsonNode);
        } else if (sdkJsonNode instanceof SdkArrayNode) {
            return visitList(sdkJsonNode);
        } else if (sdkJsonNode instanceof SdkNullNode) {
            return visitNull();
        } else {
            throw new IllegalStateException("Visitor not defined for " + sdkJsonNode);
        }
    }

    private Document visitNull() {
        return Document.fromNull();
    }

}
