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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.useragent.BusinessMetricFeatureId;
import software.amazon.awssdk.testutils.EnvironmentVariableHelper;
import software.amazon.awssdk.utils.Pair;

class SystemSettingCredentialsProvidersTest {

    private static final Pair<SdkSystemSetting, String> ACCESS_KEY_ID = Pair.of(SdkSystemSetting.AWS_ACCESS_KEY_ID, "access");
    private static final Pair<SdkSystemSetting, String> SECRET_KEY = Pair.of(SdkSystemSetting.AWS_SECRET_ACCESS_KEY, "secret");
    private static final Pair<SdkSystemSetting, String> SESSION_TOKEN = Pair.of(SdkSystemSetting.AWS_SESSION_TOKEN, "token");
    private static final Pair<SdkSystemSetting, String> ACCOUNT_ID = Pair.of(SdkSystemSetting.AWS_ACCOUNT_ID, "accountid");
    private static final EnvironmentVariableHelper ENVIRONMENT_VARIABLE_HELPER = new EnvironmentVariableHelper();

    @BeforeEach
    public void setup() {
        clearSettings();
    }

    @AfterEach
    public void teardown() {
        clearSettings();
    }

    public static void clearSettings() {
        ENVIRONMENT_VARIABLE_HELPER.reset();
        System.clearProperty(SdkSystemSetting.AWS_ACCESS_KEY_ID.property());
        System.clearProperty(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property());
        System.clearProperty(SdkSystemSetting.AWS_SESSION_TOKEN.property());
        System.clearProperty(SdkSystemSetting.AWS_ACCOUNT_ID.property());
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("config")
    void configureEnvVars_resolveCredentials(String description,
                                             List<Pair<SdkSystemSetting, String>> systemSettings,
                                             Consumer<AwsCredentials> expected) {
        configureEnvironmentVariables(systemSettings);
        EnvironmentVariableCredentialsProvider provider = EnvironmentVariableCredentialsProvider.create();
        if (expected != null) {
            AwsCredentials resolvedCredentials = provider.resolveCredentials();
            assertThat(resolvedCredentials).satisfies(expected);
            assertThat(resolvedCredentials.providerName()).isPresent().contains(BusinessMetricFeatureId.CREDENTIALS_ENV_VARS.value());
        } else {
            assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
        }
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("config")
    void configureSystemProperties_resolveCredentials(String description,
                                                      List<Pair<SdkSystemSetting, String>> systemSettings,
                                                      Consumer<AwsCredentials> expected) {
        configureSystemProperties(systemSettings);
        SystemPropertyCredentialsProvider provider = SystemPropertyCredentialsProvider.create();
        if (expected != null) {
            AwsCredentials resolvedCredentials = provider.resolveCredentials();
            assertThat(resolvedCredentials).satisfies(expected);
            assertThat(resolvedCredentials.providerName()).isPresent().contains(BusinessMetricFeatureId.CREDENTIALS_JVM_SYSTEM_PROPERTIES.value());

        } else {
            assertThatThrownBy(provider::resolveCredentials).isInstanceOf(SdkClientException.class);
        }
    }

    private static List<Arguments> config() {
        return Arrays.asList(
            Arguments.of("When access key id and secret is set, return basic credentials",
                         Arrays.asList(ACCESS_KEY_ID, SECRET_KEY),
                         (Consumer<AwsCredentials>) awsCredentials -> {
                             assertThat(awsCredentials.accessKeyId()).isEqualTo("access");
                             assertThat(awsCredentials.secretAccessKey()).isEqualTo("secret");
                             assertThat(awsCredentials).isNotInstanceOf(AwsSessionCredentials.class);
                             assertThat(awsCredentials).hasFieldOrPropertyWithValue("accountId", null);
                         }),
            Arguments.of("When access key id, secret and token is set, return session credentials",
                         Arrays.asList(ACCESS_KEY_ID, SECRET_KEY, SESSION_TOKEN),
                         (Consumer<AwsCredentials>) awsCredentials -> {
                             assertThat(awsCredentials).isInstanceOf(AwsSessionCredentials.class);
                             assertThat(((AwsSessionCredentials) awsCredentials).sessionToken()).isEqualTo("token");
                         }),
            Arguments.of("When access key id is null, throw exception", Arrays.asList(SECRET_KEY), null),
            Arguments.of("When secret key is null, throw exception", Arrays.asList(ACCESS_KEY_ID), null),
            Arguments.of("When account id is set, return basic credentials with account id",
                         Arrays.asList(ACCESS_KEY_ID, SECRET_KEY, ACCOUNT_ID),
                         (Consumer<AwsCredentials>) awsCredentials -> {
                             assertThat(awsCredentials.accessKeyId()).isEqualTo("access");
                             assertThat(awsCredentials.secretAccessKey()).isEqualTo("secret");
                             assertThat(awsCredentials.accountId()).isPresent().isEqualTo(Optional.of("accountid"));
                             assertThat(awsCredentials).isNotInstanceOf(AwsSessionCredentials.class);
                         }),
            Arguments.of("When account id and token is set, return session credentials with account id",
                         Arrays.asList(ACCESS_KEY_ID, SECRET_KEY, SESSION_TOKEN, ACCOUNT_ID),
                         (Consumer<AwsCredentials>) awsCredentials -> {
                             assertThat(awsCredentials.accessKeyId()).isEqualTo("access");
                             assertThat(awsCredentials.secretAccessKey()).isEqualTo("secret");
                             assertThat(awsCredentials.accountId()).isPresent().isEqualTo(Optional.of("accountid"));
                             assertThat(awsCredentials).isInstanceOf(AwsSessionCredentials.class);
                         })
        );
    }

    private void configureEnvironmentVariables(List<Pair<SdkSystemSetting, String>> systemSettings) {
        for (Pair<SdkSystemSetting, String> setting : systemSettings) {
            ENVIRONMENT_VARIABLE_HELPER.set(setting.left(), setting.right());
        }
    }

    private void configureSystemProperties(List<Pair<SdkSystemSetting, String>> systemSettings) {
        for (Pair<SdkSystemSetting, String> setting : systemSettings) {
            System.setProperty(setting.left().property(), setting.right());
        }
    }
}
