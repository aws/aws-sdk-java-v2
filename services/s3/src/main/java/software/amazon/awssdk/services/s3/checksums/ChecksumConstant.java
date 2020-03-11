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

package software.amazon.awssdk.services.s3.checksums;

import software.amazon.awssdk.annotations.SdkInternalApi;

@SdkInternalApi
public final class ChecksumConstant {

    /**
     * Header name for the content-length of an S3 object.
     */
    public static final String CONTENT_LENGTH_HEADER = "Content-Length";

    /**
     * Header name for specifying S3 to send a trailing checksum.
     */
    public static final String ENABLE_CHECKSUM_REQUEST_HEADER = "x-amz-te";

    /**
     * Header name for specifying if trailing checksums were sent on an object.
     */
    public static final String CHECKSUM_ENABLED_RESPONSE_HEADER = "x-amz-transfer-encoding";

    /**
     * Header value for specifying MD5 as the trailing checksum of an object.
     */
    public static final String ENABLE_MD5_CHECKSUM_HEADER_VALUE = "append-md5";

    /**
     * Header value for specifying server side encryption.
     */
    public static final String SERVER_SIDE_ENCRYPTION_HEADER = "x-amz-server-side-encryption";

    /**
     * Header value for specifying server side encryption with a customer managed key.
     */
    public static final String SERVER_SIDE_CUSTOMER_ENCRYPTION_HEADER = "x-amz-server-side-encryption-customer-algorithm";

    /**
     * Length of an MD5 checksum in bytes.
     */
    public static final int S3_MD5_CHECKSUM_LENGTH = 16;

    private ChecksumConstant() {
    }
}
