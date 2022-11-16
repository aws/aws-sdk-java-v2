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

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.stree.JacksonJrsTreeCodec;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.codegen.model.service.Location;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

class AddShapesTest {

    @Test
    void generateShapeModel_memberRequiredByShape_setsMemberModelAsRequired() {
        String requestShapeName = "AbortMultipartUploadRequest";
        String queryParamName = "UploadId";

        ServiceModel serviceModel = serviceModel(true);
        C2jModels c2jModels = C2jModels.builder()
                                       .serviceModel(serviceModel)
                                       .customizationConfig(CustomizationConfig.create())
                                       .build();
        IntermediateModel testModel = new IntermediateModelBuilder(c2jModels).build();

        ShapeModel requestShapeModel = testModel.getShapes().get(requestShapeName);
        MemberModel uploadIdMemberModel = requestShapeModel.findMemberModelByC2jName(queryParamName);

        assertThat(requestShapeModel.getRequired()).contains(queryParamName);
        assertThat(uploadIdMemberModel.getHttp().getLocation()).isEqualTo(Location.QUERY_STRING);
        assertThat(uploadIdMemberModel.isRequired()).isTrue();
    }

    @Test
    void generateShapeModel_memberNotRequiredByShape_doesNotSetMemberModelAsRequired() {
        String requestShapeName = "AbortMultipartUploadRequest";
        String queryParamName = "UploadId";

        ServiceModel serviceModel = serviceModel(false);
        C2jModels c2jModels = C2jModels.builder()
                                       .serviceModel(serviceModel)
                                       .customizationConfig(CustomizationConfig.create())
                                       .build();
        IntermediateModel testModel = new IntermediateModelBuilder(c2jModels).build();

        ShapeModel requestShapeModel = testModel.getShapes().get(requestShapeName);
        MemberModel uploadIdMemberModel = requestShapeModel.findMemberModelByC2jName(queryParamName);

        assertThat(requestShapeModel.getRequired()).doesNotContain(queryParamName);
        assertThat(uploadIdMemberModel.getHttp().getLocation()).isEqualTo(Location.QUERY_STRING);
        assertThat(uploadIdMemberModel.isRequired()).isFalse();
    }

