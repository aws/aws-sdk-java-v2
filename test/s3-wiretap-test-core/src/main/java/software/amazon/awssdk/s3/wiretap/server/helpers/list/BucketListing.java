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

package software.amazon.awssdk.s3.wiretap.server.helpers.list;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.NavigableMap;
import java.util.TreeMap;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.StoredObject;

public final class BucketListing {

    static final Comparator<String> UTF8_ORDER = (a, b) -> {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        return Arrays.compareUnsigned(x, y);
    };

    private final NavigableMap<String, StoredObject> objects;

    private BucketListing(NavigableMap<String, StoredObject> objects) {
        this.objects = objects;
    }

    public static BucketListing empty() {
        return new BucketListing(new TreeMap<>(UTF8_ORDER));
    }

    public BucketListing with(String key, StoredObject object) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("an S3 key must be non-empty");
        }
        if (key.codePoints().anyMatch(cp -> cp >= Character.MIN_SURROGATE && cp <= Character.MAX_SURROGATE)) {
            throw new IllegalArgumentException("key holds an unpaired surrogate; it has no UTF-8 "
                    + "encoding, so it is not a key S3 could hold");
        }
        for (int i = 0; i < key.length(); i++) {
            char c = key.charAt(i);
            if (c < 0x20 || (c >= 0x7F && c <= 0x9F) || c == 0xFFFE || c == 0xFFFF
                    || c == '\'' || c == '"') {
                throw new IllegalArgumentException("key " + key.replaceAll("\\p{Cntrl}", "?")
                        + " holds a character whose rendering in listing XML has not been captured"
                        + " from S3 (a C0/C1 control, a non-XML character, ' or \"); the mock will not"
                        + " guess it");
            }
        }
        TreeMap<String, StoredObject> copy = new TreeMap<>(UTF8_ORDER);
        copy.putAll(objects);
        copy.put(key, object);
        return new BucketListing(copy);
    }

    public NavigableMap<String, StoredObject> objects() {
        return Collections.unmodifiableNavigableMap(objects);
    }

    public StoredObject object(String key) {
        StoredObject object = objects.get(key);
        if (object == null) {
            throw new IllegalArgumentException("the listing holds no key " + key + "; it holds "
                    + objects.keySet());
        }
        return object;
    }
}
