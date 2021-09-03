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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.utils.Validate;
import software.amazon.awssdk.utils.builder.CopyableBuilder;
import software.amazon.awssdk.utils.builder.ToCopyableBuilder;

/**
 * This class represents the concept of a regional scope, in form of a string with possible wildcards.
 * <p/>
 * The string can contain a region value such as us-east-1, or add the wildcard '*' at the end of the region
 * string to represent all possible regional combinations that can match the expression. A wildcard must be
 * it's own segment and preceeded by a '-' dash, unless the whole expression is the wildcard.
 * <p/>
 * Examples of valid combinations:
 * <ul>
 *     <li>'us-east-1' - Represents the region exactly</li>
 *     <li>'eu-west-*' - Represents all regions that start with 'eu-west-'</li>
 *     <li>'eu-*' - Represents all regions that start with 'eu-'</li>
 *     <li>'*' - Represents all regions, i.e. a global scope</li>
 * </ul>
 * <p/>
 * Examples of invalid combinations:
 * <ul>
 *     <li>'us-*-1' - The wildcard must appear at the end.</li>
 *     <li>'eu-we*' - The wildcard must be its own segment</li>
 * </ul>
 */
@SdkInternalApi
public final class RegionScope implements ToCopyableBuilder<RegionScope.Builder, RegionScope> {

    public static final RegionScope GLOBAL;

    private static final Pattern REGION_SCOPE_PATTERN;

    //Pattern must be compiled when static scope is created
    static {
        REGION_SCOPE_PATTERN = Pattern.compile("^([a-z0-9-])*([*]?)$");
        GLOBAL = RegionScope.of("*");
    }

    private final String regionScope;

    private RegionScope(Builder b) {
        this.regionScope = Validate.paramNotBlank(b.regionScope, "regionScope");
        validateFormat(regionScope);
    }

    /**
     * Get a new builder for this class.
     *
     * @return A newly initialized instance of a builder.
     */
    public static Builder builder() {
        return new Builder();
    }


    /**
     * Gets the string representation of this region scope.
     */
    public String id() {
        return this.regionScope;
    }

    /**
     * Creates a RegionScope with the supplied value.
     *
     * @param value See class documentation {@link RegionScope} for allowed values.
     */
    public static RegionScope of(String value) {
        return builder().regionScope(value).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RegionScope that = (RegionScope) o;

        return regionScope.equals(that.regionScope);
    }

    @Override
    public int hashCode() {
        return 31 * (1 + (regionScope != null ? regionScope.hashCode() : 0));
    }

    @Override
    public Builder toBuilder() {
        return builder().regionScope(regionScope);
    }

    private void validateFormat(String regionScope) {
        Matcher matcher = REGION_SCOPE_PATTERN.matcher(regionScope);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Incorrect region scope '" + regionScope + "'. Region scope must be a"
                                               + " string that either is a complete region string, such as 'us-east-1',"
                                               + " or uses the wildcard '*' to represent any region that starts with"
                                               + " the preceding parts. Wildcards must appear as a separate segment after"
                                               + " a '-' dash, for example 'us-east-*'. A global scope of '*' is allowed.");
        }
        List<String> segments = Arrays.asList(regionScope.split("-"));
        String lastSegment = segments.get(segments.size() - 1);
        if (lastSegment.contains("*") && lastSegment.length() != 1) {
            throw new IllegalArgumentException("Incorrect region scope '" + regionScope
                                               + "'. A wildcard must only appear on its own at the end of the expression "
                                               + "after a '-' dash. A global scope of '*' is allowed.");
        }
    }

    public static final class Builder implements CopyableBuilder<Builder, RegionScope> {
        private String regionScope;

        private Builder() {
        }

        public void setRegionScope(String regionScope) {
            regionScope(regionScope);
        }

        public Builder regionScope(String regionScope) {
            this.regionScope = regionScope;
            return this;
        }

        @Override
        public RegionScope build() {
            return new RegionScope(this);
        }
    }
}
