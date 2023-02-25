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

package software.amazon.awssdk.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.internal.util.ClassLoaderHelper;
import software.amazon.awssdk.utils.StringUtils;
import software.amazon.awssdk.utils.Validate;

@SdkProtectedApi
public class PreloadClassProcessor {

    public void loadClasses(boolean initializeClasses)  {

        try {
            createPreloadClassesFromResources(classLoader().getResources("META-INF/preload.classes"))
                .forEach(clz -> {
                    try {
                        if (!StringUtils.isEmpty(clz)) {
                            Class.forName(clz, initializeClasses, PreloadClassProcessor.class.getClassLoader());
                        }
                    } catch (ClassNotFoundException e) {
                        System.out.println("class not found: " + clz);
                    }
                });
        } catch (IOException e) {
            System.out.println("Failed to load classes" + e);
        }

    }

    private Stream<String> createPreloadClassesFromResources(Enumeration<URL> resources) {
        if (resources == null) {
            return Stream.empty();
        }

        return Collections.list(resources).stream().flatMap(this::createPreloadClassesFromResources);
    }

    private Stream<String> createPreloadClassesFromResources(URL resource) {
        try {
            if (resource == null) {
                return Stream.empty();
            }

            List<String> classes = new ArrayList<>();

            try (InputStream stream = resource.openStream();
                 InputStreamReader streamReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
                 BufferedReader fileReader = new BufferedReader(streamReader)) {

                String preloadClassName = fileReader.readLine();
                while (preloadClassName != null) {
                        classes.add(preloadClassName);
                    //System.out.println("loading class: " + preloadClassName);
                    preloadClassName = fileReader.readLine();
                }
            }

            System.out.println("number of classes being loaded " + classes.size());
            return classes.stream();
        } catch (IOException e) {
            throw SdkClientException.builder()
                                    .message("Unable to instantiate execution interceptor chain.")
                                    .cause(e)
                                    .build();
        }
    }

    private ClassLoader classLoader() {
        return Validate.notNull(ClassLoaderHelper.classLoader(getClass()),
                                "Failed to load the classloader of this class or the system.");
    }
}
