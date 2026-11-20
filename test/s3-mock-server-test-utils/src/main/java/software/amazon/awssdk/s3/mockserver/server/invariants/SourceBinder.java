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

package software.amazon.awssdk.s3.mockserver.server.invariants;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.s3.mockserver.client.Payload;
import software.amazon.awssdk.s3.mockserver.helpers.Immutable;
import software.amazon.awssdk.s3.mockserver.server.CapturedRequest;

final class SourceBinder {

    static final int PREFIX = 64;

    private static final int MIN_PREFIX = 16;

    private static final int MEMO_ENTRIES = 4096;

    private static final Map<DigestKey, byte[]> MEMO = Collections.synchronizedMap(
            new LinkedHashMap<DigestKey, byte[]>(64, 0.75f, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(Map.Entry<DigestKey, byte[]> eldest) {
                    return size() > MEMO_ENTRIES;
                }
            });

    private final List<Payload> sources;

    SourceBinder(Collection<Payload> sources) {
        this.sources = Immutable.copyOfList(sources);
    }

    String mismatch(List<CapturedRequest> pieces) {
        long total = pieces.stream().mapToLong(CapturedRequest::fullBodyLength).sum();
        byte[] prefix = prefixOf(pieces);
        if (total < MIN_PREFIX) {
            for (Payload source : sources) {
                if (source.size() == total && Arrays.equals(source.toByteArray(), prefix)) {
                    return null;
                }
            }
            return total + " bytes that equal no declared source";
        }
        List<Payload> candidates = candidates(prefix);
        if (candidates.isEmpty()) {
            return total + " bytes whose first " + prefix.length + " bytes begin no declared source";
        }
        for (Payload candidate : candidates) {
            if (candidate.size() == total && firstDifferingPiece(pieces, candidate) < 0) {
                return null;
            }
        }
        Payload closest = candidates.stream().filter(c -> c.size() == total).findFirst().orElse(candidates.get(0));
        if (closest.size() != total) {
            return total + " bytes, but the source it begins as (" + closest.contentKey() + ") is "
                    + closest.size() + " bytes";
        }
        int piece = firstDifferingPiece(pieces, closest);
        long offset = 0;
        for (int i = 0; i < piece; i++) {
            offset += pieces.get(i).fullBodyLength();
        }
        return total + " bytes that begin as source " + closest.contentKey() + " but differ from it in"
                + (pieces.size() > 1 ? " part " + (piece + 1) + " of " + pieces.size() + "," : "")
                + " bytes " + offset + ".." + (offset + pieces.get(piece).fullBodyLength() - 1);
    }

    Payload wholeSource(CapturedRequest body) {
        if (body.fullBodyDigest() == null) {
            return null;
        }
        for (Payload candidate : candidates(prefixOf(Immutable.listOf(body)))) {
            if (candidate.size() == body.fullBodyLength()
                    && Arrays.equals(digest(candidate, 0, candidate.size()), body.fullBodyDigest())) {
                return candidate;
            }
        }
        return null;
    }

    private List<Payload> candidates(byte[] prefix) {
        List<Payload> found = new ArrayList<>();
        for (Payload source : sources) {
            if (source.size() >= prefix.length && Arrays.equals(source.toByteArray(0, prefix.length), prefix)) {
                found.add(source);
            }
        }
        return found;
    }

    private static int firstDifferingPiece(List<CapturedRequest> pieces, Payload source) {
        long offset = 0;
        for (int i = 0; i < pieces.size(); i++) {
            CapturedRequest piece = pieces.get(i);
            long length = piece.fullBodyLength();
            if (offset + length > source.size()
                    || !Arrays.equals(digest(source, (int) offset, (int) length), piece.fullBodyDigest())) {
                return i;
            }
            offset += length;
        }
        return -1;
    }

    private static byte[] prefixOf(List<CapturedRequest> pieces) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (CapturedRequest piece : pieces) {
            byte[] captured = WireInvariants.payloadPrefix(piece);
            int usable = (int) Math.min(captured.length, piece.fullBodyLength());
            out.write(captured, 0, Math.min(usable, PREFIX - out.size()));
            if (out.size() >= PREFIX || usable < piece.fullBodyLength()) {
                break;
            }
        }
        return out.toByteArray();
    }

    private static byte[] digest(Payload source, int offset, int length) {
        DigestKey key = new DigestKey(source.contentKey(), offset, length);
        byte[] known = MEMO.get(key);
        if (known != null) {
            return known;
        }
        byte[] computed = source.sha256(offset, length);
        MEMO.put(key, computed);
        return computed;
    }

    private static final class DigestKey {
        private final String content;
        private final int offset;
        private final int length;

        private DigestKey(String content, int offset, int length) {
            this.content = content;
            this.offset = offset;
            this.length = length;
        }

        public String content() {
            return content;
        }

        public int offset() {
            return offset;
        }

        public int length() {
            return length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DigestKey that = (DigestKey) o;
            return Objects.equals(content, that.content)
                    && offset == that.offset
                    && length == that.length;
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(content);
            result = 31 * result + Objects.hashCode(offset);
            result = 31 * result + Objects.hashCode(length);
            return result;
        }

        @Override
        public String toString() {
            return "DigestKey[content=" + content + ", offset=" + offset + ", length=" + length + "]";
        }
    }
}
