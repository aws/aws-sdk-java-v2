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

package software.amazon.awssdk.migration.internal.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class SdkTypeUtilsTest {

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

    @Test
    public void isV1Class_v1Request_returnsFalse() {
        JavaType sendMessage = JavaType.buildType(SendMessageRequest.class.getCanonicalName());
        assertThat(SdkTypeUtils.isV1Class(sendMessage)).isFalse();
    }

    @Test
    public void isV1Class_v1ServiceClass_returnsTrue() {
        JavaType sqs = JavaType.buildType(AmazonSQS.class.getCanonicalName());
        assertThat(SdkTypeUtils.isV1Class(sqs)).isTrue();
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
    public void asV2Type_convertsClassCorrectly() {
        JavaType.FullyQualified v1Type = TypeUtils.asFullyQualified(JavaType.buildType(SendMessageRequest.class.getCanonicalName()));
        assertThat(SdkTypeUtils.asV2Type(v1Type).getFullyQualifiedName()).isEqualTo(software.amazon.awssdk.services.sqs.model.SendMessageRequest.class.getCanonicalName());
    }

    @Test
    public void asV2Type_typeIsNotV1_throws() {
        JavaType.FullyQualified v2Type =
            TypeUtils.asFullyQualified(JavaType.buildType(software.amazon.awssdk.services.sqs.model.SendMessageRequest.class.getCanonicalName()));

        assertThatThrownBy(() -> SdkTypeUtils.asV2Type(v2Type))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void v2ModelBuilder_typeIsNotV2_throws() {
        JavaType.FullyQualified v1Type =
            TypeUtils.asFullyQualified(JavaType.buildType(SendMessageRequest.class.getCanonicalName()));

        assertThatThrownBy(() -> SdkTypeUtils.v2ModelBuilder(v1Type))
            .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    public void v2ModelBuilder_typeIsV2_convertsClassCorrectly() {
        JavaType.FullyQualified sendMessageRequest =
            TypeUtils.asFullyQualified(JavaType.buildType(software.amazon.awssdk.services.sqs.model.SendMessageRequest.class.getCanonicalName()));

        assertThat(SdkTypeUtils.v2ModelBuilder(sendMessageRequest).getFullyQualifiedName())
            .isEqualTo(software.amazon.awssdk.services.sqs.model.SendMessageRequest.Builder.class.getCanonicalName());
    }
}
