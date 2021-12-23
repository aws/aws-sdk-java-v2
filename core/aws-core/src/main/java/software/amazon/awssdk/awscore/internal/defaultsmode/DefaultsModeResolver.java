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

package software.amazon.awssdk.awscore.internal.defaultsmode;

import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.awscore.defaultsmode.DefaultsMode;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.retry.RetryMode;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.OptionalUtils;

/**
 * Allows customizing the variables used during determination of a {@link DefaultsMode}. Created via {@link #create()}.
 */
@SdkInternalApi
public final class DefaultsModeResolver {

    private static final DefaultsMode SDK_DEFAULT_DEFAULTS_MODE = DefaultsMode.LEGACY;
    private Supplier<ProfileFile> profileFile;
    private String profileName;
    private DefaultsMode mode;

    private DefaultsModeResolver() {
    }

    public static DefaultsModeResolver create() {
        return new DefaultsModeResolver();
    }

    /**
     * Configure the profile file that should be used when determining the {@link RetryMode}. The supplier is only consulted
     * if a higher-priority determinant (e.g. environment variables) does not find the setting.
     */
    public DefaultsModeResolver profileFile(Supplier<ProfileFile> profileFile) {
        this.profileFile = profileFile;
        return this;
    }

    /**
     * Configure the profile file name should be used when determining the {@link RetryMode}.
     */
    public DefaultsModeResolver profileName(String profileName) {
        this.profileName = profileName;
        return this;
    }

    /**
     * Configure the {@link DefaultsMode} that should be used if the mode is not specified anywhere else.
     */
    public DefaultsModeResolver defaultMode(DefaultsMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Resolve which defaults mode should be used, based on the configured values.
     */
    public DefaultsMode resolve() {
        return OptionalUtils.firstPresent(DefaultsModeResolver.fromSystemSettings(), () -> fromProfileFile(profileFile,
                                                                                                           profileName))
                            .orElseGet(this::fromDefaultMode);
    }

    private static Optional<DefaultsMode> fromSystemSettings() {
        return SdkSystemSetting.AWS_DEFAULTS_MODE.getStringValue()
                                                 .map(value -> DefaultsMode.fromValue(value.toLowerCase(Locale.US)));
    }

    private static Optional<DefaultsMode> fromProfileFile(Supplier<ProfileFile> profileFile, String profileName) {
        profileFile = profileFile != null ? profileFile : ProfileFile::defaultProfileFile;
        profileName = profileName != null ? profileName : ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(p -> p.property(ProfileProperty.DEFAULTS_MODE))
                          .map(value -> DefaultsMode.fromValue(value.toLowerCase(Locale.US)));
    }

    private DefaultsMode fromDefaultMode() {
        return mode != null ? mode : SDK_DEFAULT_DEFAULTS_MODE;
    }
}
