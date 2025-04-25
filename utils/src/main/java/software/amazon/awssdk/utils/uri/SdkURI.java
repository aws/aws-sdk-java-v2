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

package software.amazon.awssdk.utils.uri;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.utils.Logger;
import software.amazon.awssdk.utils.cache.lru.LruCache;
import software.amazon.awssdk.utils.uri.internal.UriConstructorArgs;

/**
 * Global cache for account-id based URI. Prevent calling new URI constructor for the same string, which can cause performance
 * issues with some uri pattern.
 * Do not directly depend on this class, it will be removed in the future.
 */
@SdkProtectedApi
public final class SdkURI {

    private static final Logger log = Logger.loggerFor(SdkURI.class);

    private static final SdkURI INSTANCE = new SdkURI();

    private final LruCache<UriConstructorArgs, URI> cache = LruCache.builder(UriConstructorArgs::newInstance)
                                                                    .maxSize(100)
                                                                    .build();

    public static SdkURI getInstance() {
        return INSTANCE;
    }

    public URI create(String s) {
        if (!isAccountIdUri(s)) {
            log.trace(() -> "skipping cache for uri " + s);
            return URI.create(s);
        }
        StringConstructorArgs key = new StringConstructorArgs(s);
        boolean containsK = cache.contains(key);
        URI uri = cache.get(key);
        logCacheUsage(containsK, uri);
        return uri;
    }

    public URI newURI(String s) throws URISyntaxException {
        if (!isAccountIdUri(s)) {
            log.trace(() -> "skipping cache for uri " + s);
            return new URI(s);
        }
        try {
            StringConstructorArgs key = new StringConstructorArgs(s);
            boolean containsK = cache.contains(key);
            URI uri = cache.get(key);
            logCacheUsage(containsK, uri);
            return uri;
        } catch (IllegalArgumentException e) {
            // URI.create() wraps the URISyntaxException thrown by new URI in a IllegalArgumentException, we need to unwrap it
            if (e.getCause() != null && e.getCause() instanceof URISyntaxException) {
                throw (URISyntaxException) e.getCause();
            }
            throw e;
        }
    }

    public URI newURI(String scheme,
                      String userInfo, String host, int port,
                      String path, String query, String fragment)
        throws URISyntaxException {
        if (!isAccountIdUri(host)) {
            log.trace(() -> "skipping cache for host" + host);
            return new URI(scheme, userInfo, host, port, path, query, fragment);
        }
        HostConstructorArgs key = new HostConstructorArgs(scheme, userInfo, host, port, path, query, fragment);
        boolean containsK = cache.contains(key);
        URI uri = cache.get(key);
        logCacheUsage(containsK, uri);
        return uri;
    }

    public URI newURI(String scheme,
                      String authority,
                      String path, String query, String fragment)
        throws URISyntaxException {
        if (!isAccountIdUri(authority)) {
            log.trace(() -> "skipping cache for authority" + authority);
            return new URI(scheme, authority, path, query, fragment);
        }
        AuthorityConstructorArgs key = new AuthorityConstructorArgs(scheme, authority, path, query, fragment);
        boolean containsK = cache.contains(key);
        URI uri = cache.get(key);
        logCacheUsage(containsK, uri);
        return uri;
    }

    // We only want to cache account-id based URI
    private boolean isAccountIdUri(String s) {
        if (s.startsWith("https://")) {
            return Character.isDigit(s.charAt(8));
        }

        if (s.startsWith("http://")) {
            return Character.isDigit(s.charAt(7));
        }
        return Character.isDigit(s.charAt(0));
    }

    private void logCacheUsage(boolean containsKey, URI uri) {
        log.trace(() -> "URI cache size: " + cache.size());
        if (containsKey) {
            log.trace(() -> "Using cached uri for " + uri.toString());
        } else {
            log.trace(() -> "Cache empty for " + uri.toString());
        }
    }

    private static final class StringConstructorArgs implements UriConstructorArgs {
        private final String str;

        private StringConstructorArgs(String str) {
            this.str = str;
        }

        @Override
        public URI newInstance() {
            return URI.create(str);
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            StringConstructorArgs that = (StringConstructorArgs) o;
            return Objects.equals(str, that.str);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(str);
        }
    }

    private static final class HostConstructorArgs implements UriConstructorArgs {
        private final String scheme;
        private final String userInfo;
        private final String host;
        private final int port;
        private final String path;
        private final String query;
        private final String fragment;

        private HostConstructorArgs(String scheme,
                                    String userInfo, String host, int port,
                                    String path, String query, String fragment) {
            this.scheme = scheme;
            this.userInfo = userInfo;
            this.host = host;
            this.port = port;
            this.path = path;
            this.query = query;
            this.fragment = fragment;
        }

        @Override
        public URI newInstance() {
            try {
                return new URI(scheme, userInfo, host, port, path, query, fragment);
            } catch (URISyntaxException x) {
                throw new IllegalArgumentException(x.getMessage(), x);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            HostConstructorArgs that = (HostConstructorArgs) o;
            return port == that.port && Objects.equals(scheme, that.scheme) && Objects.equals(userInfo, that.userInfo) && Objects.equals(host, that.host) && Objects.equals(path, that.path) && Objects.equals(query, that.query) && Objects.equals(fragment, that.fragment);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(scheme);
            result = 31 * result + Objects.hashCode(userInfo);
            result = 31 * result + Objects.hashCode(host);
            result = 31 * result + port;
            result = 31 * result + Objects.hashCode(path);
            result = 31 * result + Objects.hashCode(query);
            result = 31 * result + Objects.hashCode(fragment);
            return result;
        }
    }

    private static final class AuthorityConstructorArgs implements UriConstructorArgs {
        private final String scheme;
        private final String authority;
        private final String path;
        private final String query;
        private final String fragment;

        private AuthorityConstructorArgs(String scheme, String authority, String path, String query, String fragment) {
            this.scheme = scheme;
            this.authority = authority;
            this.path = path;
            this.query = query;
            this.fragment = fragment;
        }

        @Override
        public URI newInstance() {
            try {
                return new URI(scheme, authority, path, query, fragment);
            } catch (URISyntaxException x) {
                throw new IllegalArgumentException(x.getMessage(), x);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            AuthorityConstructorArgs that = (AuthorityConstructorArgs) o;
            return Objects.equals(scheme, that.scheme) && Objects.equals(authority, that.authority) && Objects.equals(path,
                                                                                                                      that.path) && Objects.equals(query, that.query) && Objects.equals(fragment, that.fragment);
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(scheme);
            result = 31 * result + Objects.hashCode(authority);
            result = 31 * result + Objects.hashCode(path);
            result = 31 * result + Objects.hashCode(query);
            result = 31 * result + Objects.hashCode(fragment);
            return result;
        }
    }
}
