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

import java.util.List;
import software.amazon.awssdk.s3.wiretap.helpers.CaseInsensitiveMap;

public record CapturedResponse(
    int statusCode,
    String statusText,
    CaseInsensitiveMap<List<String>> headers,
    byte[] body,
    byte[] decodedBody,
    long bodyTotalLength
) implements HttpResponseHead {
}
