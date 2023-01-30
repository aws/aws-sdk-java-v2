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

package software.amazon.awssdk.transfer.s3.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Paths;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

public class UploadDirectoryRequestTest {

    @Test
    public void noSourceDirectory_throws() {
        assertThatThrownBy(() ->
                               UploadDirectoryRequest.builder().bucket("bucket").build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("source");
    }

    @Test
    public void noBucket_throws() {
        assertThatThrownBy(() ->
                               UploadDirectoryRequest.builder().source(Paths.get(".")).build()
        ).isInstanceOf(NullPointerException.class).hasMessageContaining("bucket");
    }

    @Test
    public void equals_hashcode() {
        EqualsVerifier.forClass(UploadDirectoryRequest.class)
                      .withNonnullFields("source", "bucket")
                      .verify();
    }
}
