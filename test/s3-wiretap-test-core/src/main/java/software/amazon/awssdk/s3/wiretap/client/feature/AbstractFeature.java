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

package software.amazon.awssdk.s3.wiretap.client.feature;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import lombok.Getter;
import lombok.experimental.Accessors;
import software.amazon.awssdk.s3.wiretap.client.ClientBuilder;
import software.amazon.awssdk.s3.wiretap.client.ClientKind;

@Accessors(fluent = true)
public abstract class AbstractFeature<T> implements Feature<T> {

    @Getter
    private final String name;
    @Getter
    private final Class<T> valueType;
    private final Map<ClientKind, BiConsumer<ClientBuilder, T>> writers =
            new EnumMap<>(ClientKind.class);

    protected AbstractFeature(String name, Class<T> valueType) {
        this.name = name;
        this.valueType = valueType;
    }

    protected final void onSync(BiConsumer<ClientBuilder, T> writer) {
        register(ClientKind.SYNC, writer);
    }

    protected final void onAsync(BiConsumer<ClientBuilder, T> writer) {
        register(ClientKind.ASYNC, writer);
    }

    protected final void onCrt(BiConsumer<ClientBuilder, T> writer) {
        register(ClientKind.CRT, writer);
    }

    private void register(ClientKind kind, BiConsumer<ClientBuilder, T> writer) {
        if (writers.put(kind, writer) != null) {
            throw new IllegalStateException(name() + " registered a " + kind + " writer twice");
        }
    }

    @Override
    public final void apply(ClientBuilder builder, Category<T> category) {
        T value = category.value();
        if (isNoOp(value)) {
            return;
        }
        BiConsumer<ClientBuilder, T> writer = writers.get(builder.kind());
        if (writer == null) {
            throw new IllegalStateException(name() + " does not support "
                    + builder.kind() + " clients (no writer registered)");
        }
        writer.accept(builder, value);
    }

    protected boolean isNoOp(T value) {
        return value == null;
    }

    @Override
    public final Set<ClientKind> supportedKinds() {
        return writers.isEmpty() ? EnumSet.noneOf(ClientKind.class) : EnumSet.copyOf(writers.keySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public final List<Category<T>> allCategories() {
        List<Class<?>> categoryClasses = new ArrayList<>();
        for (Class<?> c = getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Class<?> nested : c.getDeclaredClasses()) {
                if (Category.class.isAssignableFrom(nested)
                        && Modifier.isStatic(nested.getModifiers())
                        && Modifier.isPublic(nested.getModifiers())
                        && !Modifier.isAbstract(nested.getModifiers())) {
                    categoryClasses.add(nested);
                }
            }
        }
        categoryClasses.sort(Comparator.comparing(Class::getSimpleName));
        List<Category<T>> categories = new ArrayList<>(categoryClasses.size());
        for (Class<?> nested : categoryClasses) {
            try {
                categories.add((Category<T>) nested.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(name() + " category " + nested.getName()
                        + " needs a public no-arg constructor to be swept by a bare @Feature", e);
            }
        }
        if (categories.isEmpty()) {
            throw new IllegalStateException(name() + " declares no categories, so a bare "
                    + "@Feature naming it has nothing to sweep");
        }
        return categories;
    }
}
