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

package software.amazon.awssdk.services.s3.internal;

import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Utilities for working with Amazon S3 bucket names, such as validation and
 * checked to see if they are compatible with DNS addressing.
 */
@SdkProtectedApi
public class BucketUtils {

    private static final int MIN_BUCKET_NAME_LENGTH = 3;
    private static final int MAX_BUCKET_NAME_LENGTH = 63;

    private static final Pattern IP_ADDRESS_PATTERN = Pattern.compile("(\\d+\\.){3}\\d+");

    private BucketUtils() {
    }

    /**
     * Validates that the specified bucket name is valid for Amazon S3 V2 naming
     * (i.e. DNS addressable in virtual host style). Throws an
     * IllegalArgumentException if the bucket name is not valid.
     * <p>
     * S3 bucket naming guidelines are specified in
     * <a href="http://docs.amazonwebservices.com/AmazonS3/latest/dev/index.html?BucketRestrictions.html"
     * > http://docs.amazonwebservices.com/AmazonS3/latest/dev/index.html?
     * BucketRestrictions.html</a>
     *
     * @param bucketName The bucket name to validate.
     * @throws IllegalArgumentException If the specified bucket name doesn't follow Amazon S3's
     * guidelines.
     */
    public static boolean isValidDnsBucketName(final String bucketName,
                                               final boolean throwOnError) {

        if (bucketName == null) {
            return exception(throwOnError, "Bucket name cannot be null");
        }

        if (bucketName.length() < MIN_BUCKET_NAME_LENGTH ||
            bucketName.length() > MAX_BUCKET_NAME_LENGTH) {

            return exception(
                throwOnError,
                "Bucket name should be between " + MIN_BUCKET_NAME_LENGTH + " and " + MAX_BUCKET_NAME_LENGTH + " characters long"
            );
        }

        if (IP_ADDRESS_PATTERN.matcher(bucketName).matches()) {
            return exception(
                throwOnError,
                "Bucket name must not be formatted as an IP Address"
            );
        }

        char previous = '\0';

        for (int i = 0; i < bucketName.length(); ++i) {
            char next = bucketName.charAt(i);

            if (next >= 'A' && next <= 'Z') {
                return exception(
                    throwOnError,
                    "Bucket name should not contain uppercase characters"
                );
            }

            if (next == ' ' || next == '\t' || next == '\r' || next == '\n') {
                return exception(
                    throwOnError,
                    "Bucket name should not contain white space"
                );
            }

            if (next == '.') {
                if (previous == '\0') {
                    return exception(
                        throwOnError,
                        "Bucket name should not begin with a period"
                    );
                }
                if (previous == '.') {
                    return exception(
                        throwOnError,
                        "Bucket name should not contain two adjacent periods"
                    );
                }
                if (previous == '-') {
                    return exception(
                        throwOnError,
                        "Bucket name should not contain dashes next to periods"
                    );
                }
            } else if (next == '-') {
                if (previous == '.') {
                    return exception(
                        throwOnError,
                        "Bucket name should not contain dashes next to periods"
                    );
                }
                if (previous == '\0') {
                    return exception(
                        throwOnError,
                        "Bucket name should not begin with a '-'"
                    );
                }
            } else if ((next < '0')
                       || (next > '9' && next < 'a')
                       || (next > 'z')) {

                return exception(
                    throwOnError,
                    "Bucket name should not contain '" + next + "'"
                );
            }

            previous = next;
        }

        if (previous == '.' || previous == '-') {
            return exception(
                throwOnError,
                "Bucket name should not end with '-' or '.'"
            );
        }

        return true;
    }

    /**
     * Validates if the given bucket name follows naming guidelines that are acceptable for using
     * virtual host style addressing.
     *
     * @param bucketName The bucket name to validate.
     * @param throwOnError boolean to decide if an error should be thrown if the bucket name doesn't follow the naming convention
     */
    public static boolean isVirtualAddressingCompatibleBucketName(final String bucketName,
                                                                  final boolean throwOnError) {
        return isValidDnsBucketName(bucketName, throwOnError) && !bucketName.contains(".");
    }

    /**
     * If 'exception' is true, throw an IllegalArgumentException with the given
     * message. Otherwise, silently return false.
     *
     * @param exception true to throw an exception
     * @param message the message for the exception
     * @return false if 'exception' is false
     */
    private static boolean exception(final boolean exception, final String message) {
        if (exception) {
            throw new IllegalArgumentException(message);
        }
        return false;
    }
}