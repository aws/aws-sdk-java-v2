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

package software.amazon.awssdk.codegen.poet.rules;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.jr.stree.JrsArray;
import com.fasterxml.jackson.jr.stree.JrsBoolean;
import com.fasterxml.jackson.jr.stree.JrsString;
import com.fasterxml.jackson.jr.stree.JrsValue;
import com.squareup.javapoet.CodeBlock;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import software.amazon.awssdk.awscore.endpoints.AwsEndpointAttribute;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4aAuthScheme;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ExpectModel;
import software.amazon.awssdk.codegen.model.service.EndpointTrait;
import software.amazon.awssdk.codegen.model.service.HostPrefixProcessor;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.utils.StringUtils;

public final class TestGeneratorUtils {
    private TestGeneratorUtils() {
    }

    public static CodeBlock createExpect(ExpectModel expect, OperationModel opModel, Map<String, TreeNode> opParams) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("$T.builder()", Expect.class);

        if (expect.getError() != null) {
            b.add(".error($S)", expect.getError());
        } else {
            CodeBlock.Builder endpointBuilder = CodeBlock.builder();

            ExpectModel.Endpoint endpoint = expect.getEndpoint();
            String expectedUrl = createExpectedUrl(endpoint, opModel, opParams);

            endpointBuilder.add("$T.builder()", Endpoint.class);
            endpointBuilder.add(".url($T.create($S))", URI.class, expectedUrl);


            if (endpoint.getHeaders() != null) {
                Map<String, List<String>> expectHeaders = endpoint.getHeaders();
                expectHeaders.forEach((name, values) -> {
                    values.forEach(v -> endpointBuilder.add(".putHeader($S, $S)", name, v));
                });
            }

            if (endpoint.getProperties() != null) {
                endpoint.getProperties().forEach((name, value) -> {
                    addEndpointAttributeBlock(endpointBuilder, name, value);
                });
            }

            endpointBuilder.add(".build()");

            b.add(".endpoint($L)", endpointBuilder.build());
        }

        b.add(".build()");

        return b.build();
    }

    public static Optional<String> getHostPrefixTemplate(OperationModel opModel) {
        EndpointTrait endpointTrait = opModel.getEndpointTrait();

        if (endpointTrait == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(endpointTrait.getHostPrefix());
    }

    private static void addEndpointAttributeBlock(CodeBlock.Builder builder, String attrName, TreeNode attrValue) {
        switch (attrName) {
            case "authSchemes":
                addAuthSchemesBlock(builder, attrValue);
                break;
            default:
                throw new RuntimeException("Encountered unknown expected endpoint attribute: " + attrName);
        }
    }

    private static void addAuthSchemesBlock(CodeBlock.Builder builder, TreeNode attrValue) {
        CodeBlock keyExpr = CodeBlock.builder()
                                     .add("$T.AUTH_SCHEMES", AwsEndpointAttribute.class)
                                     .build();

        CodeBlock.Builder schemesListExpr = CodeBlock.builder()
            .add("$T.asList(", Arrays.class);

        JrsArray schemesArray = (JrsArray) attrValue;

        Iterator<JrsValue> elementsIter = schemesArray.elements();
        while (elementsIter.hasNext()) {
            schemesListExpr.add("$L", authSchemeCreationExpr(elementsIter.next()));

            if (elementsIter.hasNext()) {
                schemesListExpr.add(",");
            }
        }
        schemesListExpr.add(")");

        builder.add(".putAttribute($L, $L)", keyExpr, schemesListExpr.build());
    }


    private static CodeBlock authSchemeCreationExpr(TreeNode attrValue) {
        CodeBlock.Builder schemeExpr = CodeBlock.builder();
        String name = ((JrsString) attrValue.get("name")).getValue();
        switch (name) {
            case "sigv4":
                schemeExpr.add("$T.builder()", SigV4AuthScheme.class);
                break;
            case "sigv4a":
                schemeExpr.add("$T.builder()", SigV4aAuthScheme.class);
                break;
            default:
                throw new RuntimeException("Unknown expected auth scheme: " + name);
        }

        Iterator<String> membersIter = attrValue.fieldNames();

        while (membersIter.hasNext()) {
            String memberName = membersIter.next();
            TreeNode memberValue = attrValue.get(memberName);
            switch (memberName) {
                case "name":
                    break;
                case "signingName":
                    schemeExpr.add(".signingName($S)", ((JrsString) memberValue).getValue());
                    break;
                case "signingRegion":
                    schemeExpr.add(".signingRegion($S)", ((JrsString) memberValue).getValue());
                    break;
                case "disableDoubleEncoding":
                    schemeExpr.add(".disableDoubleEncoding($L)", ((JrsBoolean) memberValue).booleanValue());
                    break;
                case "signingRegionSet": {
                    JrsArray regions = (JrsArray) memberValue;
                    regions.elements().forEachRemaining(r -> {
                        schemeExpr.add(".addSigningRegion($S)", ((JrsString) r).getValue());
                    });
                    break;
                }
                default:
                    throw new RuntimeException("Unknown auth scheme property: " + memberName);
            }
        }
        schemeExpr.add(".build()");

        return schemeExpr.build();
    }


    private static String createExpectedUrl(ExpectModel.Endpoint endpoint,
                                            OperationModel opModel,
                                            Map<String, TreeNode> opParams) {
        Optional<String> prefix = getHostPrefix(opModel, opParams);
        if (!prefix.isPresent()) {
            return endpoint.getUrl();
        }

        URI originalUrl = URI.create(endpoint.getUrl());

        try {
            URI newUrl = new URI(originalUrl.getScheme(),
                                 null,
                                 prefix.get() + originalUrl.getHost(),
                                 originalUrl.getPort(),
                                 originalUrl.getPath(),
                                 originalUrl.getQuery(),
                                 originalUrl.getFragment());
            return newUrl.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Expected url creation failed", e);
        }
    }

    private static Optional<String> getHostPrefix(OperationModel opModel, Map<String, TreeNode> opParams) {
        if (opModel == null) {
            return Optional.empty();
        }

        Optional<String> hostPrefixTemplate = getHostPrefixTemplate(opModel);

        if (!hostPrefixTemplate.isPresent() || StringUtils.isBlank(hostPrefixTemplate.get())) {
            return Optional.empty();
        }

        HostPrefixProcessor processor = new HostPrefixProcessor(hostPrefixTemplate.get());

        String pattern = processor.hostWithStringSpecifier();

        for (String c2jName : processor.c2jNames()) {
            if (opParams != null && opParams.containsKey(c2jName)) {
                String value = ((JrsString) opParams.get(c2jName)).getValue();
                pattern = StringUtils.replaceOnce(pattern, "%s", value);
            } else {
                pattern = StringUtils.replaceOnce(pattern, "%s", "aws");
            }
        }

        return Optional.of(pattern);
    }
}
