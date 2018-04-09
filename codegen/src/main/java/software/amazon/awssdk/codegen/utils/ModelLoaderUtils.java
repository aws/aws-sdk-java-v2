/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.io.InputStream;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.codegen.internal.Jackson;
import software.amazon.awssdk.codegen.internal.Utils;
import software.amazon.awssdk.codegen.model.service.ServiceModel;

public final class ModelLoaderUtils {

    public static final Logger log = LoggerFactory.getLogger(ModelLoaderUtils.class);

    private ModelLoaderUtils() {
    }

    public static ServiceModel loadModel(String modelLocation) {
        return loadConfigurationModel(ServiceModel.class, modelLocation);
    }

    /**
     * Deserialize the contents of a given configuration file.
     *
     * @param clzz                      Class to deserialize into
     * @param configurationFileLocation Location of config file to load
     * @return Marshalled configuration class
     */
    public static <T> T loadConfigurationModel(Class<T> clzz, String configurationFileLocation) {
        log.info("Loading config file {}", configurationFileLocation);
        InputStream fileContents = null;
        try {
            fileContents = getRequiredResourceAsStream(configurationFileLocation);
            return Jackson.load(clzz, fileContents);
        } catch (IOException e) {
            log.error("Failed to read the configuration file {}", configurationFileLocation);
            throw new RuntimeException(e);
        } finally {
            if (fileContents != null) {
                Utils.closeQuietly(fileContents);
            }
        }
    }

    /**
     * Return an InputStream of the specified resource, failing if it can't be found.
     *
     * @param location Location of resource
     */
    public static InputStream getRequiredResourceAsStream(String location) {
        return Utils.getRequiredResourceAsStream(ModelLoaderUtils.class, location);
    }

    public static <T> T loadModel(Class<T> clzz, File file) {
        try {
            return Jackson.load(clzz, file);
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
}
