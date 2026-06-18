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

package software.amazon.awssdk.http.auth.aws.internal.signer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import software.amazon.awssdk.annotations.Immutable;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.utils.StringUtils;

/**
 * A lightweight {@link SdkHttpFullRequest} view that wraps an unsigned source request and overlays the headers a
 * SigV4 signer adds (or replaces) on top of it. Used by {@link FastV4HeaderSigner} as a drop-in replacement for the
 * {@code source.toBuilder().putHeader(...).build()} round-trip — the savings come from not deep-copying the source's
 * header map and not allocating intermediate {@code LowCopyListMap.ForBuilder} / {@code ForBuildable} / {@code Lazy}
 * wrappers on every sign.
 *
 * <p>The view is immutable from the caller's perspective. Header lookups are case-insensitive and produce the same
 * results the rebuilt {@code DefaultSdkHttpFullRequest} would have:
 * <ul>
 *     <li>{@link #firstMatchingHeader(String)} returns the signer-added value if present, otherwise the source's
 *         value (unless suppressed).</li>
 *     <li>{@link #forEachHeader(BiConsumer)} yields the source's headers (skipping suppressed names) followed by
 *         the signer-added headers.</li>
 *     <li>{@link #headers()} lazily builds a case-insensitively-sorted {@link TreeMap} merging both sources, only
 *         on first call.</li>
 * </ul>
 *
 * <p>{@link #toBuilder()} materializes a real {@code SdkHttpFullRequest.Builder} by populating one from this view's
 * effective state. This is the slow fallback path; in normal sign-then-execute flows it isn't called.
 */
@SdkInternalApi
@Immutable
final class SignedSdkHttpFullRequest implements SdkHttpFullRequest {

    private final SdkHttpRequest source;
    private final ContentStreamProvider payload;

    /** Strided (name, value) pairs the signer is adding. Names are emitted exactly as the signer chose to spell them. */
    private final String[] addedHeaders;
    private final int addedCount;

    /**
     * Names of source headers that should be hidden from the merged view because the signer is providing its own
     * value (case-insensitively). For example, {@code x-amz-content-sha256} is always signer-managed, so any value
     * the user supplied on the source request is suppressed before the signer's value is added back through
     * {@link #addedHeaders}. Stored lowercase so {@link #isSuppressed(String)} can use {@code equalsIgnoreCase}
     * cheaply.
     */
    private final String[] suppressedSourceHeaders;
    private final int suppressedCount;

    private volatile Map<String, List<String>> mergedHeadersCache;

    SignedSdkHttpFullRequest(SdkHttpRequest source,
                             ContentStreamProvider payload,
                             String[] addedHeaders,
                             int addedCount,
                             String[] suppressedSourceHeaders,
                             int suppressedCount) {
        this.source = source;
        this.payload = payload;
        this.addedHeaders = addedHeaders;
        this.addedCount = addedCount;
        this.suppressedSourceHeaders = suppressedSourceHeaders;
        this.suppressedCount = suppressedCount;
    }

    @Override
    public String protocol() {
        return source.protocol();
    }

    @Override
    public String host() {
        return source.host();
    }

    @Override
    public int port() {
        return source.port();
    }

    @Override
    public String encodedPath() {
        return source.encodedPath();
    }

    @Override
    public Map<String, List<String>> rawQueryParameters() {
        return source.rawQueryParameters();
    }

    @Override
    public Optional<String> firstMatchingRawQueryParameter(String key) {
        return source.firstMatchingRawQueryParameter(key);
    }

    @Override
    public Optional<String> firstMatchingRawQueryParameter(Collection<String> keys) {
        return source.firstMatchingRawQueryParameter(keys);
    }

    @Override
    public List<String> firstMatchingRawQueryParameters(String key) {
        return source.firstMatchingRawQueryParameters(key);
    }

    @Override
    public void forEachRawQueryParameter(BiConsumer<? super String, ? super List<String>> consumer) {
        source.forEachRawQueryParameter(consumer);
    }

    @Override
    public int numRawQueryParameters() {
        return source.numRawQueryParameters();
    }

