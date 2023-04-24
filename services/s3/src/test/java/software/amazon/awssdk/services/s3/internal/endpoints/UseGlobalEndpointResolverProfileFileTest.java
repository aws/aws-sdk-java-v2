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

package software.amazon.awssdk.services.s3.internal.endpoints;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.client.config.SdkClientConfiguration;
import software.amazon.awssdk.core.client.config.SdkClientOption;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.ServiceMetadataAdvancedOption;
import software.amazon.awssdk.utils.StringInputStream;

class UseGlobalEndpointResolverProfileFileTest {

    @Test
    void resolve_nonUsEast1_resolvesToFalse() {
        SdkClientConfiguration config = SdkClientConfiguration.builder().build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        assertThat(resolver.resolve(Region.AF_SOUTH_1)).isFalse();
    }

    @Test
    void resolve_nullProfileFileSupplierAndNullDefaultRegionalEndpoint_resolvesToTrue() {
        SdkClientConfiguration config = SdkClientConfiguration
            .builder()
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, null)
            .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint")
            .build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        assertThat(resolver.resolve(Region.US_EAST_1)).isTrue();
    }

    @Test
    void resolve_nullProfileFileSupplierAndDefaultRegionalEndPointLegacy_resolvesToTrue() {
        SdkClientConfiguration config = SdkClientConfiguration
            .builder()
            .option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, "legacy")
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, null)
            .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint")
            .build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        assertThat(resolver.resolve(Region.US_EAST_1)).isTrue();
    }

    @Test
    void resolve_nullProfileFileSupplierAndDefaultRegionalEndPointRegional_resolvesToFalse() {
        SdkClientConfiguration config = SdkClientConfiguration
            .builder()
            .option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, "regional")
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, null)
            .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint")
            .build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        assertThat(resolver.resolve(Region.US_EAST_1)).isFalse();
    }

    @Test
    void resolve_nullProfileFileSupplier_resolvesToTrue() {
        Supplier<ProfileFile> supplier = null;

        SdkClientConfiguration config = SdkClientConfiguration
            .builder()
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, supplier)
            .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint")
            .build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        assertThat(resolver.resolve(Region.US_EAST_1)).isTrue();
    }

    @Test
    void resolve_profileFileSupplierRegionalEndpointLegacy_resolvesToTrue() {
        ProfileFile file = configuration("[profile regional_s3_endpoint]\n"
                                         + "s3_us_east_1_regional_endpoint = legacy");
        Supplier<ProfileFile> supplier = () -> file;

        SdkClientConfiguration config = SdkClientConfiguration
            .builder()
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, supplier)
            .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint")
            .build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        assertThat(resolver.resolve(Region.US_EAST_1)).isTrue();
    }

    @Test
    void resolve_profileFileSupplierRegionalEndpointRegional_resolvesToFalse() {
        ProfileFile file = configuration("[profile regional_s3_endpoint]\n"
                                         + "s3_us_east_1_regional_endpoint = regional");
        Supplier<ProfileFile> supplier = () -> file;

        SdkClientConfiguration config = SdkClientConfiguration
            .builder()
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, supplier)
            .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint")
            .build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        assertThat(resolver.resolve(Region.US_EAST_1)).isFalse();
    }

    private ProfileFile configuration(String string) {
        return ProfileFile.builder()
                          .content(new StringInputStream(string))
                          .type(ProfileFile.Type.CONFIGURATION)
                          .build();
    }

}
