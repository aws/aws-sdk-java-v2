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

package software.amazon.awssdk.awscore.endpoints;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.OptionalUtils;

@SdkProtectedApi
public final class AccountIdEndpointModeResolver {

    private static final AccountIdEndpointMode SDK_DEFAULT_MODE = AccountIdEndpointMode.PREFERRED;

    private Supplier<ProfileFile> profileFile;
    private String profileName;
    private AccountIdEndpointMode defaultMode;

    private AccountIdEndpointModeResolver() {
    }

    public static AccountIdEndpointModeResolver create() {
        return new AccountIdEndpointModeResolver();
    }

    public AccountIdEndpointModeResolver profileFile(Supplier<ProfileFile> profileFile) {
        this.profileFile = profileFile;
        return this;
    }

    public AccountIdEndpointModeResolver profileName(String profileName) {
        this.profileName = profileName;
        return this;
    }

    public AccountIdEndpointModeResolver defaultMode(AccountIdEndpointMode defaultMode) {
        this.defaultMode = defaultMode;
        return this;
    }

    public AccountIdEndpointMode resolve() {
        return OptionalUtils.firstPresent(fromSystemSettings(),
                                          () -> fromProfileFile(profileFile, profileName))
                            .orElseGet(this::fromDefaultMode);
    }

    private Optional<AccountIdEndpointMode> fromSystemSettings() {
        return SdkSystemSetting.AWS_ACCOUNT_ID_ENDPOINT_MODE.getStringValue()
                                                            .flatMap(this::stringToEnum);
    }

    private Optional<AccountIdEndpointMode> fromProfileFile(Supplier<ProfileFile> profileFile, String profileName) {
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(p -> p.property(ProfileProperty.ACCOUNT_ID_ENDPOINT_MODE))
                          .flatMap(this::stringToEnum);
    }

    private AccountIdEndpointMode fromDefaultMode() {
        return defaultMode != null ? defaultMode : SDK_DEFAULT_MODE;
    }

    private Optional<AccountIdEndpointMode> stringToEnum(String value) {
        return Optional.of(AccountIdEndpointMode.fromValue(value));
    }
}
