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

package software.amazon.awssdk.codegen.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.Metadata;
import software.amazon.awssdk.codegen.model.intermediate.OperationModel;
import software.amazon.awssdk.codegen.model.service.AuthType;

public class AuthUtilsTest {

    @ParameterizedTest
    @MethodSource("serviceValues")
    public void testIfServiceHasBearerAuth(AuthType serviceAuthType,
                                           List<AuthType> opAuthTypes,
                                           Boolean expectedResult) {
        IntermediateModel model = modelWith(serviceAuthType);
        model.setOperations(createOperations(opAuthTypes));
        assertThat(AuthUtils.usesBearerAuth(model)).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> serviceValues() {
        List<AuthType> oneBearerOp = Arrays.asList(AuthType.BEARER, AuthType.S3V4, AuthType.NONE);
        List<AuthType> noBearerOp = Arrays.asList(AuthType.S3V4, AuthType.S3V4, AuthType.NONE);

        return Stream.of(Arguments.of(AuthType.BEARER, noBearerOp, true),
                         Arguments.of(AuthType.BEARER, oneBearerOp, true),
                         Arguments.of(AuthType.S3V4, noBearerOp, false),
                         Arguments.of(AuthType.S3V4, oneBearerOp, true));
    }

    @ParameterizedTest
    @MethodSource("awsAuthServiceValues")
    public void testIfServiceHasAwsAuthAuth(AuthType serviceAuthType,
                                           List<AuthType> opAuthTypes,
                                           Boolean expectedResult) {
        IntermediateModel model = modelWith(serviceAuthType);
        model.setOperations(createOperations(opAuthTypes));
        assertThat(AuthUtils.usesAwsAuth(model)).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> awsAuthServiceValues() {
        List<AuthType> oneAwsAuthOp = Arrays.asList(AuthType.V4, AuthType.BEARER, AuthType.NONE);
        List<AuthType> noAwsAuthOp = Arrays.asList(AuthType.BEARER, AuthType.NONE);

        return Stream.of(Arguments.of(AuthType.BEARER, oneAwsAuthOp, true),
                         Arguments.of(AuthType.BEARER, noAwsAuthOp, false),
                         Arguments.of(AuthType.V4, oneAwsAuthOp, true),
                         Arguments.of(AuthType.V4, noAwsAuthOp, true));
    }

    @ParameterizedTest
    @MethodSource("opValues")
    public void testIfOperationIsBearerAuth(AuthType serviceAuthType, AuthType opAuthType, Boolean expectedResult) {
        IntermediateModel model = modelWith(serviceAuthType);
        OperationModel opModel = opModelWith(opAuthType);
        assertThat(AuthUtils.isOpBearerAuth(model, opModel)).isEqualTo(expectedResult);
    }

    private static Stream<Arguments> opValues() {
        return Stream.of(Arguments.of(AuthType.BEARER, AuthType.BEARER, true),
                         Arguments.of(AuthType.BEARER, AuthType.S3V4, false),
                         Arguments.of(AuthType.BEARER, AuthType.NONE, true),
                         Arguments.of(AuthType.BEARER, null, true),
                         Arguments.of(AuthType.S3V4, AuthType.BEARER, true),
                         Arguments.of(AuthType.S3V4, AuthType.S3V4, false),
                         Arguments.of(AuthType.S3V4, AuthType.NONE, false),
                         Arguments.of(AuthType.S3V4, null, false));
    }

    private static OperationModel opModelWith(AuthType authType) {
        OperationModel opModel = new OperationModel();
        opModel.setAuthType(authType);
        return opModel;
    }

    private static IntermediateModel modelWith(AuthType authType) {
        IntermediateModel model = new IntermediateModel();
        Metadata metadata = new Metadata();
        metadata.setAuthType(authType);
        model.setMetadata(metadata);
        return model;
    }

    private static Map<String, OperationModel> createOperations(List<AuthType> opAuthTypes) {
        return IntStream.range(0, opAuthTypes.size())
                        .boxed()
                        .collect(Collectors.toMap(i -> "Op" + i, i -> opModelWith(opAuthTypes.get(i))));
    }
}
