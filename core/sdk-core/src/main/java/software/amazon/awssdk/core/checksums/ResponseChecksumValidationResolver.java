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

package software.amazon.awssdk.core.checksums;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.OptionalUtils;

/**
 * Allows customizing the variables used during determination of a {@link ResponseChecksumValidation}.
 * Created via{@link #create()}.
 */
@SdkProtectedApi
public final class ResponseChecksumValidationResolver {

    private static final ResponseChecksumValidation SDK_DEFAULT_CHECKSUM_VALIDATION = ResponseChecksumValidation.WHEN_SUPPORTED;

    private Supplier<ProfileFile> profileFile;
    private String profileName;
    private ResponseChecksumValidation defaultChecksumValidation;

    private ResponseChecksumValidationResolver() {
    }

    public static ResponseChecksumValidationResolver create() {
        return new ResponseChecksumValidationResolver();
    }

    /**
     * Configure the profile file that should be used when determining the {@link ResponseChecksumValidation}. The supplier is
     * only consulted if a higher-priority determinant (e.g. environment variables) does not find the setting.
     */
    public ResponseChecksumValidationResolver profileFile(Supplier<ProfileFile> profileFile) {
        this.profileFile = profileFile;
        return this;
    }

    /**
     * Configure the profile file name should be used when determining the {@link ResponseChecksumValidation}.
     */
    public ResponseChecksumValidationResolver profileName(String profileName) {
        this.profileName = profileName;
        return this;
    }

    /**
     * Configure the {@link ResponseChecksumValidation} that should be used if the mode is not specified anywhere else.
     */
    public ResponseChecksumValidationResolver defaultChecksumValidation(ResponseChecksumValidation defaultChecksumValidation) {
        this.defaultChecksumValidation = defaultChecksumValidation;
        return this;
    }

    /**
     * Resolve which response checksum validation setting should be used, based on the configured values.
     */
    public ResponseChecksumValidation resolve() {
        return OptionalUtils.firstPresent(fromSystemSettings(),
                                          () -> fromProfileFile(profileFile, profileName))
                            .orElseGet(this::fromDefaultChecksumValidation);
    }

    private Optional<ResponseChecksumValidation> fromSystemSettings() {
        return SdkSystemSetting.AWS_RESPONSE_CHECKSUM_VALIDATION.getStringValue()
                                                                .flatMap(this::stringToEnum);
    }

    private Optional<ResponseChecksumValidation> fromProfileFile(Supplier<ProfileFile> profileFile, String profileName) {
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(p -> p.property(ProfileProperty.RESPONSE_CHECKSUM_VALIDATION))
                          .flatMap(this::stringToEnum);
    }

    private ResponseChecksumValidation fromDefaultChecksumValidation() {
        return defaultChecksumValidation != null ? defaultChecksumValidation : SDK_DEFAULT_CHECKSUM_VALIDATION;
    }

    private Optional<ResponseChecksumValidation> stringToEnum(String value) {
        return Optional.of(ResponseChecksumValidation.fromValue(value));
    }
}
