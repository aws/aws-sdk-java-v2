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

package software.amazon.awssdk.codegen.utils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.internal.Jackson;

public final class ModelLoaderUtils {

    private static final Logger log = LoggerFactory.getLogger(ModelLoaderUtils.class);

    private ModelLoaderUtils() {
    }

    public static <T> T loadModel(Class<T> clzz, File file) {
        try {
            return Jackson.load(clzz, file);
        } catch (IOException e) {
            log.error("Failed to read the configuration file {}", file.getAbsolutePath());
            throw new RuntimeException(e);
        }
    }

    public static <T> T loadModel(Class<T> clzz, File file, boolean failOnUnknownProperties) {
        try {
            return Jackson.load(clzz, file, failOnUnknownProperties);
        } catch (IOException e) {
            log.error("Failed to read the configuration file {}", file.getAbsolutePath());
            throw new RuntimeException(e);
        }
    }

    public static <T> Optional<T> loadOptionalModel(Class<T> clzz, File file) {
        if (!file.exists()) {
            return Optional.empty();
        }
        return Optional.of(loadModel(clzz, file));
    }

    public static <T> Optional<T> loadOptionalModel(Class<T> clzz, File file, boolean failOnUnknownProperties) {
        if (!file.exists()) {
            return Optional.empty();
        }
        return Optional.of(loadModel(clzz, file, failOnUnknownProperties));
    }
}
