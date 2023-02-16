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

import static software.amazon.awssdk.codegen.poet.rules.TestGeneratorUtils.getHostPrefixTemplate;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.jr.stree.JrsArray;
import com.fasterxml.jackson.jr.stree.JrsObject;
import com.fasterxml.jackson.jr.stree.JrsValue;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import javax.lang.model.element.Modifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.ParameterHttpMapping;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.EndpointTestSuiteModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.OperationInput;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.service.ClientContextParam;
import software.amazon.awssdk.codegen.model.service.Location;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.PoetExtension;
import software.amazon.awssdk.codegen.poet.PoetUtils;
import software.amazon.awssdk.codegen.utils.AuthUtils;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.rules.testing.AsyncTestCase;
import software.amazon.awssdk.core.rules.testing.BaseRuleSetClientTest;
import software.amazon.awssdk.core.rules.testing.SyncTestCase;
import software.amazon.awssdk.core.rules.testing.util.EmptyPublisher;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.utils.Validate;

public class EndpointRulesClientTestSpec implements ClassSpec {
    /**
     * Many of the services, (especially the services whose rules are completely auto generated), share a same set of tests
     * that fail for the SDK (with a valid reason).
     */
    private static final Map<String, String> GLOBAL_SKIP_ENDPOINT_TESTS;

    static {
        Map<String, String> tests = new HashMap<>();
        tests.put("For region us-iso-west-1 with FIPS enabled and DualStack enabled", "Client builder does the validation");
        tests.put("For region us-iso-west-1 with FIPS disabled and DualStack enabled", "Client builder does the validation");
        tests.put("For region us-iso-east-1 with FIPS enabled and DualStack enabled", "Client builder does the validation");
        tests.put("For region us-iso-east-1 with FIPS disabled and DualStack enabled", "Client builder does the validation");
        tests.put("For region us-isob-east-1 with FIPS enabled and DualStack enabled", "Client builder does the validation");
        tests.put("For region us-isob-east-1 with FIPS disabled and DualStack enabled", "Client builder does the validation");
        tests.put("For region us-isob-west-1 with FIPS enabled and DualStack enabled", "Client builder does the validation");
        tests.put("For region us-isob-west-1 with FIPS disabled and DualStack enabled", "Client builder does the validation");
        GLOBAL_SKIP_ENDPOINT_TESTS = Collections.unmodifiableMap(tests);

    }

    private final IntermediateModel model;
    private final EndpointRulesSpecUtils endpointRulesSpecUtils;
    private final PoetExtension poetExtension;

    public EndpointRulesClientTestSpec(IntermediateModel model) {
        this.model = model;
        this.endpointRulesSpecUtils = new EndpointRulesSpecUtils(model);
        this.poetExtension = new PoetExtension(model);
    }

    @Override
    public TypeSpec poetSpec() {
        TypeSpec.Builder b = PoetUtils.createClassBuilder(className())
                                      .addModifiers(Modifier.PUBLIC)
                                      .superclass(BaseRuleSetClientTest.class);

        if (endpointRulesSpecUtils.isS3()) {
            b.addField(s3RegionEndpointSystemPropertySaveValueField());
        }

        b.addMethod(methodSetupMethod());
        b.addMethod(teardownMethod());

        if (hasSyncClient()) {
            b.addMethod(syncTest());
        }
        b.addMethod(asyncTest());

        if (hasSyncClient()) {
            b.addMethod(syncTestsSourceMethod());
        }
        b.addMethod(asyncTestsSourceMethod());

        return b.build();
    }

    @Override
    public ClassName className() {
        return endpointRulesSpecUtils.clientEndpointTestsName();
    }

