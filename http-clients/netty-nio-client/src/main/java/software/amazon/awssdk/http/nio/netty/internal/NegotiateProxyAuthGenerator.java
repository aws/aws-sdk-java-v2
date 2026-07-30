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

import com.sun.security.auth.module.Krb5LoginModule;
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
import software.amazon.awssdk.annotations.SdkTestInternalApi;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.nio.netty.ProxyAuthScheme;
import software.amazon.awssdk.utils.BinaryUtils;

/**
 * Auth generator for Kerberos. This does not login/authentication to Kerberos. It expects the ticket cache to be present and
 * simply reads that to generate the token.
 */
@SdkInternalApi
public class NegotiateProxyAuthGenerator implements ProxyAuthGenerator {
    private static final String OID = "1.3.6.1.5.5.2";
    private static final String SERVICE_NAME = "HTTP";
    private final Configuration config;

    public NegotiateProxyAuthGenerator() {
        this(createDefaultConfig());
    }

    @SdkTestInternalApi
    NegotiateProxyAuthGenerator(Configuration config) {
        this.config = config;
    }

    @Override
    public ProxyAuthScheme scheme() {
        return ProxyAuthScheme.NEGOTIATE;
    }

    @Override
    public String generateAuthParams(SdkHttpRequest request) {
        try {
            Subject subject = getSubject();

            byte[] token = Subject.doAs(subject, (PrivilegedExceptionAction<byte[]>) () -> {
                GSSContext ctx = createGSSContext(getManager(), request.getUri());
                ctx.requestMutualAuth(true);
                return ctx.initSecContext(new byte[0], 0, 0);
            });

            return BinaryUtils.toBase64(token);
        } catch (PrivilegedActionException e) {
            throw new RuntimeException("Unable to generate token", e);
        }
    }

    private Subject getSubject() {
        try {
            LoginContext loginContext = new LoginContext("dummy", null, null, config);
            loginContext.login();
            return loginContext.getSubject();
        } catch (LoginException e) {
            throw new RuntimeException("Unable to perform login", e);
        }
    }

    private GSSContext createGSSContext(GSSManager manager, URI endpoint) {
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
     * See javadoc for {@link Krb5LoginModule} for additional info on the configuration options.
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
