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

package software.amazon.awssdk.core.internal.util;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import software.amazon.awssdk.annotations.SdkInternalApi;

/**
 * Checks whether a download destination is usable without sending a request.
 * <p>
 * These checks are advisory and not atomic: the exclusive open performed once the response arrives stays authoritative and
 * still rejects a destination that appeared in between. They only read the filesystem, so they are safe to run repeatedly.
 */
@SdkInternalApi
public final class FileDestinationPreflight {

    private FileDestinationPreflight() {
    }

    public static void validateCreateNew(Path path) throws FileAlreadyExistsException {
        // NOFOLLOW_LINKS because a dangling symlink is absent as far as a following check is concerned, yet an
        // exclusive create on that path still fails with EEXIST.
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new FileAlreadyExistsException(path.toString());
        }
    }
}
