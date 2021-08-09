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

package software.amazon.awssdk.protocol.asserts.marshalling;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;

/**
 * Assertion on the marshalled request.
 */
public abstract class MarshallingAssertion {

    /**
     * Asserts on the marshalled request.
     *
     * @param actual Marshalled request
     * @throws AssertionError If any assertions fail
     */
    public final void assertMatches(LoggedRequest actual) throws AssertionError {
        // Catches the exception to play nicer with lambda's
        try {
            doAssert(actual);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Hook to allow subclasses to perform their own assertion logic. Allows subclasses to throw
     * checked exceptions without propogating it back to the caller.
     */
    protected abstract void doAssert(LoggedRequest actual) throws Exception;
}
