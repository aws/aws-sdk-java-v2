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

package software.amazon.awssdk.services.s3.internal.settingproviders;

import static org.assertj.core.api.Assertions.assertThat;
import static software.amazon.awssdk.profiles.ProfileFileSystemSetting.AWS_CONFIG_FILE;
import static software.amazon.awssdk.services.s3.S3SystemSetting.AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS;

import java.util.Optional;
import org.junit.After;
import org.junit.Test;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

public class DisableMultiRegionProviderChainTest {
    private final EnvironmentVariableHelper helper = new EnvironmentVariableHelper();

    @After
    public void clearSystemProperty() {
        System.clearProperty(AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS.property());
        System.clearProperty(AWS_CONFIG_FILE.property());
        helper.reset();
    }

    @Test
    public void notSpecified_shouldReturnEmptyOptional() {
        assertThat(DisableMultiRegionProviderChain.create().resolve()).isEqualTo(Optional.empty());
    }

    @Test
    public void specifiedInBothProviders_systemPropertiesShouldTakePrecedence() {
        System.setProperty(AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS.property(), "false");
        String configFile = getClass().getResource("ProfileFile_true").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), configFile);

        assertThat(DisableMultiRegionProviderChain.create().resolve()).isEqualTo(Optional.of(Boolean.FALSE));
    }

    @Test
    public void systemPropertiesThrowException_shouldUseConfigFile() {
        System.setProperty(AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS.property(), "foobar");
        String configFile = getClass().getResource("ProfileFile_true").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), configFile);

        assertThat(DisableMultiRegionProviderChain.create().resolve()).isEqualTo(Optional.of(Boolean.TRUE));
    }

    @Test
    public void systemPropertiesNotSpecified_shouldUseConfigFile() {
        String configFile = getClass().getResource("ProfileFile_true").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), configFile);

        assertThat(DisableMultiRegionProviderChain.create().resolve()).isEqualTo(Optional.of(Boolean.TRUE));
    }

    @Test
    public void bothProvidersThrowException_shouldReturnEmpty() {
        System.setProperty(AWS_S3_DISABLE_MULTIREGION_ACCESS_POINTS.property(), "foobar");
        String configFile = getClass().getResource("ProfileFile_unsupportedValue").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), configFile);

        assertThat(DisableMultiRegionProviderChain.create().resolve()).isEqualTo(Optional.empty());
    }
}
