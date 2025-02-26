// /*
//  * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License").
//  * You may not use this file except in compliance with the License.
//  * A copy of the License is located at
//  *
//  *  http://aws.amazon.com/apache2.0
//  *
//  * or in the "license" file accompanying this file. This file is distributed
//  * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
//  * express or implied. See the License for the specific language governing
//  * permissions and limitations under the License.
//  */
//
// package software.amazon.awssdk.profiles;
//
// public class ProfileServiceConfigApplier {
//     private final ProfileFile profileFile;
//     private final String profileName;
//
//     public ProfileServiceConfigApplier(ProfileFile profileFile, String profileName) {
//         this.profileFile = profileFile;
//         this.profileName = profileName;
//     }
//
//     public Optional<URI> getEndpointOverride(String serviceName) {
//         return profileFile.profile(profileName)
//                           .flatMap(profile -> profile.property("services"))
//                           .flatMap(servicesSection -> profileFile.getSection("services", servicesSection))
//                           .flatMap(services -> Optional.ofNullable(
//                               services.properties().get(serviceName + ".endpoint_url")))
//                           .map(URI::create);
//     }
//
//     public void configure(SdkClientBuilder<?, ?> builder, String serviceName) {
//         getEndpointOverride(serviceName).ifPresent(builder::endpointOverride);
//     }
// }
