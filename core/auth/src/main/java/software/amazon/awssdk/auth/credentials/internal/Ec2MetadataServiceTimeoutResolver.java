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

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.OptionalUtils;

@SdkInternalApi
public final class Ec2MetadataServiceTimeoutResolver {
    private final Supplier<ProfileFile> profileFile;
    private final String profileName;
    private final Lazy<Long> resolvedValue;

    private Ec2MetadataServiceTimeoutResolver(Supplier<ProfileFile> profileFile, String profileName) {
        this.profileFile = profileFile;
        this.profileName = profileName;
        this.resolvedValue = new Lazy<>(this::doResolve);
    }

    public static Ec2MetadataServiceTimeoutResolver create(Supplier<ProfileFile> profileFile, String profileName) {
        return new Ec2MetadataServiceTimeoutResolver(profileFile, profileName);
    }

    private static Optional<Long> fromSystemSettings() {
        return SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.getNonDefaultStringValue()
                                                            .map(Ec2MetadataServiceTimeoutResolver::parseTimeoutValue);
    }

    private static Optional<Long> fromProfileFile(Supplier<ProfileFile> profileFile, String profileName) {
        return profileFile.get()
                          .profile(profileName)
                          .flatMap(p -> p.property(ProfileProperty.METADATA_SERVICE_TIMEOUT))
                          .map(Ec2MetadataServiceTimeoutResolver::parseTimeoutValue);
    }

    private static long parseTimeoutValue(String timeoutValue) {
        try {
            int timeoutSeconds = Integer.parseInt(timeoutValue);
            return Duration.ofSeconds(timeoutSeconds).toMillis();
        } catch (NumberFormatException e) {
            try {
                double timeoutSeconds = Double.parseDouble(timeoutValue);
                return Math.round(timeoutSeconds * 1000);
            } catch (NumberFormatException ignored) {
                throw new IllegalStateException(String.format(
                    "Timeout value '%s' is not a valid integer or double.",
                    timeoutValue
                ));
            }
        }
    }

    public long resolve() {
        return resolvedValue.getValue();
    }

    private long doResolve() {
        return OptionalUtils.firstPresent(fromSystemSettings(),
                                          () -> fromProfileFile(profileFile, profileName))
                            .orElseGet(() -> parseTimeoutValue(SdkSystemSetting.AWS_METADATA_SERVICE_TIMEOUT.defaultValue()));
    }
}
