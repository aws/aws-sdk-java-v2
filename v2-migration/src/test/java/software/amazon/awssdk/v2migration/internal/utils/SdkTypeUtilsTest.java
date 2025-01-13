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

package software.amazon.awssdk.v2migration.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClientBuilder;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.SqsClientBuilder;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.ListMessageMoveTasksRequest;

public class SdkTypeUtilsTest {

    public static Stream<Arguments> isEligibleToConvertToBuilderTestCase() {
        return Stream.of(
            Arguments.of(SqsClient.class.getCanonicalName(), true),
            Arguments.of(ListMessageMoveTasksRequest.class.getCanonicalName(), true),
            Arguments.of(AmazonSQS.class.getCanonicalName(), false),
            Arguments.of(EnvironmentVariableCredentialsProvider.class.getCanonicalName(), false)
        );
    }

    public static Stream<Arguments> v2BuilderTestCase() {
        return Stream.of(
            Arguments.of(software.amazon.awssdk.services.sqs.model.SendMessageRequest.class.getCanonicalName(),
                         software.amazon.awssdk.services.sqs.model.SendMessageRequest.Builder.class.getCanonicalName()),
            Arguments.of(SqsClient.class.getCanonicalName(),
                         SqsClientBuilder.class.getCanonicalName())
        );
    }

    public static Stream<Arguments> isV2ClientFromClientBuilderClassTestCase() {
        return Stream.of(
            Arguments.of(SqsAsyncClient.class.getCanonicalName(), true),
            Arguments.of(CreateQueueRequest.class.getCanonicalName(), false),
            Arguments.of(SqsClient.class.getCanonicalName(), true)
        );
    }

    public static Stream<Arguments> isV2ClientFromClientBuilderBuilderTestCase() {
        return Stream.of(
            Arguments.of(SqsAsyncClient.class.getCanonicalName(), false),
            Arguments.of(CreateQueueRequest.class.getCanonicalName(), false),
            Arguments.of(SqsAsyncClientBuilder.class.getCanonicalName(), true)
        );
    }

    public static Stream<Arguments> isV2AsyncClientClassTestCase() {
        return Stream.of(
            Arguments.of(SqsAsyncClient.class.getCanonicalName(), true),
            Arguments.of(SqsClient.class.getCanonicalName(), false),
            Arguments.of(SqsAsyncClientBuilder.class.getCanonicalName(), false)
        );
    }

    public static Stream<Arguments> v2ClientFromClientBuilderTestCase() {
        return Stream.of(
            Arguments.of(SqsAsyncClientBuilder.class.getCanonicalName(),
                         SqsAsyncClient.class.getCanonicalName()),
            Arguments.of(SqsClientBuilder.class.getCanonicalName(),
                         SqsClient.class.getCanonicalName())
        );
    }

    @Test
    public void isV1ModelClass_v1Request_returnsTrue() {
        JavaType sendMessage = JavaType.buildType(SendMessageRequest.class.getCanonicalName());
        assertThat(SdkTypeUtils.isV1ModelClass(sendMessage)).isTrue();
    }

    @Test
    public void isV1ModelClass_v2Request_returnsFalse() {
        JavaType sendMessage =
            JavaType.buildType(software.amazon.awssdk.services.sqs.model.SendMessageRequest.class.getCanonicalName());
        assertThat(SdkTypeUtils.isV1ModelClass(sendMessage)).isFalse();
    }

    @Test
    public void isV1ModelClass_v1ServiceClass_returnsFalse() {
        JavaType sqs = JavaType.buildType(AmazonSQS.class.getCanonicalName());
        assertThat(SdkTypeUtils.isV1ModelClass(sqs)).isFalse();
    }

    public static Stream<Arguments> isSupportedV1ClassParams() {
        return Stream.of(Arguments.of("v1ModelClass_shouldReturnTrue", SendMessageRequest.class.getCanonicalName(), true),
                         Arguments.of("v1ClientClass_shouldReturnTrue", AmazonSQS.class.getCanonicalName(), true),
                         Arguments.of("tmClass_shouldReturnFalse", TransferManager.class.getCanonicalName(), false),
                         Arguments.of("ddbMapper_shouldReturnFalse", DynamoDBMapper.class.getCanonicalName(), false),
                         Arguments.of("customSdk_shouldReturnFalse", "com.amazonaws.services.foobar.model.FooBarRequest", false),
                         Arguments.of("customSdk_shouldReturnFalse", "com.amazonaws.services.foobar.FooBarClient", false)
                         );
    }


    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("isSupportedV1ClassParams")
    public void isSupportedV1Class(String description, String fqcn, boolean expected) {
        JavaType.FullyQualified type =
            TypeUtils.asFullyQualified(JavaType.buildType(fqcn));
        assertThat(SdkTypeUtils.isSupportedV1Class(type)).isEqualTo(expected);
    }

