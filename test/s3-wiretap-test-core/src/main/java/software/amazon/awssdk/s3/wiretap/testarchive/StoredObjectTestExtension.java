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

package software.amazon.awssdk.s3.wiretap.testarchive;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.support.AnnotationSupport;
import software.amazon.awssdk.s3.wiretap.server.helpers.get.StoredObject;

public class StoredObjectTestExtension implements TestTemplateInvocationContextProvider {

    private final ClientTestExtension clients = new ClientTestExtension();

    @Override
    public boolean supportsTestTemplate(ExtensionContext context) {
        return context.getTestMethod()
                .map(m -> AnnotationSupport.findAnnotation(m, StoredObjectTest.class).isPresent())
                .orElse(false);
    }

    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext context) {
        Method method = context.getRequiredTestMethod();
        StoredObjectTest annotation = AnnotationSupport
                .findAnnotation(method, StoredObjectTest.class)
                .orElseThrow(() -> new IllegalStateException(
                        "no @StoredObjectTest on " + method.getName()));

        List<StoredObject> objects = objects(method, annotation);
        List<ChecksumModeOption> checksumModes = checksumModes(method);
        boolean labelObject = objects.size() > 1;
        boolean labelChecksumMode = checksumModes.size() > 1;

        return clients.provideTestTemplateInvocationContexts(context)
                .flatMap(base -> objects.stream()
                        .flatMap(object -> checksumModes.stream()
                                .map(mode -> wrap(base, object, mode,
                                        labelObject, labelChecksumMode))));
    }

    private static List<StoredObject> objects(Method method, StoredObjectTest annotation) {
        if (!Arrays.asList(method.getParameterTypes()).contains(StoredObject.class)) {
            throw new IllegalStateException("@StoredObjectTest method " + method.getName()
                    + " must declare a StoredObject parameter to consume the swept shape");
        }
        if (annotation.shapes().length == 0) {
            throw new IllegalStateException("@StoredObjectTest method " + method.getName()
                    + " has an empty shapes(); list at least one shape");
        }
        List<StoredObject> objects = new ArrayList<>();
        for (StoredObject.Shape shape : annotation.shapes()) {
            objects.add(shape.create(annotation.size(), annotation.partSize()));
        }
        return objects;
    }

    private static List<ChecksumModeOption> checksumModes(Method method) {
        return Arrays.asList(method.getParameterTypes()).contains(ChecksumModeOption.class)
                ? List.of(ChecksumModeOption.ENABLED, ChecksumModeOption.UNSET)
                : List.of(ChecksumModeOption.UNSET);
    }

    private static TestTemplateInvocationContext wrap(TestTemplateInvocationContext base,
                                                      StoredObject object,
                                                      ChecksumModeOption checksumMode,
                                                      boolean labelObject,
                                                      boolean labelChecksumMode) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                StringBuilder name = new StringBuilder(base.getDisplayName(invocationIndex));
                if (labelObject) {
                    name.append(", object=").append(object.shape().label());
                }
                if (labelChecksumMode) {
                    name.append(", checksumMode=").append(checksumMode.label());
                }
                return name.toString();
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                List<Extension> extensions = new ArrayList<>(base.getAdditionalExtensions());
                extensions.add(new DataResolver(object, checksumMode));
                return extensions;
            }
        };
    }

    private record DataResolver(StoredObject object, ChecksumModeOption checksumMode)
            implements ParameterResolver {

        @Override
        public boolean supportsParameter(ParameterContext parameterContext,
                                         ExtensionContext extensionContext) {
            Class<?> type = parameterContext.getParameter().getType();
            return type == StoredObject.class || type == ChecksumModeOption.class;
        }

        @Override
        public Object resolveParameter(ParameterContext parameterContext,
                                       ExtensionContext extensionContext) {
            return parameterContext.getParameter().getType() == StoredObject.class
                    ? object
                    : checksumMode;
        }
    }
}
