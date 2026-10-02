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

package software.amazon.awssdk.services.s3.internal.plugins;

import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.awscore.AwsExecutionAttribute;
import software.amazon.awssdk.core.ClientEndpointProvider;
import software.amazon.awssdk.core.interceptor.ExecutionAttributes;
import software.amazon.awssdk.core.interceptor.SdkExecutionAttribute;
import software.amazon.awssdk.core.interceptor.SdkInternalExecutionAttribute;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.endpoints.S3EndpointParams;
import software.amazon.awssdk.services.s3.endpoints.internal.S3EndpointResolverUtils;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.utils.AttributeMap;

class ListOfStringsAuthParamPluginTest {

    @Test
    void callingDeleteObjects_requestWithCompleteListOfKey_returnsRightValues() {
        DeleteObjectsRequest request = DeleteObjectsRequest.builder().bucket("test").delete(d -> d.objects(
            ObjectIdentifier.builder().key("x").versionId("1").build(),
            ObjectIdentifier.builder().key("y").versionId("2").build()
        )).build();
        S3EndpointParams params = S3EndpointResolverUtils.ruleParams(request, deleteObjectsAttributes());
        assertThat(params.deleteObjectKeys()).isEqualTo(asList("x", "y"));
    }

    @Test
    void callingDeleteObjects_requestWithInCompleteListOfKey_returnsRightValues() {
        DeleteObjectsRequest request = DeleteObjectsRequest.builder().bucket("test").delete(d -> d.objects(
            ObjectIdentifier.builder().versionId("1").build(),
            ObjectIdentifier.builder().key("y").versionId("2").build()
        )).build();
        S3EndpointParams params = S3EndpointResolverUtils.ruleParams(request, deleteObjectsAttributes());
        assertThat(params.deleteObjectKeys()).isEqualTo(asList("y"));
    }

    @Test
    void callingDeleteObjects_requestWithNoList_returnsRightValues() {
        DeleteObjectsRequest request = DeleteObjectsRequest.builder().delete(Delete.builder().build()).build();
        S3EndpointParams params = S3EndpointResolverUtils.ruleParams(request, deleteObjectsAttributes());
        assertThat(params.deleteObjectKeys()).asList().isEmpty();
    }

    @Test
    void callingDeleteObjects_requestWithoutEmptyList_returnsRightValues() {
        DeleteObjectsRequest request = DeleteObjectsRequest.builder().delete(Delete.builder().objects(Collections.emptyList()).build()).build();
        S3EndpointParams params = S3EndpointResolverUtils.ruleParams(request, deleteObjectsAttributes());
        assertThat(params.deleteObjectKeys()).asList().isEmpty();
    }

    @Test
    void callingDeleteObjects_requestWithListButNoKey_returnsRightValues() {
        List<ObjectIdentifier> objects = asList(ObjectIdentifier.builder().build());
        DeleteObjectsRequest request = DeleteObjectsRequest.builder().delete(Delete.builder().objects(objects).build()).build();
        S3EndpointParams params = S3EndpointResolverUtils.ruleParams(request, deleteObjectsAttributes());
        assertThat(params.deleteObjectKeys()).asList().isEmpty();
    }

    private static ExecutionAttributes deleteObjectsAttributes() {
        ExecutionAttributes attrs = new ExecutionAttributes();
        attrs.putAttribute(SdkExecutionAttribute.OPERATION_NAME, "DeleteObjects");
        attrs.putAttribute(AwsExecutionAttribute.AWS_REGION, Region.US_EAST_1);
        attrs.putAttribute(SdkInternalExecutionAttribute.CLIENT_ENDPOINT_PROVIDER,
                           ClientEndpointProvider.forEndpointOverride(URI.create("https://s3.us-east-1.amazonaws.com")));
        attrs.putAttribute(SdkInternalExecutionAttribute.CLIENT_CONTEXT_PARAMS, AttributeMap.empty());
        return attrs;
    }
}
