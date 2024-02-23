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

package software.amazon.awssdk.regions.internal.util;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileFileSystemSetting;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.OptionalUtils;

@SdkInternalApi
public final class Ec2MetadataDisableV1Resolver {

    private Ec2MetadataDisableV1Resolver() {
    }

    public static Ec2MetadataDisableV1Resolver create() {
        return new Ec2MetadataDisableV1Resolver();
    }

    public boolean resolve() {
        return OptionalUtils.firstPresent(fromSystemSettings(), Ec2MetadataDisableV1Resolver::fromProfileFile)
                            .orElse(false);
    }

    private static Optional<Boolean> fromSystemSettings() {
        return SdkSystemSetting.AWS_EC2_METADATA_V1_DISABLED.getBooleanValue();
    }

    private static Optional<Boolean> fromProfileFile() {
        Supplier<ProfileFile> profileFile = ProfileFile::defaultProfileFile;
        String profileName = ProfileFileSystemSetting.AWS_PROFILE.getStringValueOrThrow();
        if (profileFile.get() == null) {
            return Optional.empty();
        }
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(p -> p.property(ProfileProperty.EC2_METADATA_V1_DISABLED))
                          .map(Ec2MetadataDisableV1Resolver::safeProfileStringToBoolean);
    }

    private static boolean safeProfileStringToBoolean(String value) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }

        throw new IllegalStateException("Profile property '" + ProfileProperty.EC2_METADATA_V1_DISABLED + "', "
                                        + "was defined as '" + value + "', but should be 'false' or 'true'");
    }

}
