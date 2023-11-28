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

package software.amazon.awssdk.services.s3.internal.s3express;

import java.util.function.Supplier;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.profiles.ProfileProperty;
import software.amazon.awssdk.utils.Lazy;
import software.amazon.awssdk.utils.Logger;

/**
 * Resolve the use S3Express auth setting for S3.
 */
@SdkInternalApi
public final class UseS3ExpressAuthResolver {
    private static final Logger LOG = Logger.loggerFor(UseS3ExpressAuthResolver.class);
    private static final String DEFAULT_SETTING = "false";
    private final Lazy<Boolean> useS3ExpressAuth;

    public UseS3ExpressAuthResolver(SdkClientConfiguration config) {
        this.useS3ExpressAuth =
            new Lazy<>(() -> useS3ExpressAuth(() -> config.option(SdkClientOption.PROFILE_FILE),
                                              () -> config.option(SdkClientOption.PROFILE_NAME)));
    }

    public boolean resolve() {
        return useS3ExpressAuth.getValue();
    }

    private boolean useS3ExpressAuth(Supplier<ProfileFile> profileFile, Supplier<String> profileName) {

        String env = envVarSetting();

        if (env != null) {
            return DEFAULT_SETTING.equalsIgnoreCase(env);
        }

        String profile = profileFileSetting(profileFile, profileName);

        if (profile != null) {
            return DEFAULT_SETTING.equalsIgnoreCase(profile);
        }

        return true;
    }


    private static String envVarSetting() {
        return SdkSystemSetting.AWS_S3_DISABLE_EXPRESS_SESSION_AUTH.getStringValue().orElse(null);
    }

    private String profileFileSetting(Supplier<ProfileFile> profileFileSupplier, Supplier<String> profileNameSupplier) {
        try {
            ProfileFile profileFile = profileFileSupplier.get();
            String profileName = profileNameSupplier.get();
            if (profileFile == null || profileName == null) {
                return null;
            }
            return profileFile.profile(profileName)
                              .flatMap(p -> p.property(ProfileProperty.DISABLE_S3_EXPRESS_AUTH))
                              .orElse(null);
        } catch (Exception t) {
            LOG.warn(() -> "Unable to load config file", t);
            return null;
        }
    }

}
