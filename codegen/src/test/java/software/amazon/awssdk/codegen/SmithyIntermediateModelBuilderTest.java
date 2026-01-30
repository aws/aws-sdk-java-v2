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

package software.amazon.awssdk.codegen;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.ServiceModel;
import software.amazon.awssdk.codegen.smithy.SmithyIntermediateModelBuilder;
import software.amazon.awssdk.codegen.smithy.SmithyModelWithCustomizations;
import software.amazon.awssdk.codegen.utils.ModelLoaderUtils;

public class SmithyIntermediateModelBuilderTest {

    @Test
    public void testTranslate() {

        final File c2jModelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/smithy/basic/service-2.json").getFile());
        IntermediateModel c2jIm = new IntermediateModelBuilder(
            C2jModels.builder()
                     .serviceModel(ModelLoaderUtils.loadModel(ServiceModel.class, c2jModelFile))
                     .customizationConfig(CustomizationConfig.create())
                     .build())
            .build();

        final File smithyModelFile = new File(IntermediateModelBuilderTest.class
                                            .getResource("poet/client/smithy/basic/model.json").getFile());
        SmithyModelWithCustomizations smithyModel = SmithyModelWithCustomizations.builder()
                                                                                 .smithyModel(smithyModelFile.toPath())
                                                                                 .build();
        IntermediateModel smithyIm = new SmithyIntermediateModelBuilder(smithyModel).build();
        
        // Assert that both models have the same operations
        assertOperationsMatch(c2jIm, smithyIm);
    }
    
    /**
     * Asserts that operations in both models match in terms of names and key properties.
     */
    private void assertOperationsMatch(IntermediateModel expected, IntermediateModel actual) {
        assertThat(actual.getOperations().keySet())
            .as("Operation names should match")
            .containsExactlyInAnyOrderElementsOf(expected.getOperations().keySet());
        
        // For each operation, compare the details
        for (String operationName : expected.getOperations().keySet()) {
            OperationModel expectedOp = expected.getOperation(operationName);
            OperationModel actualOp = actual.getOperation(operationName);
            
            assertThat(actualOp)
                .as("Operation %s should exist in both models", operationName)
                .isNotNull();
            
            assertOperationDetailsMatch(operationName, expectedOp, actualOp);
        }
    }
    
    /**
     * Asserts that two operation models match in their key properties.
     */
    private void assertOperationDetailsMatch(String operationName, OperationModel expected, OperationModel actual) {
        assertThat(actual.getOperationName())
            .as("Operation name for %s", operationName)
            .isEqualTo(expected.getOperationName());
        
        assertThat(actual.getDocumentation())
            .as("Documentation for operation %s", operationName)
            .isEqualTo(expected.getDocumentation());
        
        assertThat(actual.isDeprecated())
            .as("Deprecated flag for operation %s", operationName)
            .isEqualTo(expected.isDeprecated());
        
        if (expected.isDeprecated()) {
            assertThat(actual.getDeprecatedMessage())
                .as("Deprecated message for operation %s", operationName)
                .isEqualTo(expected.getDeprecatedMessage());
        }
        
        assertThat(actual.isPaginated())
            .as("Paginated flag for operation %s", operationName)
            .isEqualTo(expected.isPaginated());
        
        assertThat(actual.isEndpointOperation())
            .as("Endpoint operation flag for operation %s", operationName)
            .isEqualTo(expected.isEndpointOperation());
        
        assertThat(actual.isHttpChecksumRequired())
            .as("HTTP checksum required flag for operation %s", operationName)
            .isEqualTo(expected.isHttpChecksumRequired());
        
        assertThat(actual.isUnsignedPayload())
            .as("Unsigned payload flag for operation %s", operationName)
            .isEqualTo(expected.isUnsignedPayload());
        
        // Compare input
        if (expected.getInput() != null) {
            assertThat(actual.getInput())
                .as("Input for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getInput().getVariableType())
                .as("Input type for operation %s", operationName)
                .isEqualTo(expected.getInput().getVariableType());
        } else {
            assertThat(actual.getInput())
                .as("Input for operation %s should be null", operationName)
                .isNull();
        }
        
        // Compare output/return type
        if (expected.getReturnType() != null) {
            assertThat(actual.getReturnType())
                .as("Return type for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getReturnType().getReturnType())
                .as("Return type name for operation %s", operationName)
                .isEqualTo(expected.getReturnType().getReturnType());
        } else {
            assertThat(actual.getReturnType())
                .as("Return type for operation %s should be null", operationName)
                .isNull();
        }
        
        // Compare exceptions
        assertThat(actual.getExceptions())
            .as("Number of exceptions for operation %s", operationName)
            .hasSameSizeAs(expected.getExceptions());
        
        // Compare exception names as a set since order may differ between C2J and Smithy
        Set<String> expectedExceptionNames = expected.getExceptions().stream()
            .map(e -> e.getExceptionName())
            .collect(Collectors.toSet());
        Set<String> actualExceptionNames = actual.getExceptions().stream()
            .map(e -> e.getExceptionName())
            .collect(Collectors.toSet());
        
        assertThat(actualExceptionNames)
            .as("Exception names for operation %s", operationName)
            .isEqualTo(expectedExceptionNames);
        
        // Compare endpoint discovery
        if (expected.getEndpointDiscovery() != null) {
            assertThat(actual.getEndpointDiscovery())
                .as("Endpoint discovery for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getEndpointDiscovery().isRequired())
                .as("Endpoint discovery required flag for operation %s", operationName)
                .isEqualTo(expected.getEndpointDiscovery().isRequired());
        } else {
            assertThat(actual.getEndpointDiscovery())
                .as("Endpoint discovery for operation %s should be null", operationName)
                .isNull();
        }
        
        // Compare endpoint trait
        if (expected.getEndpointTrait() != null) {
            assertThat(actual.getEndpointTrait())
                .as("Endpoint trait for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getEndpointTrait().getHostPrefix())
                .as("Endpoint trait host prefix for operation %s", operationName)
                .isEqualTo(expected.getEndpointTrait().getHostPrefix());
        } else {
            assertThat(actual.getEndpointTrait())
                .as("Endpoint trait for operation %s should be null", operationName)
                .isNull();
        }
        
        // Compare HTTP checksum
        if (expected.getHttpChecksum() != null) {
            assertThat(actual.getHttpChecksum())
                .as("HTTP checksum for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getHttpChecksum().isRequestChecksumRequired())
                .as("HTTP checksum request required for operation %s", operationName)
                .isEqualTo(expected.getHttpChecksum().isRequestChecksumRequired());
        } else {
            assertThat(actual.getHttpChecksum())
                .as("HTTP checksum for operation %s should be null", operationName)
                .isNull();
        }
        
        // Compare request compression
        if (expected.getRequestcompression() != null) {
            assertThat(actual.getRequestcompression())
                .as("Request compression for operation %s should not be null", operationName)
                .isNotNull();
            assertThat(actual.getRequestcompression().getEncodings())
                .as("Request compression encodings for operation %s", operationName)
                .isEqualTo(expected.getRequestcompression().getEncodings());
        } else {
            assertThat(actual.getRequestcompression())
                .as("Request compression for operation %s should be null", operationName)
                .isNull();
        }
    }
}
