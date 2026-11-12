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

package software.amazon.awssdk.s3.mockserver.server;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.s3.mockserver.helpers.ByteArrays;
import software.amazon.awssdk.s3.mockserver.helpers.CaseInsensitiveMap;
import software.amazon.awssdk.s3.mockserver.helpers.CaseInsensitiveTreeMap;

public final class CapturedBody {
    private final byte[] captured;
    private final long fullLength;
    private final CaseInsensitiveMap<List<String>> trailingHeaders;

    public CapturedBody(byte[] captured, long fullLength, CaseInsensitiveMap<List<String>> trailingHeaders) {
        this.captured = captured;
        this.fullLength = fullLength;
        this.trailingHeaders = trailingHeaders;
    }

    public byte[] captured() {
        return captured;
    }

    public long fullLength() {
        return fullLength;
    }

    public CaseInsensitiveMap<List<String>> trailingHeaders() {
        return trailingHeaders;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CapturedBody that = (CapturedBody) o;
        return captured == that.captured
            && fullLength == that.fullLength
            && Objects.equals(trailingHeaders, that.trailingHeaders);
    }

    @Override
    public int hashCode() {
        int result = 0;
        result = 31 * result + Objects.hashCode(captured);
        result = 31 * result + Long.hashCode(fullLength);
        result = 31 * result + Objects.hashCode(trailingHeaders);
        return result;
    }

    @Override
    public String toString() {
        return "CapturedBody[captured=" + ByteArrays.describe(captured) + ", fullLength=" + fullLength
                + ", trailingHeaders=" + trailingHeaders + "]";
    }

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
