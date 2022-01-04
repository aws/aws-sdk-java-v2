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

package software.amazon.awssdk.regions;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.core.SdkSystemSetting;
import software.amazon.awssdk.core.client.config.ClientOption;
import software.amazon.awssdk.profiles.ProfileProperty;


/**
 * A collection of advanced options that can be configured on a {@link ServiceMetadata} via
 * {@link ServiceMetadataConfiguration.Builder#putAdvancedOption(ServiceMetadataAdvancedOption, Object)}.
 *
 * @param <T> The type of value associated with the option.
 */
@SdkPublicApi
public class ServiceMetadataAdvancedOption<T> extends ClientOption<T> {

    /**
     * The default S3 regional endpoint setting for the {@code us-east-1} region to use. Setting
     * the value to {@code regional} causes the SDK to use the {@code s3.us-east-1.amazonaws.com} endpoint when using the
     * {@link Region#US_EAST_1} region instead of the global {@code s3.amazonaws.com} by default if it's not configured otherwise
     * via {@link SdkSystemSetting#AWS_S3_US_EAST_1_REGIONAL_ENDPOINT} or {@link ProfileProperty#S3_US_EAST_1_REGIONAL_ENDPOINT}
     */
    public static final ServiceMetadataAdvancedOption<String> DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT =
        new ServiceMetadataAdvancedOption<>(String.class);

    protected ServiceMetadataAdvancedOption(Class<T> valueClass) {
        super(valueClass);
    }
}
