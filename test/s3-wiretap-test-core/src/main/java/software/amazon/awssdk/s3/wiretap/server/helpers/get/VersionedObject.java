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

package software.amazon.awssdk.s3.wiretap.server.helpers.get;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import software.amazon.awssdk.s3.wiretap.client.Payload;

public final class VersionedObject {

    private final List<StoredObject> versions;

    private VersionedObject(List<StoredObject> versions) {
        this.versions = List.copyOf(versions);
    }

    public static VersionedObject of(List<StoredObject> oldestFirst) {
        if (oldestFirst == null || oldestFirst.isEmpty()) {
            throw new IllegalArgumentException("a version stack needs at least one version");
        }
        Set<String> seen = new LinkedHashSet<>();
        for (StoredObject version : oldestFirst) {
            String id = version.versionId();
            if (id == null) {
                throw new IllegalArgumentException("every version in the stack must carry a version"
                        + " id (see StoredObject.withVersionId); this one has none: "
                        + version.describe());
            }
            if (!seen.add(id)) {
                throw new IllegalArgumentException("duplicate version id " + id + " in the stack —"
                        + " version(id) would be ambiguous, so a test reading the older version"
                        + " could be served the newer one");
            }
        }
        return new VersionedObject(oldestFirst);
    }

    public static VersionedObject of(StoredObject... oldestFirst) {
        return of(List.of(oldestFirst));
    }

    public static VersionedObject withSyntheticVersionIds(StoredObject... oldestFirst) {
        List<StoredObject> stamped = new ArrayList<>(oldestFirst.length);
        for (int i = 0; i < oldestFirst.length; i++) {
            stamped.add(oldestFirst[i].withVersionId(syntheticVersionId(oldestFirst[i], i + 1)));
        }
        return of(stamped);
    }

    private static String syntheticVersionId(StoredObject object, int ordinal) {
        String seed = object.eTag() + "#" + ordinal;
        byte[] digest = Payload.of(seed.getBytes(StandardCharsets.UTF_8)).md5Bytes();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    public StoredObject current() {
        return versions.get(versions.size() - 1);
    }

    public StoredObject version(String versionId) {
        for (StoredObject version : versions) {
            if (version.versionId().equals(versionId)) {
                return version;
            }
        }
        return null;
    }

    public List<StoredObject> versions() {
        return versions;
    }

    public List<String> versionIds() {
        return versions.stream().map(StoredObject::versionId).toList();
    }

    public int versionCount() {
        return versions.size();
    }

    public String describe() {
        StringBuilder sb = new StringBuilder(versionCount() + " version(s)");
        for (int i = 0; i < versions.size(); i++) {
            sb.append("\n  [").append(i + 1).append(i == versions.size() - 1 ? "/current] " : "] ")
                    .append(versions.get(i).describe());
        }
        return sb.toString();
    }
}
