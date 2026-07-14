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

package software.amazon.awssdk.http.crt;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * A TLS protocol version that {@link AwsCrtHttpClient} and {@link AwsCrtAsyncHttpClient} uses.
 *
 * @see AwsCrtHttpClient.Builder#minTlsVersion(TlsVersion)
 * @see AwsCrtAsyncHttpClient.Builder#minTlsVersion(TlsVersion)
 */
@SdkPublicApi
public enum TlsVersion {

    /**
     * TLS 1.3.
     */
    TLS_1_3,

    /**
     * The underlying OS/platform + CRT TLS default.
     */
    SYSTEM_DEFAULT
}
