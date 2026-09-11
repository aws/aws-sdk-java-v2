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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.jr.stree.JrsNumber;
import com.fasterxml.jackson.jr.stree.JrsString;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.rules.endpoints.ParameterModel;
import software.amazon.awssdk.codegen.model.service.OperationContextParam;
import software.amazon.awssdk.codegen.poet.ClientTestModels;
import software.amazon.awssdk.codegen.poet.PoetUtils;

public class EndpointResolverUtilsSpecTest {

    @Test
    void endpointResolverUtilsClass() {
        ClassSpec spec = new EndpointResolverUtilsSpec(ClientTestModels.queryServiceModels());
        assertThat(spec, generatesTo("endpoint-resolver-utils.java"));
    }

    @Test
    void endpointResolverUtilsClassWithSigv4aMultiAuth() {
        ClassSpec spec = new EndpointResolverUtilsSpec(ClientTestModels.opsWithSigv4a());
        assertThat(spec, generatesTo("endpoint-resolver-utils-with-multiauthsigv4a.java"));
    }

    @Test
    void endpointResolverUtilsClassWithEndpointBasedAuth() {
        ClassSpec spec = new EndpointResolverUtilsSpec(
            ClientTestModels.queryServiceModelsEndpointAuthParamsWithoutAllowList());
        assertThat(spec, generatesTo("endpoint-resolver-utils-with-endpointsbasedauth.java"));
    }

    @Test
    void endpointResolverUtilsClassWithStringArray() {
        ClassSpec spec = new EndpointResolverUtilsSpec(ClientTestModels.stringArrayServiceModels());
        assertThat(spec, generatesTo("endpoint-resolver-utils-with-stringarray.java"));
    }

    @Test
    void multipleLoweredBindingsUseUniqueNames() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        addStringArrayBinding(model, "ListOfObjectsOperation", "stringArrayParam2",
                              "nested.listOfObjects[*].key");

        String generated = PoetUtils.buildJavaFile(new EndpointResolverUtilsSpec(model)).toString();
        String method = operationBindingMethod(generated, "ListOfObjectsOperationRequest request");

        assertTrue(method.contains("Nested nested = request.nested()"));
        assertTrue(method.contains("Nested nested_ = request.nested()"));
    }

    @Test
    void unsupportedBindingFallsBackForWholeOperation() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        addStringArrayBinding(model, "ListOfObjectsOperation", "stringArrayParam2", "nested.listOfObjects");

        String generated = PoetUtils.buildJavaFile(new EndpointResolverUtilsSpec(model)).toString();
        String method = operationBindingMethod(generated, "ListOfObjectsOperationRequest request");

        assertTrue(method.contains("JmesPathRuntime.Value input = new JmesPathRuntime.Value(request)"));
        assertTrue(method.contains("params.stringArrayParam(input.field(\"nested\")"));
        assertTrue(method.contains("params.stringArrayParam2(input.field(\"nested\")"));
        assertFalse(method.contains("List<String> stringArrayParam = new ArrayList<>()"));
    }

    @Test
    void malformedPathOrderedAfterUnsupportedBindingStillReportsTheOperation() {
        IntermediateModel model = ClientTestModels.stringArrayServiceModels();
        addStringArrayBinding(model, "ListOfObjectsOperation", "stringArrayParam2", "nested.listOfObjects");
        addBinding(model, "ListOfObjectsOperation", "stringArrayParam3", new JrsNumber(1));

        RuntimeException e = assertThrows(RuntimeException.class,
                                         () -> PoetUtils.buildJavaFile(new EndpointResolverUtilsSpec(model)));

        assertTrue(e.getMessage() != null && e.getMessage().contains("ListOfObjectsOperation"),
                   "expected the descriptive model error naming the operation, but got: " + e);
        assertTrue(e.getMessage().contains("VALUE_NUMBER_INT"));
    }

    private static void addStringArrayBinding(IntermediateModel model, String operationName, String parameterName,
                                              String path) {
        addBinding(model, operationName, parameterName, new JrsString(path));
    }

    private static void addBinding(IntermediateModel model, String operationName, String parameterName,
                                   TreeNode path) {
        ParameterModel parameter = model.getEndpointRuleSetModel().getParameters().get("stringArrayParam");
        model.getEndpointRuleSetModel().getParameters().put(parameterName, parameter);

        OperationContextParam operationContextParam = new OperationContextParam();
        operationContextParam.setPath(path);
        OperationModel operation = model.getOperation(operationName);
        Map<String, OperationContextParam> params = new LinkedHashMap<>(operation.getOperationContextParams());
        params.put(parameterName, operationContextParam);
        operation.setOperationContextParams(params);
    }

    private static String operationBindingMethod(String generated, String requestParameter) {
        int request = generated.indexOf(requestParameter);
        int start = generated.lastIndexOf("private static void setOperationContextParams", request);
        int end = generated.indexOf("private static void", request + requestParameter.length());
        return generated.substring(start, end);
    }

}
