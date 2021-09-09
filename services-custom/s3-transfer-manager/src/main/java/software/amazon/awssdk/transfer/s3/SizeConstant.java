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

package software.amazon.awssdk.transfer.s3;


import software.amazon.awssdk.annotations.SdkPreviewApi;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Helpful constants for common size units.
 */
@SdkPublicApi
@SdkPreviewApi
public final class SizeConstant {

    /**
     * 1 Kibibyte
     * */
    public static final long KB = 1024;

    /**
     * 1 Mebibyte.
     */
    public static final long MB = 1024 * KB;

    /**
     * 1 Gibibyte.
     */
    public static final long GB = 1024 * MB;

    private SizeConstant() {

    }
}
