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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.utils.Validate;

/**
 * This class represents the concept of a set of regions.
 * <p>
 * A region-set can contain one or more comma-separated AWS regions, or a single wildcard to represent all regions ("global").
 * Whitespace is trimmed from entries of the set.
 * <p>
 * Examples of region-sets:
 * <ul>
 *     <li>'*' - Represents all regions, global</li>
 *     <li>'eu-west-1' - Represents a single region, eu-west-1</li>
 *     <li>'us-west-2,us-east-1' - Represents 2 regions, us-west-2 and us-east-1</li>
 * </ul>
 */
@SdkPublicApi
@Immutable
public final class RegionSet {

    /**
     * The "Global" region, which is represented with a single wildcard character: "*".
     */
    public static final RegionSet GLOBAL;

    static {
        GLOBAL = create(Collections.singleton("*"));
    }

    private final Set<String> regionSet;
    private final String regionSetString;

    private RegionSet(Collection<String> regions) {
        this.regionSet = Collections.unmodifiableSet(new HashSet<>(regions));
        this.regionSetString = String.join(",", regionSet);
    }

    /**
     * Gets the string representation of this RegionSet.
     */
    public String asString() {
        return regionSetString;
    }

    /**
     * Gets the set of strings that represent this RegionSet.
     */
    public Set<String> asSet() {
        return regionSet;
    }

    /**
     * Creates a RegionSet with the supplied region-set string.
     *
     * @param value See class documentation {@link RegionSet} for the expected format.
     */
    public static RegionSet create(String value) {
        Validate.notEmpty(value, "value");
        return create(Arrays.asList(value.trim().split(",")));
    }

    /**
     * Creates a RegionSet from the supplied collection.
     *
     * @param regions A collection of regions.
     */
    public static RegionSet create(Collection<String> regions) {
        Validate.notEmpty(regions, "regions");
        return new RegionSet(
            regions.stream().map(s -> Validate.notEmpty(s, "region").trim()).collect(Collectors.toList())
        );
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
        return Objects.hashCode(regionSet);
    }
}
