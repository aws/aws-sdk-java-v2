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

package software.amazon.awssdk.s3.wiretap.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;
import software.amazon.awssdk.s3.wiretap.client.feature.Feature;
import software.amazon.awssdk.s3.wiretap.client.feature.transport.HttpClientFeature;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

public final class ClientConfig {

    private final Map<Feature<?>, Category<?>> selected;

    public ClientConfig(Map<Feature<?>, Category<?>> selected) {
        this.selected = new LinkedHashMap<>(selected);
    }

    public String describe() {
        StringJoiner joiner = new StringJoiner(", ");
        for (Map.Entry<Feature<?>, Category<?>> entry : selected.entrySet()) {
            if (isDefault(entry.getKey(), entry.getValue()) && !entry.getKey().alwaysShowInLabel()) {
                continue;
            }
            joiner.add(entry.getKey().name() + "=" + entry.getValue().getClass().getSimpleName());
        }
        String label = joiner.toString();
        return label.isEmpty() ? "default" : label;
    }

    private static boolean isDefault(Feature<?> feature, Category<?> category) {
        return feature.defaultCategory(S3Client.class).getClass() == category.getClass();
    }

    public Map<Feature<?>, Category<?>> entries() {
        return Collections.unmodifiableMap(selected);
    }

    public ClientKind kind() {
        return value(HttpClientFeature.class).kind();
    }

    public List<Feature<?>> inapplicableFeatures() {
        ClientKind kind = kind();
        List<Feature<?>> inapplicable = new ArrayList<>();
        for (Map.Entry<Feature<?>, Category<?>> entry : selected.entrySet()) {
            Feature<?> feature = entry.getKey();
            if (isDefault(feature, entry.getValue())) {
                continue;
            }
            if (!feature.supportedKinds().contains(kind)) {
                inapplicable.add(feature);
            }
        }
        return inapplicable;
    }

    public <T, F extends Feature<T>> Category<T> category(Class<F> featureType) {
        for (Map.Entry<Feature<?>, Category<?>> entry : selected.entrySet()) {
            if (featureType.isInstance(entry.getKey())) {
                @SuppressWarnings("unchecked")
                Category<T> category = (Category<T>) entry.getValue();
                return category;
            }
        }
        throw new IllegalArgumentException("No " + featureType.getSimpleName() + " in this config");
    }

    public <T, F extends Feature<T>> T value(Class<F> featureType) {
        return category(featureType).value();
    }

    public <T> T value(Feature<T> feature) {
        Category<?> category = selected.get(feature);
        if (category == null) {
            throw new IllegalArgumentException("No " + feature.name() + " in this config");
        }
        @SuppressWarnings("unchecked")
        T value = (T) category.value();
        return value;
    }

    public S3Client buildSync(ClientBuilder builder) {
        applyAll(builder);
        if (builder.isCrtCreate()) {
            return new SyncOverAsync(builder.buildCrtAsync());
        }
        if (builder.isAsync()) {
            return new SyncOverAsync(builder.buildAsync());
        }
        return builder.buildSync();
    }

    public S3AsyncClient buildAsync(ClientBuilder builder) {
        applyAll(builder);
        return builder.isCrtCreate() ? builder.buildCrtAsync() : builder.buildAsync();
    }

    public ConfiguredClient buildConfigured(ClientBuilder builder) {
        applyAll(builder);
        if (builder.isCrtCreate() || builder.isAsync()) {
            S3AsyncClient async = builder.isCrtCreate() ? builder.buildCrtAsync() : builder.buildAsync();
            return new ConfiguredClient(new SyncOverAsync(async), async, this);
        }
        return new ConfiguredClient(builder.buildSync(), null, this);
    }

    private void applyAll(ClientBuilder builder) {
        for (Map.Entry<Feature<?>, Category<?>> entry : selected.entrySet()) {
            applyOne(builder, entry.getKey(), entry.getValue());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void applyOne(ClientBuilder builder, Feature<T> feature, Category<?> category) {
        feature.apply(builder, (Category<T>) category);
    }

    @Override
    public String toString() {
        return describe();
    }
}
