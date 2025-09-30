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

package software.amazon.awssdk.auth.credentials;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;

class SystemSettingsCredentialsProviderTest {

    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @BeforeAll
    public static void methodSetup() {
        System.setProperty(SdkSystemSetting.AWS_ACCESS_KEY_ID.property(), "akid1");
        System.setProperty(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property(), "skid1");
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_ACCESS_KEY_ID.environmentVariable(), "akid2");
        ENVIRONMENT_VARIABLE_HELPER.set(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.environmentVariable(), "skid2");
    }

    @AfterAll
    public static void teardown() {
        System.clearProperty(SdkSystemSetting.AWS_ACCESS_KEY_ID.property());
        System.clearProperty(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property());
        ENVIRONMENT_VARIABLE_HELPER.reset();
    }

    @Test
    void systemPropertyCredentialsProvider_resolveCredentials_returnsCredentialsWithProvider()  {
        AwsCredentials credentials = SystemPropertyCredentialsProvider.create().resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("akid1");
        assertThat(credentials.secretAccessKey()).isEqualTo("skid1");
        assertThat(credentials.providerName()).isPresent().contains("SystemPropertyCredentialsProvider");
    }

    @Test
    void environmentVariableCredentialsProvider_resolveCredentials_returnsCredentialsWithProvider()  {
        AwsCredentials credentials = EnvironmentVariableCredentialsProvider.create().resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("akid2");
        assertThat(credentials.secretAccessKey()).isEqualTo("skid2");
        assertThat(credentials.providerName()).isPresent().contains("EnvironmentVariableCredentialsProvider");
    }
}
