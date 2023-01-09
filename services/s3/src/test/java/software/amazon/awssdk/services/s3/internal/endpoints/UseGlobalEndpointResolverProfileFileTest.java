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
    void useUsEast1RegionalEndpoint_fromProfileFileNotRegional_resolvesFromProperty() {
        ProfileFile file = ProfileFile.builder()
                                      .content(new StringInputStream("[profile regional_s3_endpoint]\n"
                                                                     + "s3_us_east_1_regional_endpoint = ABC"))
                                      .type(ProfileFile.Type.CONFIGURATION)
                                      .build();

        SdkClientConfiguration.Builder configBuilder = SdkClientConfiguration
            .builder()
            .option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, "regional")
            .option(SdkClientOption.PROFILE_FILE, file)
            .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint");

        SdkClientConfiguration config = configBuilder.build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        boolean expected = !"regional".equalsIgnoreCase("ABC");
        assertThat(resolver.resolve(Region.US_EAST_1)).isEqualTo(expected);
    }

    @Test
    void useUsEast1RegionalEndpoint_fromNullProfileFileSupplierAndNullProfileFile_fallsBackToProfileFile() {
        Supplier<ProfileFile> supplier = null;

        SdkClientConfiguration.Builder configBuilder = SdkClientConfiguration
            .builder()
            .option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, "regional")
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, supplier)
            .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint");

        SdkClientConfiguration config = configBuilder.build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        boolean expected =
            !"regional".equalsIgnoreCase(config.option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT));
        assertThat(resolver.resolve(Region.US_EAST_1)).isEqualTo(expected);
    }

    @Test
    void useUsEast1RegionalEndpoint_fromNullProfileFileSupplierAndNonNullProfileFile_fallsBackToProfileFile() {
        Supplier<ProfileFile> supplier = null;
        ProfileFile file = ProfileFile.builder()
                                      .content(new StringInputStream("[profile regional_s3_endpoint]\n"
                                                                     + "s3_us_east_1_regional_endpoint = ABC"))
                                      .type(ProfileFile.Type.CONFIGURATION)
                                      .build();

        SdkClientConfiguration.Builder configBuilder = SdkClientConfiguration
            .builder()
            .option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, "regional")
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, supplier)
            .option(SdkClientOption.PROFILE_FILE, file)
            .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint");

        SdkClientConfiguration config = configBuilder.build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        boolean expected = !"regional".equalsIgnoreCase("ABC");
        assertThat(resolver.resolve(Region.US_EAST_1)).isEqualTo(expected);
    }

    @Test
    void useUsEast1RegionalEndpoint_fromProfileFileSupplierMissingRegionalProperty_resolvesFromDefault() {
        ProfileFile file = ProfileFile.builder()
                                      .content(new StringInputStream("[profile regional_s3_endpoint]\n"
                                                                     + "s3_us_east_1_regional = ABC"))
                                      .type(ProfileFile.Type.CONFIGURATION)
                                      .build();
        Supplier<ProfileFile> supplier = () -> file;

        SdkClientConfiguration.Builder configBuilder = SdkClientConfiguration
            .builder()
            .option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, "regional")
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, supplier)
            .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint");

        SdkClientConfiguration config = configBuilder.build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        boolean expected =
            !"regional".equalsIgnoreCase(config.option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT));
        assertThat(resolver.resolve(Region.US_EAST_1)).isEqualTo(expected);
    }

    @Test
    void useUsEast1RegionalEndpoint_fromProfileFileSupplierNotRegional_resolvesFromProperty() {
        ProfileFile file = ProfileFile.builder()
                                      .content(new StringInputStream("[profile regional_s3_endpoint]\n"
                                                                     + "s3_us_east_1_regional_endpoint = ABC"))
                                      .type(ProfileFile.Type.CONFIGURATION)
                                      .build();
        Supplier<ProfileFile> supplier = () -> file;

        SdkClientConfiguration.Builder configBuilder = SdkClientConfiguration
            .builder()
            .option(ServiceMetadataAdvancedOption.DEFAULT_S3_US_EAST_1_REGIONAL_ENDPOINT, "regional")
            .option(SdkClientOption.PROFILE_FILE_SUPPLIER, supplier)
            .option(SdkClientOption.PROFILE_NAME, "regional_s3_endpoint");

        SdkClientConfiguration config = configBuilder.build();
        UseGlobalEndpointResolver resolver = new UseGlobalEndpointResolver(config);

        boolean expected = !"regional".equalsIgnoreCase("ABC");
        assertThat(resolver.resolve(Region.US_EAST_1)).isEqualTo(expected);
    }

}
