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

package software.amazon.awssdk.http.auth.aws.signer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * This class represents the concept of a set of regions.
 * <p>
 * A region-set can contain one or more comma-separated AWS regions, or a single wildcard to represent all regions ("global").
 * <p>
 * Examples of a valid region-set:
 * <ul>
 *     <li>'*' - Represents all regions, global</li>
 *     <li>'eu-west-1' - Represents a single region, eu-west-1</li>
 *     <li>'us-west-2,us-east-1' - Represents 2 regions, us-west-2 and us-east-1</li>
 * </ul>
 * <p>
 * Example of an invalid region set:
 * <ul>
 *     <li>'us-*-1,eu-west-1' - A wildcard must be its own item.</li>
 * </ul>
 */
@SdkPublicApi
public final class RegionSet {

    public static final RegionSet GLOBAL;

    private static final Pattern REGION_SCOPE_PATTERN;

    static {
        REGION_SCOPE_PATTERN = Pattern.compile("^(\\*|([a-zA-Z0-9-]+)(\\s*,\\s*[a-zA-Z0-9-]+)*)$");
        GLOBAL = create("*");
    }

    private final String regionSet;

    private RegionSet(String regionSet) {
        this.regionSet = Validate.paramNotBlank(regionSet, "regionSet");
        validateFormat(regionSet);
    }

    /**
     * Gets the string representation of this RegionSet.
     */
    public String id() {
        return this.regionSet;
    }

    /**
     * Creates a RegionSet with the supplied value.
     *
     * @param value See class documentation {@link RegionSet} for the expected format.
     */
    public static RegionSet create(String value) {
        return new RegionSet(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RegionSet that = (RegionSet) o;

        return regionSet.equals(that.regionSet);
    }

    @Override
    public int hashCode() {
        return 31 * (1 + (regionSet != null ? regionSet.hashCode() : 0));
    }

    private void validateFormat(String regionSet) {
        Matcher matcher = REGION_SCOPE_PATTERN.matcher(regionSet.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid region-set '" + regionSet + "'. The region-set must be one or more "
                                               + "complete regions, such as 'us-east-1' or 'us-west-2, us-east-1', or the " 
                                               + "wildcard ('*') to represent all regions.");
        }
    }
}
