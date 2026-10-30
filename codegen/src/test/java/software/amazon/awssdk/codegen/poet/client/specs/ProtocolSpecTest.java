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

package software.amazon.awssdk.codegen.poet.client.specs;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.model.intermediate.Protocol.EC2;
import static software.amazon.awssdk.codegen.model.intermediate.Protocol.QUERY;
import static software.amazon.awssdk.codegen.model.intermediate.Protocol.REST_JSON;
import static software.amazon.awssdk.codegen.model.intermediate.Protocol.REST_XML;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.intermediate.Protocol;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;

public class ProtocolSpecTest {

    @ParameterizedTest(name = "protocol = {0}, isFault = {1}, code = \"{2}\"")
    @MethodSource("populateHttpStatusCodeTestCases")
    void populateHttpStatusCode_behavesCorrectlyForProtocol(Protocol protocol, boolean isFault, String result) {
        IntermediateModel intermediateModel = new IntermediateModel();
        Metadata md = new Metadata();
        md.setProtocol(protocol);
        intermediateModel.setMetadata(md);

        ShapeModel model = new ShapeModel();
        model.withIsFault(isFault);

        TestProtocolSpec spec = new TestProtocolSpec();
        assertThat(spec.populateHttpStatusCode(model, intermediateModel)).isEqualTo(result);
    }

    private static Stream<Arguments> populateHttpStatusCodeTestCases() {
        boolean[] isFault = {true, false};
        Set<Protocol> noopProtocols = EnumSet.of(EC2, QUERY, REST_XML, REST_JSON);

        List<Arguments> testCases = new ArrayList<>();

        for (Protocol p : Protocol.values()) {
            for (boolean fault : isFault) {
                if (noopProtocols.contains(p)) {
                    testCases.add(Arguments.of(p, fault, ""));
                } else {
                    int statusCode = fault ? 500 : 400;
                    testCases.add(Arguments.of(p, fault, String.format(".httpStatusCode(%d)", statusCode)));
                }
            }
        }

        testCases.add(Arguments.of(null, true, ""));
        testCases.add(Arguments.of(null, false, ""));

        return testCases.stream();
    }

    private static class TestProtocolSpec implements ProtocolSpec {

        @Override
        public FieldSpec protocolFactory(IntermediateModel model) {
            return null;
        }

        @Override
        public MethodSpec initProtocolFactory(IntermediateModel model) {
            return null;
        }

        @Override
        public CodeBlock responseHandler(IntermediateModel model, OperationModel opModel) {
            return null;
        }

        @Override
        public Optional<CodeBlock> errorResponseHandler(OperationModel opModel) {
            return Optional.empty();
        }

        @Override
        public CodeBlock executionHandler(OperationModel opModel) {
            return null;
        }

        @Override
        public Optional<MethodSpec> createErrorResponseHandler() {
            return Optional.empty();
        }
    }
}