    private String findDefaultRequest() {
        Map<String, OperationModel> operations = this.model.getOperations();

        // If this is an endpoint discovery service, then use the endpoint
        // operation since it goes to the service endpoint
        Optional<String> endpointOperation = operations.entrySet()
                                                       .stream()
                                                       .filter(e -> e.getValue().isEndpointOperation())
                                                       .map(Map.Entry::getKey)
                                                       .findFirst();

        if (endpointOperation.isPresent()) {
            return endpointOperation.get();
        }

        // Ideally look for something that we don't need to set any parameters
        // on. That means either a request with no members or one that does not
        // have any members bound to the URI path
        Optional<String> name = operations.entrySet().stream()
                                          .filter(e -> canBeEmpty(e.getValue()))
                                          .findFirst()
                                          .map(Map.Entry::getKey);

        if (name.isPresent()) {
            return name.get();
        }

        // Settle for a non-streaming operation...
        Optional<String> nonStreaming = operations.entrySet().stream().filter(e ->
                                                                                  !e.getValue().hasStreamingInput()
                                                                                  && !e.getValue().hasStreamingOutput())
                                                  .map(Map.Entry::getKey)
                                                  .findFirst();


        // Failing that, just pick the first one
        return nonStreaming.orElseGet(() -> operations.keySet().stream().findFirst().get());
    }

    private MethodSpec syncTest() {
        AnnotationSpec methodSourceSpec = AnnotationSpec.builder(MethodSource.class)
                                                        .addMember("value", "$S", "syncTestCases")
                                                        .build();

        MethodSpec.Builder b = MethodSpec.methodBuilder("syncClient_usesCorrectEndpoint")
                                         .addModifiers(Modifier.PUBLIC)
                                         .addParameter(SyncTestCase.class, "tc")
                                         .addAnnotation(methodSourceSpec)
                                         .addAnnotation(ParameterizedTest.class)
                                         .returns(void.class);

        b.addStatement("runAndVerify(tc)");

        return b.build();

    }

    private MethodSpec asyncTest() {
        AnnotationSpec methodSourceSpec = AnnotationSpec.builder(MethodSource.class)
                                                        .addMember("value", "$S", "asyncTestCases")
                                                        .build();

        MethodSpec.Builder b = MethodSpec.methodBuilder("asyncClient_usesCorrectEndpoint")
                                         .addModifiers(Modifier.PUBLIC)
                                         .addParameter(AsyncTestCase.class, "tc")
                                         .addAnnotation(methodSourceSpec)
                                         .addAnnotation(ParameterizedTest.class)
                                         .returns(void.class);

        b.addStatement("runAndVerify(tc)");

        return b.build();

    }

    private MethodSpec syncTestsSourceMethod() {
        String defaultOperation = findDefaultRequest();
        OperationModel defaultOpModel = model.getOperation(defaultOperation);

        MethodSpec.Builder b = MethodSpec.methodBuilder("syncTestCases")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .returns(ParameterizedTypeName.get(List.class, SyncTestCase.class));

        b.addCode("return $T.asList(", Arrays.class);

        EndpointTestSuiteModel endpointTestSuiteModel = model.getEndpointTestSuiteModel();
        Iterator<EndpointTestModel> testIter = endpointTestSuiteModel.getTestCases().iterator();

        while (testIter.hasNext()) {
            EndpointTestModel test = testIter.next();

            if (test.getOperationInputs() != null) {
                Iterator<OperationInput> operationInputsIter = test.getOperationInputs().iterator();
                while (operationInputsIter.hasNext()) {
                    OperationInput opInput = operationInputsIter.next();
                    OperationModel opModel = model.getOperation(opInput.getOperationName());

                    b.addCode("new $T($S, $L, $L$L)",
                              SyncTestCase.class,
                              test.getDocumentation(),
                              syncOperationCallLambda(opModel, test.getParams(), opInput.getOperationParams()),
                              TestGeneratorUtils.createExpect(test.getExpect(), opModel, opInput.getOperationParams()),
                              getSkipReasonBlock(test.getDocumentation()));

                    if (operationInputsIter.hasNext()) {
                        b.addCode(",");
                    }
                }
            } else {
                b.addCode("new $T($S, $L, $L$L)",
                          SyncTestCase.class,
                          test.getDocumentation(),
                          syncOperationCallLambda(defaultOpModel, test.getParams(), Collections.emptyMap()),
                          TestGeneratorUtils.createExpect(test.getExpect(), defaultOpModel, null),
                          getSkipReasonBlock(test.getDocumentation()));
            }

            if (testIter.hasNext()) {
                b.addCode(",");
            }
        }

        b.addStatement(")");

        return b.build();
    }

