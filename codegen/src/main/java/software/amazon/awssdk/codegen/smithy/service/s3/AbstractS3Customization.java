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

package software.amazon.awssdk.codegen.smithy.service.s3;

import java.util.Collections;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.smithy.service.SmithyServiceCustomization;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * A customization for the S3 service.
 */
@SdkInternalApi
abstract class AbstractS3Customization implements SmithyServiceCustomization {
    private static final String NAMESPACE = "com.amazonaws.s3";

    @Override
    public final Set<String> sdkIds() {
        return Collections.singleton("S3");
    }

    static ShapeId s3(String name) {
        return ShapeId.fromParts(NAMESPACE, name);
    }

    static ShapeId s3Member(String shape, String member) {
        return ShapeId.fromParts(NAMESPACE, shape, member);
    }
}
