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

package software.amazon.awssdk.http.nio.netty;

import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Supported auth schemes for authentication with a proxy.
 */
@SdkPublicApi
public enum ProxyAuthScheme {
    /**
     * Basic authentication, as defined by <a href="https://datatracker.ietf.org/doc/html/rfc7617">RFC 7617</a>. Requires a
     * username and password.
     */
    BASIC("Basic"),

    /**
     * Kerberos authentication, using SPNEGO as defined by
     * <a href="https://datatracker.ietf.org/doc/html/rfc4559">RFC 4559</a>.
     * <p>
     * Credentials are read from the ambient Kerberos ticket cache. The client never prompts for a password and never reads a
     * keytab, so the environment must already hold a valid ticket-granting ticket, typically obtained by running
     * {@code kinit} and verifiable with {@code klist}. The cache location follows the usual Kerberos conventions, including
     * the {@code KRB5CCNAME} environment variable. Any username and password configured on the proxy are ignored.
     * <p>
     * Because the ticket is ambient state rather than configuration, a missing or expired ticket is not detected when the
     * client is built. It surfaces when a proxy connection is established, so it fails per-request, and an expiring ticket in
     * a long-lived process affects new connections while existing pooled connections continue to work until they are
     * replaced. Recovery requires refreshing the ticket out of band.
     * <p>
     * The service principal is derived from the configured proxy host as {@code HTTP/<host>}, so the host must be one the
     * Kerberos realm knows. A hostname normally resolves correctly, but an IP literal does not.
     * <p>
     * This scheme uses the JDK's GSS-API and JAAS Kerberos support, which live in the {@code java.security.jgss} and
     * {@code jdk.security.auth} modules. A custom runtime image built without them, or a GraalVM native image built without
     * the corresponding reflection configuration, fails when the first proxy connection is established. The login module is
     * the one shipped with OpenJDK class libraries, so the legacy IBM JDK, which substitutes its own, is not supported.
     */
    NEGOTIATE("Negotiate"),
    ;

    private final String value;

    ProxyAuthScheme(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
