/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.ToString;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * A named collection of configuration stored in a {@link ProfileFile}.
 *
 * Raw property access can be made via {@link #property(String)} and {@link #properties()}.
 *
 * @see ProfileFile
 */
@SdkPublicApi
public final class Profile implements ToCopyableBuilder<Profile.Builder, Profile> {

    /**
     * The name of this profile (minus any profile prefixes).
     */
    private final String name;

    /**
     * The raw properties in this profile.
     */
    private final Map<String, String> properties;

    /**
     * @see ProfileFile
     * @see #builder()
     */
    private Profile(Builder builder) {
        this.name = Validate.paramNotNull(builder.name, "name");
        this.properties = Validate.paramNotNull(builder.properties, "properties");
    }

    /**
     * Create a builder for defining a profile with specific attributes. For reading profiles from a file, see
     * {@link ProfileFile}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Retrieve the name of this profile.
     */
    public String name() {
        return name;
    }

    /**
     * Retrieve a specific raw property from this profile.
     *
     * @param propertyKey The name of the property to retrieve.
     * @return The value of the property, if configured.
     */
    public Optional<String> property(String propertyKey) {
        return Optional.ofNullable(properties.get(propertyKey));
    }

    /**
     * Retrieve an unmodifiable view of all of the properties currently in this profile.
     */
    public Map<String, String> properties() {
        return properties;
    }

    @Override
    public Builder toBuilder() {
        return builder().name(name)
                        .properties(properties);
    }

    @Override
    public String toString() {
        return ToString.builder("Profile")
                       .add("name", name)
                       .add("properties", properties.keySet())
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
        final Profile profile = (Profile) o;
        return Objects.equals(name, profile.name) &&
               Objects.equals(properties, profile.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, properties);
    }

    /**
     * A builder for a {@link Profile}. See {@link #builder()}.
     */
    public static class Builder implements CopyableBuilder<Builder, Profile> {
        private String name;
        private Map<String, String> properties;

        /**
         * @see #builder()
         */
        private Builder() {}

        /**
         * Define the name of this profile, without the legacy "profile" prefix.
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Define the properties configured in this profile.
         */
        public Builder properties(Map<String, String> properties) {
            this.properties = Collections.unmodifiableMap(new LinkedHashMap<>(properties));
            return this;
        }

        /**
         * Create a profile using the current state of this builder.
         */
        public Profile build() {
            return new Profile(this);
        }
    }
}
