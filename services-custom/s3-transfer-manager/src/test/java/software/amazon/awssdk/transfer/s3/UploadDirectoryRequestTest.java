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

package software.amazon.awssdk.transfer.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Paths;
import org.junit.Test;

public class UploadDirectoryRequestTest {

    @Test
    public void noSourceDirectory_throws() {
        assertThatThrownBy(() ->
            UploadDirectoryRequest.builder().bucket("bucket").build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("sourceDirectory");
    }

    @Test
    public void noBucket_throws() {
        assertThatThrownBy(() ->
                               UploadDirectoryRequest.builder().sourceDirectory(Paths.get(".")).build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("bucket");
    }

    @Test
    public void equals_hashcode() {
        UploadDirectoryRequest request1 = UploadDirectoryRequest
            .builder()
            .bucket("bucket")
            .sourceDirectory(Paths.get("."))
            .overrideConfiguration(o -> o.recursive(true))
            .build();

        UploadDirectoryRequest request2 = request1.toBuilder()
            .build();

        UploadDirectoryRequest request3 = request1.toBuilder()
            .bucket("anotherBucket")
            .build();

        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
        assertThat(request1).isNotEqualTo(request3);
        assertThat(request1.hashCode()).isNotEqualTo(request3.hashCode());
    }
}
