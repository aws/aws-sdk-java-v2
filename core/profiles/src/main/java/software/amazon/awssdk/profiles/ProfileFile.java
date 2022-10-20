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

package software.amazon.awssdk.profiles;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.profiles.internal.ProfileFileReader;
import software.amazon.awssdk.utils.FunctionalUtils;
import software.amazon.awssdk.utils.IoUtils;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.SdkBuilder;

/**
 * Provides programmatic access to the contents of an AWS configuration profile file.
 *
 * AWS configuration profiles allow you to share multiple sets of AWS security credentials between different tools such as the
 * AWS SDK for Java and the AWS CLI.
 *
 * <p>
 * For more information on setting up AWS configuration profiles, see:
 * http://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html
 *
 * <p>
 * A profile file can be created with {@link #builder()} and merged with other profiles files with {@link #aggregator()}. By
 * default, the SDK will use the {@link #defaultProfileFile()} when that behavior hasn't been explicitly overridden.
 */
@SdkPublicApi
public final class ProfileFile {
    public static final String PROFILES_SECTION_TITLE = "profiles";
    private final Map<String, Map<String, Profile>> profilesAndSectionsMap;

    /**
     * @see #builder()
     */
    private ProfileFile(Map<String, Map<String, Map<String, String>>> profilesSectionMap) {
        Validate.paramNotNull(profilesSectionMap, "profilesSectionMap");
        this.profilesAndSectionsMap = convertToProfilesSectionsMap(profilesSectionMap);
    }

    public Optional<Profile> getSection(String sectionName, String sectionTitle) {
        Map<String, Profile> sectionMap = profilesAndSectionsMap.get(sectionName);
        if (sectionMap != null) {
            return Optional.ofNullable(sectionMap.get(sectionTitle));
        }
        return Optional.empty();
    }

    /**
     * Create a builder for a {@link ProfileFile}.
     */
    public static Builder builder() {
        return new BuilderImpl();
    }

    /**
     * Create a builder that can merge multiple {@link ProfileFile}s together.
     */
    public static Aggregator aggregator() {
        return new Aggregator();
    }

    /**
     * Get the default profile file, using the credentials file from "~/.aws/credentials", the config file from "~/.aws/config"
     * and the "default" profile. This default behavior can be customized using the
     * {@link ProfileFileSystemSetting#AWS_SHARED_CREDENTIALS_FILE}, {@link ProfileFileSystemSetting#AWS_CONFIG_FILE} and
     * {@link ProfileFileSystemSetting#AWS_PROFILE} settings or by specifying a different profile file and profile name.
     *
     * <p>
     * The file is read each time this method is invoked.
     */
    public static ProfileFile defaultProfileFile() {
        return ProfileFile.aggregator()
                          .applyMutation(ProfileFile::addCredentialsFile)
                          .applyMutation(ProfileFile::addConfigFile)
                          .build();
    }

    /**
     * Retrieve the profile from this file with the given name.
     *
     * @param profileName The name of the profile that should be retrieved from this file.
     * @return The profile, if available.
     */
    public Optional<Profile> profile(String profileName) {
        Map<String, Profile> profileMap = profilesAndSectionsMap.get(PROFILES_SECTION_TITLE);
        return profileMap != null ? Optional.ofNullable(profileMap.get(profileName)) : Optional.empty();
    }

    /**
     * Retrieve an unmodifiable collection including all of the profiles in this file.
     * @return An unmodifiable collection of the profiles in this file, keyed by profile name.
     */
    public Map<String, Profile> profiles() {
        Map<String, Profile> profileMap = profilesAndSectionsMap.get(PROFILES_SECTION_TITLE);
        return profileMap != null ? Collections.unmodifiableMap(profileMap) : profileMap;
    }

