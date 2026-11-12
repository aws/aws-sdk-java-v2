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

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.NAMESPACE;
import static software.amazon.awssdk.codegen.smithy.customization.s3.S3Fixtures.s3Model;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.model.config.customization.CustomizationConfig;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ShapeId;

class S3RetypePartNumberMarkersTest {
    @Test
    void preprocess_makesThePartNumberMarkersIntegers() {
        Model model = new S3RetypePartNumberMarkers().preprocess(s3Model(), CustomizationConfig.create());

        assertThat(model.expectShape(ShapeId.from(NAMESPACE + "PartNumberMarker")).isIntegerShape()).isTrue();
        assertThat(model.expectShape(ShapeId.from(NAMESPACE + "NextPartNumberMarker")).isIntegerShape()).isTrue();
    }
}
