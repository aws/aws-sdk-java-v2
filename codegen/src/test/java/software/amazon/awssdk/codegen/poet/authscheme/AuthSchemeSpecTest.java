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

package software.amazon.awssdk.codegen.poet.authscheme;

import static org.hamcrest.MatcherAssert.assertThat;
import static software.amazon.awssdk.codegen.poet.PoetMatchers.generatesTo;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.codegen.poet.ClassSpec;
import software.amazon.awssdk.codegen.poet.ClientTestModels;

public class AuthSchemeSpecTest {

    @Test
    public void authSchemeParams() {
        ClassSpec authSchemeParams = new AuthSchemeParamsSpec(ClientTestModels.queryServiceModels());
        assertThat(authSchemeParams, generatesTo("auth-scheme-params.java"));
    }

    @Test
    public void authSchemeProvider() {
        ClassSpec authSchemeProvider = new AuthSchemeProviderSpec(ClientTestModels.queryServiceModels());
        assertThat(authSchemeProvider, generatesTo("auth-scheme-provider.java"));
    }

    @Test
    public void defaultAuthSchemeParams() {
        ClassSpec defaultAuthSchemeParams = new DefaultAuthSchemeParamsSpec(ClientTestModels.queryServiceModels());
        assertThat(defaultAuthSchemeParams, generatesTo("default-auth-scheme-params.java"));
    }

    @Test
    public void defaultAuthSchemeProvider() {
        ClassSpec defaultAuthSchemeProvider = new DefaultAuthSchemeProviderSpec(ClientTestModels.queryServiceModels());
        assertThat(defaultAuthSchemeProvider, generatesTo("default-auth-scheme-provider.java"));
    }
}
