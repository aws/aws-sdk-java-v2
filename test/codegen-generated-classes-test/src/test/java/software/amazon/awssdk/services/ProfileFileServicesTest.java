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

package software.amazon.awssdk.services;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.profiles.Profile;
import software.amazon.awssdk.profiles.ProfileFile;

import static org.assertj.core.api.Assertions.assertThat;

public class ProfileFileServicesTest {

    @Test
    public void servicesSection_shouldRecognizeServicesSection() {
        String profileContent =
            "[services dev]\n" +
            "s3 = \n" +
            "  endpoint_url = https://foo.bar:9000\n";

        ProfileFile profileFile = ProfileFile.builder()
                                             .content(profileContent)
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();

        Optional<Profile> servicesSection = profileFile.getSection("services", "dev");
        assertThat(servicesSection).isPresent();
    }

    @Test
    public void servicesSection_canParseServicesSectionProperties() {
        String profileContent =
            "[services dev]\n" +
            "s3 = \n" +
            "  endpoint_url = https://foo.bar:9000\n" +
            "  foo = bar\n" +
            "\n" +
            "[profile test-profile]\n" +
            "services = dev";

        ProfileFile profileFile = ProfileFile.builder()
                                             .content(profileContent)
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();

        Optional<Profile> servicesSection = profileFile.getSection("services", "dev");
        assertThat(servicesSection).isPresent();

        Profile services = servicesSection.get();
        assertThat(services.properties())
            .containsEntry("s3.endpoint_url", "https://foo.bar:9000");
    }


    @Test
    public void servicesSection_canParseMultipleServicesInSection() {
        String profileContent =
            "[services testing-s3-and-eb]\n" +
            "s3 = \n" +
            "  endpoint_url = http://localhost:4567\n" +
            "elastic_beanstalk = \n" +
            "  endpoint_url = http://localhost:8000\n" +
            "\n" +
            "[profile dev]\n" +
            "services = testing-s3-and-eb";

        ProfileFile profileFile = ProfileFile.builder()
                                             .content(profileContent)
                                             .type(ProfileFile.Type.CONFIGURATION)
                                             .build();

        Optional<Profile> servicesSection = profileFile.getSection("services", "testing-s3-and-eb");
        assertThat(servicesSection).isPresent();

        Profile services = servicesSection.get();
        assertThat(services.properties())
            .containsEntry("s3.endpoint_url", "http://localhost:4567")
            .containsEntry("elastic_beanstalk.endpoint_url", "http://localhost:8000");
    }
}