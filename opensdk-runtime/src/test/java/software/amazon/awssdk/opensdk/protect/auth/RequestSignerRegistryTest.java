/*
 * Copyright 2011-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package software.amazon.awssdk.opensdk.protect.auth;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;
import org.junit.Test;
import software.amazon.awssdk.auth.RequestSigner;

public class RequestSignerRegistryTest {

    @Test
    public void canExplicitlyRegisterSignerOfType() {
        FooCustomRequestSigner theOneWeWant = mock(FooCustomRequestSigner.class);
        RequestSignerRegistry sut = new RequestSignerRegistry().register(theOneWeWant, RequestSigner.class);

        Optional<RequestSigner> result = sut.getSigner(RequestSigner.class);
        assertThat(result, equalTo(Optional.of(theOneWeWant)));
    }

    interface FooCustomRequestSigner extends RequestSigner {

    }
}
