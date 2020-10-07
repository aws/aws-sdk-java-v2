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

package software.amazon.awssdk.http;

import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * Constants for commonly used HTTP headers.
 */
@SdkProtectedApi
public final class Header {

    public static final String CONTENT_LENGTH = "Content-Length";

    public static final String CONTENT_TYPE = "Content-Type";

    public static final String CONTENT_MD5 = "Content-MD5";

    public static final String TRANSFER_ENCODING = "Transfer-Encoding";

    public static final String CHUNKED = "chunked";

    public static final String HOST = "Host";

    public static final String CONNECTION = "Connection";

    public static final String KEEP_ALIVE_VALUE = "keep-alive";

    private Header() {
    }

}