    @Override
    public Optional<String> encodedQueryParameters() {
        return source.encodedQueryParameters();
    }

    @Override
    public Optional<String> encodedQueryParametersAsFormData() {
        return source.encodedQueryParametersAsFormData();
    }

    @Override
    public SdkHttpMethod method() {
        return source.method();
    }

    @Override
    public Optional<ContentStreamProvider> contentStreamProvider() {
        return Optional.ofNullable(payload);
    }

    @Override
    public Map<String, List<String>> headers() {
        Map<String, List<String>> cached = mergedHeadersCache;
        if (cached == null) {
            cached = buildMergedHeaders();
            mergedHeadersCache = cached;
        }
        return cached;
    }

    @Override
    public Optional<String> firstMatchingHeader(String header) {
        // Signer-added headers always take precedence (mirrors V2's putHeader-overrides semantics).
        for (int i = 0; i < addedCount; i++) {
            if (header.equalsIgnoreCase(addedHeaders[i * 2])) {
                String value = addedHeaders[i * 2 + 1];
                return StringUtils.isEmpty(value) ? Optional.empty() : Optional.of(value);
            }
        }
        if (isSuppressed(header)) {
            return Optional.empty();
        }
        return source.firstMatchingHeader(header);
    }

    @Override
    public Optional<String> firstMatchingHeader(Collection<String> headersToFind) {
        for (String name : headersToFind) {
            Optional<String> match = firstMatchingHeader(name);
            if (match.isPresent()) {
                return match;
            }
        }
        return Optional.empty();
    }

    @Override
    public List<String> matchingHeaders(String header) {
        for (int i = 0; i < addedCount; i++) {
            if (header.equalsIgnoreCase(addedHeaders[i * 2])) {
                return Collections.singletonList(addedHeaders[i * 2 + 1]);
            }
        }
        if (isSuppressed(header)) {
            return Collections.emptyList();
        }
        return source.matchingHeaders(header);
    }

    @Override
    public void forEachHeader(BiConsumer<? super String, ? super List<String>> consumer) {
        source.forEachHeader((name, values) -> {
            if (!isSuppressed(name)) {
                consumer.accept(name, values);
            }
        });
        for (int i = 0; i < addedCount; i++) {
            consumer.accept(addedHeaders[i * 2], Collections.singletonList(addedHeaders[i * 2 + 1]));
        }
    }

    @Override
    public int numHeaders() {
        return headers().size();
    }

    @Override
    public SdkHttpFullRequest.Builder toBuilder() {
        // Materialize a real DefaultSdkHttpFullRequest.Builder. This is the fallback path: it pays the same allocation
        // cost the fast path was avoiding, but it's only triggered if a downstream consumer actually needs to mutate
        // the signed request — which the production sign-then-execute pipeline doesn't.
        SdkHttpFullRequest.Builder builder = SdkHttpFullRequest.builder()
                                                               .protocol(source.protocol())
                                                               .host(source.host())
                                                               .port(source.port())
                                                               .encodedPath(source.encodedPath())
                                                               .method(source.method())
                                                               .contentStreamProvider(payload);
        forEachHeader(builder::putHeader);
        source.forEachRawQueryParameter(builder::putRawQueryParameter);
        return builder;
    }

    /**
     * Build the case-insensitively-sorted merged map. Source header values are aliased (no copy) since they're
     * already unmodifiable from the source's perspective; signer-added values are wrapped in
     * {@link Collections#singletonList(Object)}.
     */
    private Map<String, List<String>> buildMergedHeaders() {
        TreeMap<String, List<String>> merged = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        source.forEachHeader((name, values) -> {
            if (!isSuppressed(name)) {
                merged.put(name, values);
            }
        });
        for (int i = 0; i < addedCount; i++) {
            merged.put(addedHeaders[i * 2], Collections.singletonList(addedHeaders[i * 2 + 1]));
        }
        return Collections.unmodifiableMap(merged);
    }

    private boolean isSuppressed(String name) {
        for (int i = 0; i < suppressedCount; i++) {
            if (name.equalsIgnoreCase(suppressedSourceHeaders[i])) {
                return true;
            }
        }
        return false;
    }
}
