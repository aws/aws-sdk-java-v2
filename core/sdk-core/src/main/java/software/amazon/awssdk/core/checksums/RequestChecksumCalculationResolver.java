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
 * Allows customizing the variables used during determination of a {@link RequestChecksumCalculation}.
 * Created via{@link #create()}.
 */
@SdkProtectedApi
public final class RequestChecksumCalculationResolver {

    private static final RequestChecksumCalculation SDK_DEFAULT_CHECKSUM_CALCULATION = RequestChecksumCalculation.WHEN_SUPPORTED;

    private Supplier<ProfileFile> profileFile;
    private String profileName;
    private RequestChecksumCalculation defaultChecksumCalculation;

    private RequestChecksumCalculationResolver() {
    }

    public static RequestChecksumCalculationResolver create() {
        return new RequestChecksumCalculationResolver();
    }

    /**
     * Configure the profile file that should be used when determining the {@link RequestChecksumCalculation}. The supplier is
     * only consulted if a higher-priority determinant (e.g. environment variables) does not find the setting.
     */
    public RequestChecksumCalculationResolver profileFile(Supplier<ProfileFile> profileFile) {
        this.profileFile = profileFile;
        return this;
    }

    /**
     * Configure the profile file name should be used when determining the {@link RequestChecksumCalculation}.
     */
    public RequestChecksumCalculationResolver profileName(String profileName) {
        this.profileName = profileName;
        return this;
    }

    /**
     * Configure the {@link RequestChecksumCalculation} that should be used if the mode is not specified anywhere else.
     */
    public RequestChecksumCalculationResolver defaultChecksumCalculation(RequestChecksumCalculation defaultChecksumCalculation) {
        this.defaultChecksumCalculation = defaultChecksumCalculation;
        return this;
    }

    /**
     * Resolve which request checksum calculation setting should be used, based on the configured values.
     */
    public RequestChecksumCalculation resolve() {
        return OptionalUtils.firstPresent(fromSystemSettings(),
                                          () -> fromProfileFile(profileFile, profileName))
                            .orElseGet(this::fromDefaultChecksumCalculation);
    }

    private Optional<RequestChecksumCalculation> fromSystemSettings() {
        return SdkSystemSetting.AWS_REQUEST_CHECKSUM_CALCULATION.getStringValue()
                                                                .flatMap(this::stringToEnum);
    }

    private Optional<RequestChecksumCalculation> fromProfileFile(Supplier<ProfileFile> profileFile, String profileName) {
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(p -> p.property(ProfileProperty.REQUEST_CHECKSUM_CALCULATION))
                          .flatMap(this::stringToEnum);
    }

    private RequestChecksumCalculation fromDefaultChecksumCalculation() {
        return defaultChecksumCalculation != null ? defaultChecksumCalculation : SDK_DEFAULT_CHECKSUM_CALCULATION;
    }

    private Optional<RequestChecksumCalculation> stringToEnum(String value) {
        return Optional.of(RequestChecksumCalculation.fromValue(value));
    }
}
