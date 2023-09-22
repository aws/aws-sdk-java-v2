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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * This class represents the concept of a set of regions.
 * <p>
 * A region-set can contain one or more comma-separated AWS regions, or a single wildcard to represent all regions ("global").
 * Whitespace is trimmed from entries of the set.
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
 *     <li>'*, us-west-2' - A wildcard should be the only entry.</li>
 *     <li>'us-*-1,eu-west-1' - A wildcard must be its own item.</li>
 * </ul>
 */
@SdkPublicApi
public final class RegionSet {

    public static final RegionSet GLOBAL;

    private static final Pattern REGION_SET_PATTERN;

    static {
        REGION_SET_PATTERN = Pattern.compile("^(\\*|([a-zA-Z0-9-]+)(\\s*,\\s*[a-zA-Z0-9-]+)*)$");
        GLOBAL = create(Collections.singleton("*"));
    }

    private final Set<String> regionSet;
    private final String regionSetString;

    private RegionSet(Collection<String> regions) {
        Validate.paramNotNull(regions, "regions");
        regionSet = new HashSet<>(regions.size());
        fillRegionSet(regionSet, regions);
        this.regionSetString = String.join(",", regionSet);
        validateFormat(regionSetString);
    }

    private static void fillRegionSet(Set<String> regionSet, Collection<String> regions) {
        for (String region : regions) {
            String regionTrimmed = region.trim();
            if (regionSet.contains(regionTrimmed)) {
                throw new IllegalArgumentException("A region may not appear more than once!");
            }
            regionSet.add(regionTrimmed);
        }
    }

    /**
     * Gets the stringified identifier for this RegionSet.
     */
    public String id() {
        return regionSetString;
    }

    /**
     * Creates a RegionSet with the supplied region-set string.
     *
     * @param value See class documentation {@link RegionSet} for the expected format.
     */
    public static RegionSet create(String value) {
        return create(Arrays.asList(value.split(",", -1)));
    }

    /**
     * Creates a RegionSet from the supplied collection.
     *
     * @param regions A collection of regions.
     */
    public static RegionSet create(Collection<String> regions) {
        return new RegionSet(regions);
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

    private static void validateFormat(String regionSet) {
        Matcher matcher = REGION_SET_PATTERN.matcher(regionSet.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid region-set '" + regionSet + "'. The region-set must be one or more "
                                               + "complete regions, such as 'us-east-1' or 'us-west-2, us-east-1', or the "
                                               + "wildcard ('*') to represent all regions.");
        }
    }
}
