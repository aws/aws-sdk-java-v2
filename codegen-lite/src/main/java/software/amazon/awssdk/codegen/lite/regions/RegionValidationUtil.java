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

package software.amazon.awssdk.codegen.lite.regions;

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class RegionValidationUtil {

    private static final String FIPS_SUFFIX = "-fips";

    private static final String FIPS_PREFIX = "fips-";

    private RegionValidationUtil() {
    }

    /**
     * Determines if a given region string is a "valid" AWS region.
     *
     * The region string must either match the partition regex, end with fips
     * and match the partition regex with that included, or include the word "global".
     *
     * @param regex - Regex for regions in a given partition.
     * @param region - Region string being checked.
     * @return true if the region string should be included as a region.
     */
    public static boolean validRegion(String region, String regex) {
        return matchesRegex(region, regex) ||
               matchesRegexFipsSuffix(region, regex) ||
               matchesRegexFipsPrefix(region, regex) ||
               isGlobal(region);
    }

    private static boolean matchesRegex(String region, String regex) {
        return region.matches(regex);
    }

    private static boolean matchesRegexFipsSuffix(String region, String regex) {
        return region.replace(FIPS_SUFFIX, "").matches(regex);
    }

    private static boolean matchesRegexFipsPrefix(String region, String regex) {
        return region.replace(FIPS_PREFIX, "").matches(regex);
    }

    private static boolean isGlobal(String region) {
        return region.contains("global");
    }

}
