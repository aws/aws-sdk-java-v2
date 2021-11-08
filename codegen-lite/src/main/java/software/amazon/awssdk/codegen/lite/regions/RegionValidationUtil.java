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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.codegen.lite.regions.model.Endpoint;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

@SdkInternalApi
public final class RegionValidationUtil {
    private static final Set<String> DEPRECATED_REGIONS_ALLOWSLIST = new HashSet<>();

    private static final String FIPS_SUFFIX = "-fips";

    private static final String FIPS_PREFIX = "fips-";

    static {
        try (InputStream allowListStream = RegionValidationUtil.class.getResourceAsStream("/software/amazon/awssdk/codegen/lite"
                                                                                          + "/DeprecatedRegionsAllowlist.txt")) {
            Validate.notNull(allowListStream, "Failed to load deprecated regions allowlist.");

            try (BufferedReader br = new BufferedReader(new InputStreamReader(allowListStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    DEPRECATED_REGIONS_ALLOWSLIST.add(StringUtils.trim(line));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

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

    public static boolean validEndpoint(String region, Endpoint endpoint) {
        boolean invalidEndpoint =
            Boolean.TRUE.equals(endpoint.getDeprecated()) && !DEPRECATED_REGIONS_ALLOWSLIST.contains(region);
        return !invalidEndpoint;
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