    private CodeBlock syncOperationCallLambda(OperationModel opModel, Map<String, TreeNode> params,
                                              Map<String, TreeNode> opParams) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.beginControlFlow("() -> ");
        b.addStatement("$T builder = $T.builder()", syncClientBuilder(), syncClientClass());
        b.addStatement("builder.credentialsProvider($T.CREDENTIALS_PROVIDER)", BaseRuleSetClientTest.class);
        if (AuthUtils.usesBearerAuth(model)) {
            b.addStatement("builder.tokenProvider($T.TOKEN_PROVIDER)", BaseRuleSetClientTest.class);
        }
        b.addStatement("builder.httpClient(getSyncHttpClient())");

        if (params != null) {
            b.add(setClientParams("builder", params));
        }

        b.addStatement("$T request = $L",
                       poetExtension.getModelClass(opModel.getInputShape().getShapeName()),
                       requestCreation(opModel, opParams));

        b.addStatement("builder.build().$L", syncOperationInvocation(opModel));

        b.endControlFlow();

        return b.build();
    }

    private CodeBlock asyncOperationCallLambda(OperationModel opModel, Map<String, TreeNode> params,
                                               Map<String, TreeNode> opParams) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.beginControlFlow("() -> ");
        b.addStatement("$T builder = $T.builder()", asyncClientBuilder(), asyncClientClass());
        b.addStatement("builder.credentialsProvider($T.CREDENTIALS_PROVIDER)", BaseRuleSetClientTest.class);
        if (AuthUtils.usesBearerAuth(model)) {
            b.addStatement("builder.tokenProvider($T.TOKEN_PROVIDER)", BaseRuleSetClientTest.class);
        }
        b.addStatement("builder.httpClient(getAsyncHttpClient())");

        if (params != null) {
            b.add(setClientParams("builder", params));
        }

        b.addStatement("$T request = $L",
                       poetExtension.getModelClass(opModel.getInputShape().getShapeName()),
                       requestCreation(opModel, opParams));

        CodeBlock asyncInvoke = asyncOperationInvocation(opModel);
        b.addStatement("return builder.build().$L", asyncInvoke);

        b.endControlFlow();

        return b.build();
    }

    private CodeBlock syncOperationInvocation(OperationModel opModel) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("$N(", opModel.getMethodName());

        b.add("$N", "request");

        if (opModel.hasStreamingInput()) {
            b.add(", $T.fromString($S)", RequestBody.class, "hello");
        }

        b.add(")");

        return b.build();
    }

    private CodeBlock asyncOperationInvocation(OperationModel opModel) {
        CodeBlock.Builder b = CodeBlock.builder();

        b.add("$N(", opModel.getMethodName());

        b.add("$N", "request");

        if (opModel.hasEventStreamInput()) {
            b.add(", new $T()", EmptyPublisher.class);
            b.add(", $T.mock($T.class)", Mockito.class, poetExtension.eventStreamResponseHandlerType(opModel));
        } else if (opModel.hasStreamingInput()) {
            b.add(", $T.fromString($S)", AsyncRequestBody.class, "hello");
        }

        if (opModel.hasStreamingOutput()) {
            b.add(", $T.get($S)", Paths.class, "test.dat");
        }


        b.add(")");
        return b.build();
    }

    private MethodSpec asyncTestsSourceMethod() {
        String opName = findDefaultRequest();
        OperationModel defaultOpModel = model.getOperation(opName);

        MethodSpec.Builder b = MethodSpec.methodBuilder("asyncTestCases")
                                         .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
                                         .returns(ParameterizedTypeName.get(List.class, AsyncTestCase.class));

        b.addCode("return $T.asList(", Arrays.class);

        EndpointTestSuiteModel endpointTestSuiteModel = model.getEndpointTestSuiteModel();
        Iterator<EndpointTestModel> testIter = endpointTestSuiteModel.getTestCases().iterator();

        while (testIter.hasNext()) {
            EndpointTestModel test = testIter.next();

            if (test.getOperationInputs() != null) {
                Iterator<OperationInput> operationInputsIter = test.getOperationInputs().iterator();
                while (operationInputsIter.hasNext()) {
                    OperationInput opInput = operationInputsIter.next();
                    OperationModel opModel = model.getOperation(opInput.getOperationName());

                    b.addCode("new $T($S, $L, $L$L)",
                              AsyncTestCase.class,
                              test.getDocumentation(),
                              asyncOperationCallLambda(opModel, test.getParams(), opInput.getOperationParams()),
                              TestGeneratorUtils.createExpect(test.getExpect(), opModel, opInput.getOperationParams()),
                              getSkipReasonBlock(test.getDocumentation()));

                    if (operationInputsIter.hasNext()) {
                        b.addCode(",");
                    }
                }
            } else {
                b.addCode("new $T($S, $L, $L$L)",
                          AsyncTestCase.class,
                          test.getDocumentation(),
                          asyncOperationCallLambda(defaultOpModel, test.getParams(), Collections.emptyMap()),
                          TestGeneratorUtils.createExpect(test.getExpect(), defaultOpModel, null),
                          getSkipReasonBlock(test.getDocumentation()));
            }

            if (testIter.hasNext()) {
                b.addCode(",");
            }
        }

        b.addStatement(")");

        return b.build();
    }

    private CodeBlock requestCreation(OperationModel opModel, Map<String, TreeNode> opParams) {
        CodeBlock.Builder b = CodeBlock.builder();

        ShapeModel inputModel = opModel.getInputShape();
        b.add("$T.builder()", poetExtension.getModelClass(inputModel.getShapeName()));

        ShapeModel inputShape = opModel.getInputShape();

        if (opParams != null) {
            opParams.forEach((n, v) -> {
                MemberModel memberModel = opModel.getInputShape().getMemberByName(n);
                CodeBlock memberValue = createMemberValue(memberModel, v);
                b.add(".$N($L)", memberModel.getFluentSetterMethodName(), memberValue);
            });
        }

        if (canBeEmpty(opModel)) {
            return b.add(".build()").build();
        }

        String hostPrefix = getHostPrefixTemplate(opModel).orElse("");

        inputShape.getMembers().forEach(m -> {
            if (!boundToPath(m) && !hostPrefix.contains("{" + m.getC2jName() + "}")) {
                return;
            }

            // if it's in operationInputs, then it's already set
            if (opParams.containsKey(m.getName())) {
                return;
            }

            b.add(".$N(", m.getFluentSetterMethodName());
            switch (m.getVariable().getSimpleType()) {
                case "Boolean":
                    b.add("true");
                    break;
                case "String":
                    b.add("$S", "aws");
                    break;
                case "Long":
                case "Integer":
                    b.add("1");
                    break;
                default:
                    throw new RuntimeException("Don't know how to set member: "
                                               + opModel.getOperationName() + "#" + m.getName()
                                               + " with type " + m.getVariable().getSimpleType());
            }
            b.add(")");
        });


        b.add(".build()");
        return b.build();
    }

    private static boolean canBeEmpty(OperationModel opModel) {
        List<MemberModel> members = opModel.getInputShape().getMembers();

        if (members == null || members.isEmpty()) {
            return true;
        }

        if (opModel.hasStreamingOutput() || opModel.hasStreamingInput()) {
            return false;
        }

        String hostPrefix = getHostPrefixTemplate(opModel).orElse("");

        if (hostPrefix.contains("{") && hostPrefix.contains("}")) {
            return false;
        }

        Optional<MemberModel> pathMemberOrStreaming = members.stream()
                                                             .filter(EndpointRulesClientTestSpec::boundToPath)
                                                             .findFirst();

        return !pathMemberOrStreaming.isPresent();
    }

    private static boolean boundToPath(MemberModel member) {
        ParameterHttpMapping http = member.getHttp();

        if (http == null) {
            return false;
        }

        return http.getLocation() == Location.URI;
    }

    private ClassName syncClientClass() {
        return poetExtension.getClientClass(model.getMetadata().getSyncInterface());
    }

    private ClassName syncClientBuilder() {
        return poetExtension.getClientClass(model.getMetadata().getSyncBuilderInterface());
    }

    private ClassName asyncClientClass() {
        return poetExtension.getClientClass(model.getMetadata().getAsyncInterface());
    }

    private ClassName asyncClientBuilder() {
        return poetExtension.getClientClass(model.getMetadata().getAsyncBuilderInterface());
    }

    private CodeBlock setClientParams(String builderName, Map<String, TreeNode> params) {
        CodeBlock.Builder b = CodeBlock.builder();

        if (hasS3ConfigParams(params)) {
            CodeBlock.Builder config = CodeBlock.builder();

            config.add("$T.builder()", configClass());

            params.forEach((n, v) -> {
                CodeBlock valueLiteral = endpointRulesSpecUtils.treeNodeToLiteral(v);
                switch (n) {
                    case "UseDualStack":
                        config.add(".dualstackEnabled($L)", valueLiteral);
                        break;
                    case "Accelerate":
                        config.add(".accelerateModeEnabled($L)", valueLiteral);
                        break;
                    case "ForcePathStyle":
                        config.add(".pathStyleAccessEnabled($L)", valueLiteral);
                        break;
                    case "UseArnRegion":
                        config.add(".useArnRegionEnabled($L)", valueLiteral);
                        break;
                    case "DisableMultiRegionAccessPoints":
                        config.add(".multiRegionEnabled(!$L)", valueLiteral);
                        break;
                    default:
                        break;
                }
            });

            config.add(".build()");

            b.addStatement("$N.serviceConfiguration($L)", builderName, config.build());
        }

        params.forEach((n, v) -> {
            if (!isClientParam(n)) {
                return;
            }

            ParameterModel paramModel = param(n);
            CodeBlock valueLiteral = endpointRulesSpecUtils.treeNodeToLiteral(v);

            if (paramModel.getBuiltInEnum() != null) {
                switch (paramModel.getBuiltInEnum()) {
                    case AWS_REGION:
                        b.addStatement("$N.region($T.of($L))", builderName, Region.class, valueLiteral);
                        break;
                    case AWS_USE_DUAL_STACK:
                        // If this is S3, it will be set in S3Configuration instead
                        if (!hasS3ConfigParams(params)) {
                            b.addStatement("$N.dualstackEnabled($L)", builderName, valueLiteral);
                        }
                        break;
                    case AWS_USE_FIPS:
                        b.addStatement("$N.fipsEnabled($L)", builderName, valueLiteral);
                        break;
                    case SDK_ENDPOINT:
                        b.addStatement("$N.endpointOverride($T.create($L))", builderName, URI.class, valueLiteral);
                        break;
                    case AWS_S3_USE_GLOBAL_ENDPOINT:
                        b.addStatement("$T.setProperty($L, $L ? \"global\" : \"regional\")", System.class,
                                       s3RegionalEndpointSystemPropertyCode(), valueLiteral);
                        break;
                    default:
                        break;
                }
            } else {
                String setterName = endpointRulesSpecUtils.clientContextParamMethodName(n);
                b.addStatement("$N.$N($L)", builderName, setterName, valueLiteral);
            }

        });
        return b.build();
    }

    private boolean isClientParam(String name) {
        ParameterModel param = param(name);

        if (param == null) {
            return false;
        }

        boolean isBuiltIn = param.getBuiltInEnum() != null;

        Map<String, ClientContextParam> clientContextParams = model.getClientContextParams();
        boolean isClientContextParam = clientContextParams != null && clientContextParams.containsKey(name);

        return isBuiltIn || isClientContextParam;
    }

    private ParameterModel param(String name) {
        return model.getEndpointRuleSetModel().getParameters().get(name);
    }

    private boolean hasSyncClient() {
        return model.getOperations()
                    .values()
                    .stream()
                    .anyMatch(o -> !(o.hasEventStreamOutput() || o.hasEventStreamInput()));
    }

    private boolean hasS3ConfigParams(Map<String, TreeNode> params) {
        String[] s3ConfigurationParams = {
            "ForcePathStyle",
            "Accelerate",
            "UseArnRegion",
            "DisableMultiRegionAccessPoints",
            "UseDualStack"
        };

        if (!endpointRulesSpecUtils.isS3() && !endpointRulesSpecUtils.isS3Control()) {
            return false;
        }

        return Stream.of(s3ConfigurationParams).anyMatch(params.keySet()::contains);
    }

    private ClassName configClass() {
        return poetExtension.getClientClass(model.getCustomizationConfig().getServiceConfig().getClassName());
    }

    private Map<String, String> getSkippedTests() {
        Map<String, String> skippedTests = new HashMap<>(GLOBAL_SKIP_ENDPOINT_TESTS);
        Map<String, String> customSkippedTests = model.getCustomizationConfig().getSkipEndpointTests();
        if (customSkippedTests != null) {
            skippedTests.putAll(customSkippedTests);
        }
        return skippedTests;
    }

    private CodeBlock getSkipReasonBlock(String testName) {
        if (getSkippedTests().containsKey(testName)) {
            Validate.notNull(getSkippedTests().get(testName), "Test %s must have a reason for skipping", testName);
            return CodeBlock.builder().add(", $S", getSkippedTests().get(testName)).build();
        }
        return CodeBlock.builder().build();
    }

    private MethodSpec methodSetupMethod() {
        MethodSpec.Builder b = MethodSpec.methodBuilder("methodSetup")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(BeforeEach.class)
            .returns(void.class);

        b.addStatement("super.methodSetup()");

        // S3 rules assume UseGlobalEndpoint == false by default
        if (endpointRulesSpecUtils.isS3()) {
            b.addStatement("$T.setProperty($L, $S)", System.class, s3RegionalEndpointSystemPropertyCode(), "regional");
        }

        return b.build();
    }

    private CodeBlock s3RegionalEndpointSystemPropertyCode() {
        return CodeBlock.builder()
                        .add("$T.AWS_S3_US_EAST_1_REGIONAL_ENDPOINT.property()", SdkSystemSetting.class)
                        .build();
    }

    private FieldSpec s3RegionEndpointSystemPropertySaveValueField() {
        return FieldSpec.builder(String.class, "regionalEndpointPropertySaveValue")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC)
            .initializer("$T.getProperty($L)", System.class, s3RegionalEndpointSystemPropertyCode())
            .build();
    }

    private MethodSpec teardownMethod() {
        MethodSpec.Builder b = MethodSpec.methodBuilder("teardown")
                                         .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                         .addAnnotation(AfterAll.class)
                                         .returns(void.class);

        if (endpointRulesSpecUtils.isS3()) {
            b.beginControlFlow("if (regionalEndpointPropertySaveValue != null)")
             .addStatement("$T.setProperty($L, regionalEndpointPropertySaveValue)", System.class,
                           s3RegionalEndpointSystemPropertyCode())
             .endControlFlow()
             .beginControlFlow("else")
             .addStatement("$T.clearProperty($L)", System.class, s3RegionalEndpointSystemPropertyCode())
                .endControlFlow();
        }
        return b.build();
    }

    private CodeBlock createMemberValue(MemberModel memberModel, TreeNode valueNode) {
        if (memberModel.isSimple()) {
            return endpointRulesSpecUtils.treeNodeToLiteral(valueNode);
        }

        CodeBlock.Builder b = CodeBlock.builder();

        if (memberModel.isList()) {
            Iterator<JrsValue> elementValuesIter = ((JrsArray) valueNode).elements();

            MemberModel listMemberModel = memberModel.getListModel().getListMemberModel();

            b.add("$T.asList(", Arrays.class);
            while (elementValuesIter.hasNext()) {
                JrsValue v = elementValuesIter.next();
                b.add(createMemberValue(listMemberModel, v));
                if (elementValuesIter.hasNext()) {
                    b.add(",");
                }
            }
            b.add(")");
            return b.build();
        }

        if (memberModel.isMap()) {
            // Not necessary at the moment
            throw new RuntimeException("Don't know how to create map member.");
        }

        return createModelClass(model.getShapes().get(memberModel.getC2jShape()), valueNode);
    }

    private CodeBlock createModelClass(ShapeModel shapeModel, TreeNode valueNode) {
        ClassName modelClassName = poetExtension.getModelClass(shapeModel.getC2jName());

        CodeBlock.Builder b = CodeBlock.builder();
        b.add("$T.builder()", modelClassName);

        JrsObject obj = (JrsObject) valueNode;

        Iterator<String> fieldNamesIter = obj.fieldNames();
        while (fieldNamesIter.hasNext()) {
            String fieldName = fieldNamesIter.next();
            MemberModel member = shapeModel.getMemberByName(fieldName);
            JrsValue value = obj.get(fieldName);

            b.add(".$N($L)", member.getFluentSetterMethodName(), createMemberValue(member, value));
        }

        b.add(".build()");

        return b.build();
    }
}