    @Test
    public void isV2ModelClass_v1ModelClass_returnsFalse() {
        JavaType sendMessage = JavaType.buildType(SendMessageRequest.class.getCanonicalName());
        assertThat(SdkTypeUtils.isV2ModelClass(sendMessage)).isFalse();
    }

    @Test
    public void isV2ModelClass_v2Request_returnsTrue() {
        JavaType sendMessage =
            JavaType.buildType(software.amazon.awssdk.services.sqs.model.SendMessageRequest.class.getCanonicalName());
        assertThat(SdkTypeUtils.isV2ModelClass(sendMessage)).isTrue();
    }

    @Test
    public void isV2ModelBuilder_v1Request_returnsFalse() {
        JavaType sendMessage = JavaType.buildType(SendMessageRequest.class.getCanonicalName());
        assertThat(SdkTypeUtils.isV2ModelBuilder(sendMessage)).isFalse();
    }

    @Test
    public void isV2ModelBuilder_v2Request_returnsFalse() {
        JavaType sendMessage =
            JavaType.buildType(software.amazon.awssdk.services.sqs.model.SendMessageRequest.class.getCanonicalName());
        assertThat(SdkTypeUtils.isV2ModelBuilder(sendMessage)).isFalse();
    }

    @Test
    public void v2ModelBuilder_typeIsNotV2_throws() {
        JavaType.FullyQualified v1Type =
            TypeUtils.asFullyQualified(JavaType.buildType(SendMessageRequest.class.getCanonicalName()));

        assertThatThrownBy(() -> SdkTypeUtils.v2Builder(v1Type))
            .isInstanceOf(IllegalArgumentException.class);

    }

    @ParameterizedTest
    @MethodSource("v2BuilderTestCase")
    public void v2Builder_convertsClassCorrectly(String fqcn, String expectedFqcn) {
        JavaType.FullyQualified sendMessageRequest =
            TypeUtils.asFullyQualified(JavaType.buildType(fqcn));

        assertThat(SdkTypeUtils.v2Builder(sendMessageRequest).getFullyQualifiedName())
            .isEqualTo(expectedFqcn);
    }

    @ParameterizedTest
    @MethodSource("v2ClientFromClientBuilderTestCase")
    public void v2ClientFromClientBuilder_convertsClassCorrectly(String fqcn, String expectedFqcn) {
        JavaType.FullyQualified clientBuilder =
            TypeUtils.asFullyQualified(JavaType.buildType(fqcn));

        assertThat(SdkTypeUtils.v2ClientFromClientBuilder(clientBuilder).getFullyQualifiedName())
            .isEqualTo(expectedFqcn);
    }

    @ParameterizedTest
    @MethodSource("isV2ClientFromClientBuilderBuilderTestCase")
    public void isV2ClientBuilderClass_v2ClientFromClientBuilderBuilderClass_returnsTrue() {
        JavaType sqs = JavaType.buildType(SqsClientBuilder.class.getCanonicalName());
        assertThat(SdkTypeUtils.isV2ClientBuilder(sqs)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("isEligibleToConvertToBuilderTestCase")
    public void isEligibleToConvertToBuilder_shouldConvert(String fqcn, boolean expected) {
        JavaType.FullyQualified type =
            TypeUtils.asFullyQualified(JavaType.buildType(fqcn));
        assertThat(SdkTypeUtils.isEligibleToConvertToBuilder(type)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("isV2ClientFromClientBuilderClassTestCase")
    public void isV2ClientFromClientBuilderClass_shouldReturnCorrectly(String fqcn, boolean expected) {
        JavaType sqs = JavaType.buildType(fqcn);
        assertThat(SdkTypeUtils.isV2ClientClass(sqs)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("isV2AsyncClientClassTestCase")
    public void isV2AsyncClientClass_shouldReturnCorrectly(String fqcn, boolean expected) {
        JavaType sqs = JavaType.buildType(fqcn);
        assertThat(SdkTypeUtils.isV2AsyncClientClass(sqs)).isEqualTo(expected);
    }
}
