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

package software.amazon.awssdk.core.exception;

import java.util.StringJoiner;
import software.amazon.awssdk.annotations.SdkProtectedApi;

@SdkProtectedApi
public class SdkDiagnostics {
    private final int numAttempts;

    //TODO: needs to use builder pattern
    public SdkDiagnostics(int numAttempts) {
        this.numAttempts = numAttempts;
    }

    @Override
    public String toString() {
        // TODO: the exact format needs to be discussed with the team
        StringJoiner details = new StringJoiner(", ", "(", ")");
        details.add("SDK Diagnotics: numAttempts = " + numAttempts);
        return details.toString();
    }
}
