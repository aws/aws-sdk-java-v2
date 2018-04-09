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

package software.amazon.awssdk.testutils.service;

import static software.amazon.awssdk.utils.JavaSystemSetting.USER_NAME;
import static software.amazon.awssdk.utils.StringUtils.lowerCase;

import java.util.Random;

public final class S3BucketUtils {
    private static final Random RANDOM = new Random();
    private static final int MAX_BUCKET_NAME_LENGTH = 63;

    private S3BucketUtils() { }

    /**
     * Creates a temporary bucket name using the class name of the calling class as a prefix.
     *
     * @return an s3 bucket name
     */
    public static String temporaryBucketName() {
        String callingClass = Thread.currentThread().getStackTrace()[2].getClassName();
        return temporaryBucketName(shortenClassName(callingClass.substring(callingClass.lastIndexOf('.'))));
    }

    /**
     * Creates a temporary bucket name using the class name of the object passed as a prefix.
     *
     * @param clz an object who's class will be used as the prefix
     * @return an s3 bucket name
     */
    public static String temporaryBucketName(Object clz) {
        return temporaryBucketName(clz.getClass());
    }

    /**
     * Creates a temporary bucket name using the class name as a prefix.
     *
     * @param clz class to use as the prefix
     * @return an s3 bucket name
     */
    public static String temporaryBucketName(Class<?> clz) {
        return temporaryBucketName(shortenClassName(clz.getSimpleName()));
    }

    /**
     * Creates a temporary bucket name using the prefix passed.
     *
     * @param prefix prefix to use for the bucket name
     * @return an s3 bucket name
     */
    public static String temporaryBucketName(String prefix) {
        String bucketName = lowerCase(prefix) + "-" + lowerCase(USER_NAME.getStringValueOrThrow()) + "-" + RANDOM.nextInt(10000);
        if (bucketName.length() > 63) {
            throw new RuntimeException("S3 buckets can only be 63 chars in length, try a shorter prefix");
        }
        return bucketName;
    }

    private static String shortenClassName(String clzName) {
        return clzName.length() <= 45 ? clzName : clzName.substring(0, 45);
    }
}
