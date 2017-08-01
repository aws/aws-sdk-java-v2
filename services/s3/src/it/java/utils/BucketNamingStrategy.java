/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package utils;


import static software.amazon.awssdk.utils.StringUtils.lowerCase;

/**
 * Naming strategy for S3 buckets that uses the test class name and the user name to come up with a unique but reproducible
 * bucket name.
 */
public class BucketNamingStrategy {

    public String getBucketName(Class<?> testClass) {
        return String.format("%s-%s", lowerCase(testClass.getSimpleName()), lowerCase(System.getProperty("user.name")));
    }
}