    private static ServiceModel serviceModel(boolean requireUploadId) {
        JSON mapper = jsonMapper();
        try {
            return mapper.beanFrom(ServiceModel.class, serviceModelDefinition(requireUploadId));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static JSON jsonMapper() {
        return JSON.builder()
                   .disable(JSON.Feature.FAIL_ON_UNKNOWN_BEAN_PROPERTY)
                   .enable(JSON.Feature.READ_JSON_ARRAYS_AS_JAVA_ARRAYS)
                   .treeCodec(new JacksonJrsTreeCodec())
                   .build();
    }

    private static String serviceModelDefinition(boolean requireUploadId) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n")
          .append("  \"version\":\"2.0\",\n")
          .append("  \"metadata\":{\n")
          .append("    \"apiVersion\":\"2006-03-01\",\n")
          .append("    \"checksumFormat\":\"md5\",\n")
          .append("    \"endpointPrefix\":\"s3\",\n")
          .append("    \"globalEndpoint\":\"s3.amazonaws.com\",\n")
          .append("    \"protocol\":\"rest-xml\",\n")
          .append("    \"serviceAbbreviation\":\"Amazon S3\",\n")
          .append("    \"serviceFullName\":\"Amazon Simple Storage Service\",\n")
          .append("    \"serviceId\":\"S3\",\n")
          .append("    \"signatureVersion\":\"s3\",\n")
          .append("    \"uid\":\"s3-2006-03-01\"\n")
          .append("  },\n")
          .append("  \"operations\":{\n")
          .append("    \"AbortMultipartUpload\":{\n")
          .append("      \"name\":\"AbortMultipartUpload\",\n")
          .append("      \"http\":{\n")
          .append("        \"method\":\"DELETE\",\n")
          .append("        \"requestUri\":\"/{Bucket}/{Key+}\",\n")
          .append("        \"responseCode\":204\n")
          .append("      },\n")
          .append("      \"input\":{\"shape\":\"AbortMultipartUploadRequest\"},\n")
          .append("      \"errors\":[\n")
          .append("        {\"shape\":\"NoSuchUpload\"}\n")
          .append("      ],\n")
          .append("      \"documentationUrl\":\"none\",\n")
          .append("      \"documentation\":\"<p>Test</p>\"\n")
          .append("    }\n")
          .append("  },\n")
          .append("  \"shapes\":{\n")
          .append("    \"AbortMultipartUploadRequest\":{\n")
          .append("      \"type\":\"structure\",\n")
          .append("      \"required\":[\n")
          .append("        \"Bucket\",\n");
        if (requireUploadId) {
            sb.append("        \"UploadId\",\n");
        }
        sb.append("        \"Key\"\n")
          .append("      ],\n")
          .append("      \"members\":{\n")
          .append("        \"Bucket\":{\n")
          .append("          \"shape\":\"BucketName\",\n")
          .append("          \"documentation\":\"<p>Test documentation</p>\",\n")
          .append("          \"contextParam\":{\"name\":\"Bucket\"},\n")
          .append("          \"location\":\"uri\",\n")
          .append("          \"locationName\":\"Bucket\"\n")
          .append("        },\n")
          .append("        \"Key\":{\n")
          .append("          \"shape\":\"ObjectKey\",\n")
          .append("          \"documentation\":\"<p>Key of the object for which the multipart upload was initiated.</p>\",\n")
          .append("          \"location\":\"uri\",\n")
          .append("          \"locationName\":\"Key\"\n")
          .append("        },\n")
          .append("        \"UploadId\":{\n")
          .append("          \"shape\":\"MultipartUploadId\",\n")
          .append("          \"documentation\":\"<p>Upload ID that identifies the multipart upload.</p>\",\n")
          .append("          \"location\":\"querystring\",\n")
          .append("          \"locationName\":\"uploadId\"\n")
          .append("        }\n")
          .append("      }\n")
          .append("    },\n")
          .append("    \"BucketName\":{\"type\":\"string\"},\n")
          .append("    \"BucketVersioningStatus\":{\n")
          .append("      \"type\":\"string\",\n")
          .append("      \"enum\":[\n")
          .append("        \"Enabled\",\n")
          .append("        \"Suspended\"\n")
          .append("      ]\n")
          .append("    },")
          .append("    \"ObjectKey\":{\n")
          .append("      \"type\":\"string\",\n")
          .append("      \"min\":1\n")
          .append("    },")
          .append("    \"MultipartUploadId\":{\"type\":\"string\"},")
          .append("    \"NoSuchUpload\":{\n")
          .append("      \"type\":\"structure\",\n")
          .append("      \"members\":{\n")
          .append("      },\n")
          .append("      \"documentation\":\"<p>The specified multipart upload does not exist.</p>\",\n")
          .append("      \"exception\":true\n")
          .append("    }")
          .append("  },\n")
          .append("  \"documentation\":\"<p/>\",\n")
          .append("  \"clientContextParams\":{\n")
          .append("    \"Accelerate\":{\n")
          .append("      \"documentation\":\"Enables this client to use S3 Transfer Acceleration endpoints.\",\n")
          .append("      \"type\":\"boolean\"\n")
          .append("    },\n")
          .append("    \"DisableMultiRegionAccessPoints\":{\n")
          .append("      \"documentation\":\"Disables this client's usage of Multi-Region Access Points.\",\n")
          .append("      \"type\":\"boolean\"\n")
          .append("    },\n")
          .append("    \"ForcePathStyle\":{\n")
          .append("      \"documentation\":\"Forces this client to use path-style addressing for buckets.\",\n")
          .append("      \"type\":\"boolean\"\n")
          .append("    },\n")
          .append("    \"UseArnRegion\":{\n")
          .append("      \"documentation\":\"Enables this client to use an ARN's region when constructing an endpoint instead of")
          .append(" the client's configured region.\",\n")
          .append("      \"type\":\"boolean\"\n")
          .append("    }\n")
          .append("  }\n")
          .append("}\n");
        return sb.toString();
    }

}
