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

package software.amazon.awssdk.s3.mockserver.testarchive;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public interface RowRepeater {

    void repeat(String row, Runnable iteration) throws Throwable;

    static RowRepeater installed() {
        return Installed.REPEATER;
    }

    static boolean isInstalled() {
        return Installed.REPEATER != null;
    }

    final class Installed {
        private static final RowRepeater REPEATER = resolve();

        private Installed() {
        }

        private static RowRepeater resolve() {
            List<RowRepeater> found = new ArrayList<>();
            ServiceLoader.load(RowRepeater.class).forEach(found::add);
            if (found.size() > 1) {
                throw new IllegalStateException("more than one RowRepeater is installed: " + found);
            }
            return found.isEmpty() ? null : found.get(0);
        }
    }
}
