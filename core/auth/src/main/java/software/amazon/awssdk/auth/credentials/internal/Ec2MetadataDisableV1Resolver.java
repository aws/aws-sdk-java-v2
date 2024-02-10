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

package software.amazon.awssdk.auth.credentials.internal;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.OptionalUtils;

@SdkInternalApi
public final class Ec2MetadataDisableV1Resolver {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;
    private final Lazy<Boolean> resolvedValue;

    private Ec2MetadataDisableV1Resolver(Supplier<ProfileFile> profileFile, String profileName) {
        this.profileFile = profileFile;
        this.profileName = profileName;
        this.resolvedValue = new Lazy<>(this::doResolve);
    }

    public static Ec2MetadataDisableV1Resolver create(Supplier<ProfileFile> profileFile, String profileName) {
        return new Ec2MetadataDisableV1Resolver(profileFile, profileName);
    }

    public boolean resolve() {
        return resolvedValue.getValue();
    }

    public boolean doResolve() {
        return OptionalUtils.firstPresent(fromSystemSettings(),
                                          () -> fromProfileFile(profileFile, profileName))
                            .orElse(false);
    }

    private static Optional<Boolean> fromSystemSettings() {
        return SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.getBooleanValue();
    }

    private static Optional<Boolean> fromProfileFile(Supplier<ProfileFile> profileFile, String profileName) {
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(p -> p.booleanProperty(ProfileProperty.EC2_METADATA_V1_DISABLED));
    }

}
