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

package software.amazon.awssdk.codegen.smithy.customization.s3;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.intermediate.IntermediateModel;
import software.amazon.awssdk.codegen.model.intermediate.MemberModel;
import software.amazon.awssdk.codegen.model.intermediate.ParameterHttpMapping;
import software.amazon.awssdk.codegen.model.intermediate.ShapeMarshaller;
import software.amazon.awssdk.codegen.model.intermediate.ShapeModel;
import software.amazon.awssdk.utils.StringUtils;

/**
 * Removes the bucket label and {@code x-id} from request URIs; the endpoint rules place the bucket. First puts the
 * {@code Bucket} and {@code Key} members of CopyObject and UploadPartCopy, renamed to {@code DestinationBucket} and
 * {@code DestinationKey}, back on their old URI labels and makes them optional.
 */
@SdkInternalApi
final class S3RemoveBucketFromUri extends AbstractS3Customization {
    private static final List<String> COPY_REQUESTS = Arrays.asList("CopyObjectRequest", "UploadPartCopyRequest");
    private static final Map<String, String> RENAMES = renames();

    @Override
    public boolean appliesConfig(String key) {
        return COPY_REQUESTS.stream().anyMatch(shape -> RENAMES.keySet().stream().anyMatch(
            member -> key.equals("shapeModifiers." + shape + ".modify." + member + ".required")));
    }

    @Override
    public void postprocess(IntermediateModel model) {
        // First, so the URI edit below finds the {Bucket} label this puts back.
        for (String shapeName : COPY_REQUESTS) {
            ShapeModel shape = Utils.findShapeModelByC2jNameIfExists(model, shapeName);
            if (shape != null) {
                RENAMES.forEach((oldName, newName) -> restoreRenamedMember(shape, oldName, newName));
            }
        }
        for (ShapeModel shape : model.getShapes().values()) {
            ShapeMarshaller marshaller = shape.getMarshaller();
            if (marshaller != null && marshaller.getRequestUri() != null) {
                marshaller.setRequestUri(withoutBucketAndXId(marshaller.getRequestUri()));
            }
        }
    }

    private static Map<String, String> renames() {
        Map<String, String> renames = new LinkedHashMap<>();
        renames.put("Bucket", "DestinationBucket");
        renames.put("Key", "DestinationKey");
        return renames;
    }

    /**
     * Makes the renamed member optional, with its old name in the shape's required list, and puts its URI label back on
     * the old name. Does nothing when the member still has its old name.
     */
    private static void restoreRenamedMember(ShapeModel shape, String oldName, String newName) {
        MemberModel member = shape.getMemberByC2jName(newName);
        if (member == null) {
            if (shape.getMemberByC2jName(oldName) == null) {
                throw new IllegalStateException(shape.getC2jName() + " has neither " + oldName + " nor " + newName);
            }
            return;
        }
        if (member.isRequired()) {
            member.setRequired(false);
            List<String> required = shape.getRequired();
            required.set(required.indexOf(newName), oldName);
        }
        ParameterHttpMapping http = member.getHttp();
        if (http != null && http.isUri()) {
            http.setMarshallLocationName(oldName);
            http.setUnmarshallLocationName(oldName);
            String uri = shape.getMarshaller().getRequestUri()
                              .replace("{" + newName + "}", "{" + oldName + "}")
                              .replace("{" + newName + "+}", "{" + oldName + "+}");
            shape.getMarshaller().setRequestUri(uri);
        }
    }

    private static String withoutBucketAndXId(String requestUri) {
        String uri = StringUtils.replaceOnce(requestUri, "/{Bucket}", "");
        int queryStart = uri.indexOf('?');
        if (queryStart < 0) {
            return uri;
        }
        String kept = Arrays.stream(uri.substring(queryStart + 1).split("&"))
                            .filter(p -> !p.startsWith("x-id="))
                            .collect(Collectors.joining("&"));
        return uri.substring(0, queryStart) + (kept.isEmpty() ? "" : "?" + kept);
    }
}
