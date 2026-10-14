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

package software.amazon.awssdk.s3.wiretap.server;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface ExchangeManipulator {

    boolean matches(Request request);

    default Request manipulateRequestHead(Request request) {
        return request;
    }

    default Response manipulateResponseHead(Response response) {
        return response;
    }

    default InputStream manipulateRequestBody(Map<String, List<String>> headers, InputStream in) {
        return in;
    }

    default InputStream manipulateResponseBody(Map<String, List<String>> headers, InputStream in) {
        return in;
    }
}
