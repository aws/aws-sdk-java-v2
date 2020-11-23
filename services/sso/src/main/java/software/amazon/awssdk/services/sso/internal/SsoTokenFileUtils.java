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

package software.amazon.awssdk.services.sso.internal;

import static software.amazon.awssdk.utils.UserHomeDirectoryUtils.userHomeDirectory;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.Validate;

/**
 * A tool class helps generating the path of cached token file.
 */
@SdkInternalApi
public class SsoTokenFileUtils {

    private static final Pattern HOME_DIRECTORY_PATTERN =
        Pattern.compile("^~(/|" + Pattern.quote(FileSystems.getDefault().getSeparator()) + ").*$");

    private SsoTokenFileUtils() {

    }

    /**
     * Generate the cached file name by generating the SHA1 Hex Digest of the UTF-8 encoded start url bytes.
     */
    public static Path generateCachedTokenPath(String startUrl, String tokenDirectory) {
        Validate.notNull(startUrl, "The start url shouldn't be null.");
        byte[] startUrlBytes = startUrl.getBytes(StandardCharsets.UTF_8);
        String encodedUrl = new String(startUrlBytes, StandardCharsets.UTF_8);
        return resolveProfileFilePath(Paths.get(tokenDirectory, sha1Hex(encodedUrl) + ".json").toString());
    }

    /**
     * Use {@link MessageDigest} instance to encrypt the input String.
     */
    private static String sha1Hex(String input) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-1");
            md.update(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw SdkClientException.builder().message("Unable to use \"SHA-1\" algorithm.").cause(e).build();
        }

        return BinaryUtils.toHex(md.digest());
    }

    private static Path resolveProfileFilePath(String path) {
        // Resolve ~ using the CLI's logic, not whatever Java decides to do with it.
        if (HOME_DIRECTORY_PATTERN.matcher(path).matches()) {
            path = userHomeDirectory() + path.substring(1);
        }

        return Paths.get(path);
    }
}
