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

package software.amazon.awssdk.services.s3.internal.usearnregion;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.profiles.ProfileFileSystemSetting.AWS_CONFIG_FILE;

import java.util.Optional;
import org.junit.After;
import org.junit.Test;

public class ProfileUseArnRegionProviderTest {
    private ProfileUseArnRegionProvider provider = ProfileUseArnRegionProvider.create();

    @After
    public void clearSystemProperty() {
        System.clearProperty(AWS_CONFIG_FILE.property());
    }

    @Test
    public void notSpecified_shouldReturnEmptyOptional() {
        assertThat(provider.resolveUseArnRegion()).isEqualTo(Optional.empty());
    }

    @Test
    public void specifiedInConfigFile_shouldResolve() {
        String configFile = getClass().getResource("UseArnRegionSet_true").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), configFile);

        assertThat(provider.resolveUseArnRegion()).isEqualTo(Optional.of(TRUE));
    }

    @Test
    public void configFile_mixedSpace() {
        String configFile = getClass().getResource("UseArnRegionSet_mixedSpace").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), configFile);

        assertThat(provider.resolveUseArnRegion()).isEqualTo(Optional.of(FALSE));
    }

    @Test
    public void unsupportedValue_shouldThrowException() {
        String configFile = getClass().getResource("UseArnRegionSet_unsupportedValue").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), configFile);

        assertThatThrownBy(() -> provider.resolveUseArnRegion()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void commaNoSpace_shouldResolveCorrectly() {
        String configFile = getClass().getResource("UseArnRegionSet_noSpace").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), configFile);

        assertThat(provider.resolveUseArnRegion()).isEqualTo(Optional.of(FALSE));
    }
}
