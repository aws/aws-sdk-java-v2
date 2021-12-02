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

import java.util.concurrent.Executor;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;
import org.mockito.Mockito;

public class S3TransferManagerOverrideConfigurationTest {

    @Test
    public void build_allProperties() {
        Executor executor = Mockito.mock(Executor.class);
        UploadDirectoryOverrideConfiguration directoryOverrideConfiguration =
            UploadDirectoryOverrideConfiguration.builder()
                                                .build();
        S3TransferManagerOverrideConfiguration configuration =
            S3TransferManagerOverrideConfiguration.builder()
                                                  .uploadDirectoryConfiguration(directoryOverrideConfiguration)
                                                  .executor(executor)
                                                  .build();

        assertThat(configuration.executor()).contains(executor);
        assertThat(configuration.uploadDirectoryConfiguration()).contains(directoryOverrideConfiguration);
    }

    @Test
    public void build_emptyBuilder() {
        S3TransferManagerOverrideConfiguration configuration = S3TransferManagerOverrideConfiguration.builder()
                                                                                                     .build();

        assertThat(configuration.executor()).isEmpty();
        assertThat(configuration.uploadDirectoryConfiguration()).isEmpty();
    }

    @Test
    public void equalsHashCode() {
        EqualsVerifier.forClass(S3TransferManagerOverrideConfiguration.class)
                      .verify();
    }
}
