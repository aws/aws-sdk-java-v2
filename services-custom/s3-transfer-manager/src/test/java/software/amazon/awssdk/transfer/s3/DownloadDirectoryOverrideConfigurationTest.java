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

import java.nio.file.Paths;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

class DownloadDirectoryOverrideConfigurationTest {

    @Test
    void defaultBuilder() {
        DownloadDirectoryOverrideConfiguration configuration =
            DownloadDirectoryOverrideConfiguration.builder()
                                                  .build();
        assertThat(configuration.downloadFileRequestTransformer()).isEmpty();
    }

    @Test
    void defaultBuilderWithPropertySet() {
        DownloadDirectoryOverrideConfiguration configuration =
            DownloadDirectoryOverrideConfiguration.builder()
                .downloadFileRequestTransformer((request -> request.destination(Paths.get("."))))
                                                  .build();
        assertThat(configuration.downloadFileRequestTransformer()).isNotEmpty();
    }

    @Test
    void equalsHashCode() {
        EqualsVerifier.forClass(DownloadDirectoryOverrideConfiguration.class).verify();
    }
}
