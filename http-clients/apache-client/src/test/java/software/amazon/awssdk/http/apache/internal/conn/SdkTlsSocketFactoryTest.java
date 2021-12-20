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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import org.junit.jupiter.api.Test;

public class SdkTlsSocketFactoryTest {
    /**
     * Test when the edge case when the both supported and enabled protocols are null.
     */
    @Test
    public void preparedSocket_NullProtocols() throws Exception {
        SdkTlsSocketFactory f = new SdkTlsSocketFactory(SSLContext.getDefault(), null);
        try (SSLSocket socket = new TestSSLSocket() {
            @Override
            public String[] getSupportedProtocols() {
                return null;
            }

            @Override
            public String[] getEnabledProtocols() {
                return null;
            }

            @Override
            public void setEnabledProtocols(String[] protocols) {
                fail();
            }
        }) {
            f.prepareSocket(socket);
        }
    }

    @Test
    public void typical() throws Exception {
        SdkTlsSocketFactory f = new SdkTlsSocketFactory(SSLContext.getDefault(), null);
        try (SSLSocket socket = new TestSSLSocket() {
            @Override
            public String[] getSupportedProtocols() {
                return shuffle(new String[] {"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"});
            }

            @Override
            public String[] getEnabledProtocols() {
                return shuffle(new String[] {"SSLv3", "TLSv1"});
            }

            @Override
            public void setEnabledProtocols(String[] protocols) {
                assertTrue(Arrays.equals(protocols, new String[] {"TLSv1.2", "TLSv1.1", "TLSv1", "SSLv3"}));
            }
        }) {
            f.prepareSocket(socket);
        }
    }

    @Test
    public void noTLS() throws Exception {
        SdkTlsSocketFactory f = new SdkTlsSocketFactory(SSLContext.getDefault(), null);
        try (SSLSocket socket = new TestSSLSocket() {
            @Override
            public String[] getSupportedProtocols() {
                return shuffle(new String[] {"SSLv2Hello", "SSLv3"});
            }

            @Override
            public String[] getEnabledProtocols() {
                return new String[] {"SSLv3"};
            }

            @Override
            public void setEnabledProtocols(String[] protocols) {
                // For backward compatibility
                assertTrue(Arrays.equals(protocols, new String[] {"SSLv3"}));
            }
        }) {
            f.prepareSocket(socket);
        }
    }

    @Test
    public void notIdeal() throws Exception {
        SdkTlsSocketFactory f = new SdkTlsSocketFactory(SSLContext.getDefault(), null);
        try (SSLSocket socket = new TestSSLSocket() {
            @Override
            public String[] getSupportedProtocols() {
                return shuffle(new String[] {"SSLv2Hello", "SSLv3", "TLSv1", "TLSv1.1"});
            }

            @Override
            public String[] getEnabledProtocols() {
                return shuffle(new String[] {"SSLv3", "TLSv1"});
            }

            @Override
            public void setEnabledProtocols(String[] protocols) {
                assertTrue(Arrays.equals(protocols, new String[] {"TLSv1.1", "TLSv1", "SSLv3"}));
            }
        }) {
            f.prepareSocket(socket);
        }
    }

    private String[] shuffle(String[] in) {
        List<String> list = new ArrayList<String>(Arrays.asList(in));
        Collections.shuffle(list);
        return list.toArray(new String[0]);
    }
}
