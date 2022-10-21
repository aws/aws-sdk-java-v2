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

package software.amazon.awssdk.core.rules.testing;

import java.util.function.Supplier;
import software.amazon.awssdk.core.rules.testing.model.Expect;
import software.amazon.awssdk.endpoints.Endpoint;

public final class EndpointProviderTestCase {
    private Supplier<Endpoint> testMethod;
    private Expect expect;

    public EndpointProviderTestCase(Supplier<Endpoint> testMethod, Expect expect) {
        this.testMethod = testMethod;
        this.expect = expect;
    }

    public Supplier<Endpoint> getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(Supplier<Endpoint> testMethod) {
        this.testMethod = testMethod;
    }

    public Expect getExpect() {
        return expect;
    }

    public void setExpect(Expect expect) {
        this.expect = expect;
    }
}
