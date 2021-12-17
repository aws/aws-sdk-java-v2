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

package software.amazon.awssdk.regions.servicemetadata;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import software.amazon.awssdk.regions.GeneratedServiceMetadataProvider;

public class GeneratedServiceMetadataProviderTest {
    private static final GeneratedServiceMetadataProvider PROVIDER = new GeneratedServiceMetadataProvider();

    @Test
    public void s3Metadata_isEnhanced() {
        assertThat(PROVIDER.serviceMetadata("s3")).isInstanceOf(EnhancedS3ServiceMetadata.class);
    }
}