    @Override
    public String toString() {
        return ToString.builder("ProfileFile")
                       .add("profilesAndSectionsMap", profilesAndSectionsMap.values())
                       .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ProfileFile that = (ProfileFile) o;
        return Objects.equals(profilesAndSectionsMap, that.profilesAndSectionsMap);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.profilesAndSectionsMap);
    }

    private static void addCredentialsFile(ProfileFile.Aggregator builder) {
        ProfileFileLocation.credentialsFileLocation()
                           .ifPresent(l -> builder.addFile(ProfileFile.builder()
                                                                      .content(l)
                                                                      .type(ProfileFile.Type.CREDENTIALS)
                                                                      .build()));
    }

    private static void addConfigFile(ProfileFile.Aggregator builder) {
        ProfileFileLocation.configurationFileLocation()
                           .ifPresent(l -> builder.addFile(ProfileFile.builder()
                                                                      .content(l)
                                                                      .type(ProfileFile.Type.CONFIGURATION)
                                                                      .build()));
    }

    /**
     * Convert the sorted map of profile and section properties into a sorted list of profiles and sections.
     * Example: sortedProfilesSectionMap
     * @param sortedProfilesSectionMap : Map of String to Profile/Sessions defined.
     * <pre>
     *     {@code
     *     [profile sso-token]
     *     sso_session = admin
     *     [sso-session admin]
     *     sso_start_url = https://view.awsapps.com/start
     *  }
     * </pre> would look like
     * <pre>
     *     {@code
     *          sortedProfilesSectionMap
     *           profiles --   // Profile Section Title
     *              sso-token --  // Profile Name
     *                  sso_session = admin    // Property definition
     *           sso-session -- // Section title for Sso-sessions
     *              admin --
     *                  sso_start_url = https://view.awsapps.com/start
     *
     *     }
     * </pre>
     * @return Map with keys representing Profiles and sections and value as Map with keys as profile/section name and value as
     * property definition.
     */
    private Map<String, Map<String, Profile>> convertToProfilesSectionsMap(
        Map<String, Map<String, Map<String, String>>> sortedProfilesSectionMap) {

        Map<String, Map<String, Profile>> result = new LinkedHashMap<>();

        sortedProfilesSectionMap.entrySet()
                                .forEach(sections -> {
                                    result.put(sections.getKey(), new LinkedHashMap<>());
                                    Map<String, Profile> stringProfileMap = result.get(sections.getKey());
                                    sections.getValue().entrySet()
                                            .forEach(section -> {
                                                Profile profile = Profile.builder()
                                                                         .name(section.getKey())
                                                                         .properties(section.getValue())
                                                                         .build();
                                                stringProfileMap.put(section.getKey(), profile);

                                            });
                                });
        return result;
    }

    /**
     * The supported types of profile files. The type of profile determines the way in which it is parsed.
     */
    public enum Type {
        /**
         * A configuration profile file, typically located at ~/.aws/config, that expects all profile names (except the default
         * profile) to be prefixed with "profile ". Any non-default profiles without this prefix will be ignored.
         */
        CONFIGURATION,

        /**
         * A credentials profile file, typically located at ~/.aws/credentials, that expects all profile name to have no
         * "profile " prefix. Any profiles with a profile prefix will be ignored.
         */
        CREDENTIALS
    }

    /**
     * A builder for a {@link ProfileFile}. {@link #content(Path)} (or {@link #content(InputStream)}) and {@link #type(Type)} are
     * required fields.
     */
    public interface Builder extends SdkBuilder<Builder, ProfileFile> {
        /**
         * Configure the content of the profile file. This stream will be read from and then closed when {@link #build()} is
         * invoked.
         */
        Builder content(InputStream contentStream);

        /**
         * Configure the location from which the profile file should be loaded.
         */
        Builder content(Path contentLocation);

        /**
         * Configure the {@link Type} of file that should be loaded.
         */
        Builder type(Type type);

        @Override
        ProfileFile build();
    }

    private static final class BuilderImpl implements Builder {
        private InputStream content;
        private Path contentLocation;
        private Type type;

        private BuilderImpl() {
        }

        @Override
        public Builder content(InputStream contentStream) {
            this.contentLocation = null;
            this.content = contentStream;
            return this;
        }

        public void setContent(InputStream contentStream) {
            content(contentStream);
        }

        @Override
        public Builder content(Path contentLocation) {
            Validate.paramNotNull(contentLocation, "profileLocation");
            Validate.validState(Files.exists(contentLocation), "Profile file '%s' does not exist.", contentLocation);

            this.content = null;
            this.contentLocation = contentLocation;
            return this;
        }

        public void setContentLocation(Path contentLocation) {
            content(contentLocation);
        }

        /**
         * Configure the {@link Type} of file that should be loaded.
         */
        @Override
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        public void setType(Type type) {
            type(type);
        }

        @Override
        public ProfileFile build() {
            InputStream stream = content != null ? content :
                                 FunctionalUtils.invokeSafely(() -> Files.newInputStream(contentLocation));

            Validate.paramNotNull(type, "type");
            Validate.paramNotNull(stream, "content");

            try {
                return new ProfileFile(ProfileFileReader.parseFile(stream, type));
            } finally {
                IoUtils.closeQuietly(stream, null);
            }
        }
    }

    /**
     * A mechanism for merging multiple {@link ProfileFile}s together into a single file. This will merge their profiles and
     * properties together.
     */
    public static final class Aggregator implements SdkBuilder<Aggregator, ProfileFile> {
        private List<ProfileFile> files = new ArrayList<>();

        /**
         * Add a file to be aggregated. In the event that there is a duplicate profile/property pair in the files, files added
         * earliest to this aggregator will take precedence, dropping the duplicated properties in the later files.
         */
        public Aggregator addFile(ProfileFile file) {
            files.add(file);
            return this;
        }

        @Override
        public ProfileFile build() {
            Map<String, Map<String, Map<String, String>>> aggregateRawProfiles = new LinkedHashMap<>();
            for (int i = files.size() - 1; i >= 0; --i) {
                files.get(i).profilesAndSectionsMap.entrySet()
                                                   .forEach(sectionKeyValue -> addToAggregate(aggregateRawProfiles,
                                                                                              sectionKeyValue.getValue(),
                                                                                              sectionKeyValue.getKey()));
            }
            return new ProfileFile(aggregateRawProfiles);
        }

        private void addToAggregate(Map<String, Map<String, Map<String, String>>> aggregateRawProfiles,
                                    Map<String, Profile> profiles, String sectionName) {

            aggregateRawProfiles.putIfAbsent(sectionName, new LinkedHashMap<>());
            Map<String, Map<String, String>> profileMap = aggregateRawProfiles.get(sectionName);
            for (Entry<String, Profile> profile : profiles.entrySet()) {
                profileMap.compute(profile.getKey(), (k, current) -> {
                    if (current == null) {
                        return new HashMap<>(profile.getValue().properties());
                    } else {
                        current.putAll(profile.getValue().properties());
                        return current;
                    }
                });
            }
        }
    }
}
