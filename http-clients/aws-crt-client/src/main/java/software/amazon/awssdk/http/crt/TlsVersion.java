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
 * <p><b>Mac-Only TLS Behavior:</b> the default CRT TLS backend on macOS (Apple Secure Transport) does not
 * support TLS 1.3. To use {@link TlsVersion#TLS_1_3} on macOS you must set the environment variable
 * {@code AWS_CRT_USE_NON_FIPS_TLS_13} to any non-empty value at process startup so the CRT selects its s2n-tls backend. See
 * <a href="https://github.com/awslabs/aws-crt-java/blob/main/README.md">Mac-Only TLS Behavior</a> for more details.
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
