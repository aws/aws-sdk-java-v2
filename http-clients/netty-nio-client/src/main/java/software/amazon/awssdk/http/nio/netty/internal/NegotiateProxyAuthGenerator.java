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

package software.amazon.awssdk.http.nio.netty.internal;

import java.net.URI;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import org.ietf.jgss.GSSContext;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.nio.netty.ProxyAuthScheme;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Auth generator for Kerberos. This does not login/authentication to Kerberos. It expects the ticket cache to be present and
 * simply reads that to generate the token.
 */
@SdkInternalApi
public class NegotiateProxyAuthGenerator implements ProxyAuthGenerator {
    // SPNEGO pseudo-mechanism OID. Lets the proxy negotiate Kerberos over HTTP "Negotiate".
    // See https://www.ietf.org/rfc/rfc4178.txt for more info
    private static final String OID = "1.3.6.1.5.5.2";
    private static final String SERVICE_NAME = "HTTP";
    private final Configuration config;

    public NegotiateProxyAuthGenerator() {
        this(createDefaultConfig());
    }

    public NegotiateProxyAuthGenerator(Configuration config) {
        if (config != null) {
            this.config = config;
        } else {
            this.config = createDefaultConfig();
        }
    }

    @Override
    public ProxyAuthScheme scheme() {
        return ProxyAuthScheme.NEGOTIATE;
    }

    @Override
    public String generateAuthParams(URI proxyEndpoint) {
        try {
            Subject subject = getSubject();

            byte[] token = Subject.doAs(subject, (PrivilegedExceptionAction<byte[]>) () -> {
                GSSContext ctx = createGssContext(getManager(), proxyEndpoint);
                try {
                    return ctx.initSecContext(new byte[0], 0, 0);
                } finally {
                    ctx.dispose();
                }
            });

            return BinaryUtils.toBase64(token);
        } catch (PrivilegedActionException e) {
            throw new RuntimeException(String.format("Unable to generate SPNEGO token for Negotiate proxy authentication "
                                                     + "with '%s@%s'. This can happen when a service ticket for the proxy "
                                                     + "cannot be obtained from the KDC, e.g. because the ticket-granting "
                                                     + "ticket has expired (renew with 'kinit') or the proxy host does not "
                                                     + "match its Kerberos service principal name.",
                                                     SERVICE_NAME, proxyEndpoint.getHost()), e);
        }
    }

    private Subject getSubject() {
        try {
            LoginContext loginContext = new LoginContext("dummy", null, null, config);
            loginContext.login();
            return loginContext.getSubject();
        } catch (LoginException e) {
            throw new RuntimeException("Unable to perform Kerberos login for Negotiate proxy authentication. This "
                                       + "typically means the Kerberos ticket cache is missing, expired, or not readable. "
                                       + "Ensure a valid ticket-granting ticket exists (e.g., by running 'kinit'), and that "
                                       + "the cache is at the expected location (see the KRB5CCNAME environment variable). "
                                       + "Verify with 'klist'.", e);
        }
    }

    private GSSContext createGssContext(GSSManager manager, URI endpoint) {
        try {
            String name = String.format("%s@%s", SERVICE_NAME, endpoint.getHost());
            GSSName serverName = manager.createName(name, GSSName.NT_HOSTBASED_SERVICE);
            Oid spnegoOid = new Oid(OID);
            return manager.createContext(serverName, spnegoOid, null,
                                         GSSContext.DEFAULT_LIFETIME);
        } catch (GSSException e) {
            throw new RuntimeException("Unable to create GSSContext", e);
        }
    }

    private static GSSManager getManager() {
        return GSSManager.getInstance();
    }

    /**
     * Create a generic {@link Configuration} that instructs the Kerberos login module to simply look in the ticket cache, and
     * not to prompt for passwords.
     * <p>
     * See javadoc for {@code com.sun.security.auth.module.Krb5LoginModule} for additional info on the configuration options.
     */
    private static Configuration createDefaultConfig() {
        return new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                Map<String, String> opts = new HashMap<>();
                opts.put("useTicketCache", "true");
                opts.put("doNotPrompt", "true");
                return new AppConfigurationEntry[] {
                    new AppConfigurationEntry(
                        "com.sun.security.auth.module.Krb5LoginModule",
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, opts)
                };
            }
        };
    }
}
