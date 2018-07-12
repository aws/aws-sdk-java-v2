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

package software.amazon.awssdk.awscore.util;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.ReviewBeforeRelease;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.regions.Region;

@SdkProtectedApi
@ReviewBeforeRelease("This isn't used for as many services as it supports. Can we simplify or remove it?")
public final class AwsHostNameUtils {

    private static final Pattern S3_ENDPOINT_PATTERN =
            Pattern.compile("^(?:.+\\.)?s3[.-]([a-z0-9-]+)$");

    private static final Pattern STANDARD_CLOUDSEARCH_ENDPOINT_PATTERN =
            Pattern.compile("^(?:.+\\.)?([a-z0-9-]+)\\.cloudsearch$");

    private static final Pattern EXTENDED_CLOUDSEARCH_ENDPOINT_PATTERN =
            Pattern.compile("^(?:.+\\.)?([a-z0-9-]+)\\.cloudsearch\\..+");

    private AwsHostNameUtils() {
    }

    /**
     * Attempts to parse the region name from an endpoint based on conventions
     * about the endpoint format.
     *
     * @param host         the hostname to parse
     * @param serviceHint  an optional hint about the service for the endpoint
     * @return the region parsed from the hostname, or
     *                     null if no region information could be found.
     */
    public static Optional<Region> parseSigningRegion(final String host, final String serviceHint) {

        if (host == null) {
            throw new IllegalArgumentException("hostname cannot be null");
        }

        if (host.endsWith(".amazonaws.com")) {
            int index = host.length() - ".amazonaws.com".length();
            return parseStandardRegionName(host.substring(0, index));
        }

        if (serviceHint != null) {
            if (serviceHint.equals("cloudsearch") && !host.startsWith("cloudsearch.")) {
                // CloudSearch domains use the nonstandard domain format
                // [domain].[region].cloudsearch.[suffix].

                Matcher matcher = EXTENDED_CLOUDSEARCH_ENDPOINT_PATTERN.matcher(host);
                if (matcher.matches()) {
                    return Optional.of(Region.of(matcher.group(1)));
                }
            }

            // If we have a service hint, look for 'service.[region]' or
            // 'service-[region]' in the endpoint's hostname.
            Pattern pattern = Pattern.compile("^(?:.+\\.)?" + Pattern.quote(serviceHint) + "[.-]([a-z0-9-]+)\\.");

            Matcher matcher = pattern.matcher(host);
            if (matcher.find()) {
                return Optional.of(Region.of(matcher.group(1)));
            }
        }

        // Endpoint is non-standard
        return Optional.empty();
    }

    /**
     * Parses the region name from a standard (*.amazonaws.com) endpoint.
     *
     * @param fragment  the portion of the endpoint excluding
     *                  &quot;.amazonaws.com&quot;
     * @return the parsed region name (or &quot;us-east-1&quot; as a
     *                  best guess if we can't tell for sure)
     */
    private static Optional<Region> parseStandardRegionName(final String fragment) {
        Matcher matcher = S3_ENDPOINT_PATTERN.matcher(fragment);
        if (matcher.matches()) {
            // host was 'bucket.s3-[region].amazonaws.com'.
            return Optional.of(Region.of(matcher.group(1)));
        }

        matcher = STANDARD_CLOUDSEARCH_ENDPOINT_PATTERN.matcher(fragment);
        if (matcher.matches()) {
            // host was 'domain.[region].cloudsearch.amazonaws.com'.
            return Optional.of(Region.of(matcher.group(1)));
        }

        int index = fragment.lastIndexOf('.');
        if (index == -1) {
            // host was 'service.amazonaws.com', assume they're talking about us-east-1.
            return Optional.of(Region.US_EAST_1);
        }

        // host was 'service.[region].amazonaws.com'.
        String region = fragment.substring(index + 1);

        // Special case for iam.us-gov.amazonaws.com, which is actually
        // us-gov-west-1.
        if ("us-gov".equals(region)) {
            region = "us-gov-west-1";
        }

        if ("s3".equals(region)) {
            region = fragment.substring(index + 4);
        }

        return Optional.of(Region.of(region));
    }
}
