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
import java.util.Map;
import software.amazon.awssdk.s3.wiretap.helpers.CaseInsensitiveMap;
import software.amazon.awssdk.s3.wiretap.helpers.CaseInsensitiveTreeMap;

public record CapturedBody(
    byte[] captured,
    long fullLength,
    CaseInsensitiveMap<List<String>> trailingHeaders
) {

    public static CapturedBody of(byte[] captured) {
        byte[] bytes = captured == null ? new byte[0] : captured;
        return new CapturedBody(bytes, bytes.length, new CaseInsensitiveTreeMap<>());
    }

    public static CapturedBody of(byte[] captured, long fullLength,
                                  Map<String, List<String>> trailers) {
        CaseInsensitiveMap<List<String>> map = new CaseInsensitiveTreeMap<>();
        if (trailers != null) {
            map.putAll(trailers);
        }
        return new CapturedBody(captured == null ? new byte[0] : captured, fullLength, map);
    }

    public String trailer(String name) {
        List<String> values = trailingHeaders.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalStateException("Multiple trailing headers named " + name + ": " + values);
        }
        return values.get(0);
    }

    public boolean complete() {
        return captured.length == fullLength;
    }
}
