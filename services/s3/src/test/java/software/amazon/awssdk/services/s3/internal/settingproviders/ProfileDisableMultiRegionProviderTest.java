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

import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static software.amazon.awssdk.profiles.ProfileFileSystemSetting.AWS_CONFIG_FILE;

import java.util.Optional;
import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.interceptor.Context;
import software.amazon.awssdk.core.interceptor.ExecutionInterceptor;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.StringInputStream;

public class ProfileDisableMultiRegionProviderTest {
    private ProfileDisableMultiRegionProvider provider = ProfileDisableMultiRegionProvider.create();

    @After
    public void clearSystemProperty() {
        System.clearProperty(AWS_CONFIG_FILE.property());
    }

    @Test
    public void notSpecified_shouldReturnEmptyOptional() {
        assertThat(provider.resolve()).isEqualTo(Optional.empty());
    }

    @Test
    public void specifiedInConfigFile_shouldResolve() {
        String configFile = getClass().getResource("ProfileFile_true").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), configFile);

        assertThat(provider.resolve()).isEqualTo(Optional.of(TRUE));
    }

    @Test
    public void configFile_mixedSpace() {
        String configFile = getClass().getResource("ProfileFile_mixedSpace").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), configFile);

        assertThat(provider.resolve()).isEqualTo(Optional.of(TRUE));
    }

    @Test
    public void unsupportedValue_shouldThrowException() {
        String configFile = getClass().getResource("ProfileFile_unsupportedValue").getFile();
        System.setProperty(AWS_CONFIG_FILE.property(), configFile);

        assertThatThrownBy(() -> provider.resolve()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void specifiedInOverrideConfig_shouldUse() {
        ExecutionInterceptor interceptor = Mockito.spy(AbstractExecutionInterceptor.class);

        String profileFileContent =
            "[default]\n" +
            "s3_disable_multiregion_access_points = true\n";

        ProfileFile profileFile = ProfileFile.builder()
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .content(new StringInputStream(profileFileContent))
                                             .build();

        S3Client s3 = S3Client.builder()
                              .region(Region.US_WEST_2)
                              .credentialsProvider(AnonymousCredentialsProvider.create())
                              .overrideConfiguration(c -> c.defaultProfileFile(profileFile)
                                                           .defaultProfileName("default")
                                                           .addExecutionInterceptor(interceptor)
                                                           .retryPolicy(r -> r.numRetries(0)))
                              .serviceConfiguration(s -> s.useArnRegionEnabled(true))
                              .build();

        String arn = "arn:aws:s3:us-banana-46:12345567890:accesspoint:foo";
        assertThatThrownBy(() -> s3.getObject(r -> r.bucket(arn).key("bar"))).isInstanceOf(SdkException.class);

        ArgumentCaptor<Context.BeforeTransmission> context = ArgumentCaptor.forClass(Context.BeforeTransmission.class);
        Mockito.verify(interceptor).beforeTransmission(context.capture(), any());

        String host = context.getValue().httpRequest().host();
        assertThat(host).contains("us-banana-46");
    }

    public static abstract class AbstractExecutionInterceptor implements ExecutionInterceptor {}
}
