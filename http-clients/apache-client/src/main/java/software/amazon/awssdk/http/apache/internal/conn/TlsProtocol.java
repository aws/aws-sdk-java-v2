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

package software.amazon.awssdk.http.apache.internal.conn;

import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * TLS protocols arranged in descending order of security preference in terms of
 * their ordinal numbers. See <a href=
 * "http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#jssenames"
 * >JSSE Standard Names</a>.
 */
@SdkInternalApi
enum TlsProtocol {
    TLS_V1_2("TLSv1.2"),
    // most secure/preferred
    TLS_V1_1("TLSv1.1"),
    TLS_V1("TLSv1"),
    TLS("TLS"),
    // least secure/preferred, but acceptable
    ;
    private final String protocolName;

    TlsProtocol(String protocolName) {
        this.protocolName = protocolName;
    }

    /**
     * Returns the corresponding TLS protocol name as per the <a href=
     * "http://docs.oracle.com/javase/7/docs/technotes/guides/security/StandardNames.html#jssenames"
     * >JSSE Standard Names</a>
     */
    String getProtocolName() {
        return protocolName;
    }
}
