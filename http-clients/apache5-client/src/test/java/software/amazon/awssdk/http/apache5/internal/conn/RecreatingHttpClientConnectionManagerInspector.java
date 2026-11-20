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

package software.amazon.awssdk.http.apache5.internal.conn;

/**
 * Exposes the package-private test hooks of {@link RecreatingHttpClientConnectionManager} to tests in other packages,
 * so that they can assert on its state rather than on its log output.
 */
public final class RecreatingHttpClientConnectionManagerInspector {

    private RecreatingHttpClientConnectionManagerInspector() {
    }

    /**
     * @return how many times the manager has rebuilt its connection pool
     */
    public static int recreationCount(RecreatingHttpClientConnectionManager connectionManager) {
        return connectionManager.recreationCount();
    }
}
