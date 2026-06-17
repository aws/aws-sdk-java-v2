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

import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.AUTHORIZATION;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.AWS4_SIGNING_ALGORITHM;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.AWS4_TERMINATOR;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.HOST;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.UNSIGNED_PAYLOAD;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_CONTENT_SHA256;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_DATE;
import static software.amazon.awssdk.http.auth.aws.signer.SignerConstant.X_AMZ_SECURITY_TOKEN;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.DigestException;
import java.security.InvalidKeyException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.SecretKeySpec;
import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.internal.signer.util.V4SigningKeyCache;
import software.amazon.awssdk.http.auth.spi.signer.SignRequest;
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.AwsSessionCredentialsIdentity;
import software.amazon.awssdk.utils.http.SdkHttpUtils;

/**
 * The fast path for header-based SigV4 signing. End-to-end equivalent of {@link DefaultAwsV4HttpSigner}'s standard
 * pipeline (Checksummer → V4RequestSigner → V4PayloadSigner) for the common case, but written to avoid the
 * per-call allocation hot spots that show up in profiling.
 *
 * <p>Specifically this implementation:
 * <ul>
 *     <li>Borrows pooled scratch state from {@link V4SigningResources} for the {@code MessageDigest}, {@code Mac},
 *         text builders, and ASCII byte buffers. No per-sign {@code Mac.getInstance} or {@code byte[]} allocation.</li>
 *     <li>Streams the body through the pooled SHA-256 digest into the pooled hex buffer instead of going through a
 *         {@link software.amazon.awssdk.http.auth.aws.internal.signer.io.ChecksumInputStream} + per-call
 *         {@code byte[4096]} read buffer.</li>
 *     <li>Builds the canonical request and string-to-sign directly into pooled byte buffers, so the SHA-256 update
 *         calls don't need {@code String.getBytes(UTF_8)} (the canonical request is ASCII by construction).</li>
 *     <li>Stores headers and query parameters as strided {@code String[]} arrays sorted in place by insertion sort,
 *         instead of {@code List<Pair<String, List<String>>>} + {@code Comparator.comparing(...)}.</li>
 *     <li>Hits a per-(secret-key, region, service) signing-key cache via an object key with precomputed hashCode.</li>
 *     <li>Mutates the {@link SdkHttpRequest.Builder} once at the end (5 putHeader calls) instead of repeatedly during
 *         the canonicalization pass, avoiding extra {@code LowCopyListMap} rebuilds.</li>
 * </ul>
 *
 * <p>Output is byte-identical to the legacy V2 path for every case the fast path handles. Specifically it produces
 * the same signature, signed-headers list, and authorization header for any header-auth SigV4 sign with no
 * flexible-checksum, no chunk encoding, no presigning, no event streaming, and standard or session AWS credentials.
 */
@SdkInternalApi
final class FastV4HeaderSigner {

    /**
     * SHA-256 of the empty input. Pre-computed so the empty-body case skips digest setup entirely.
     */
    private static final String EMPTY_BODY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static final byte[] HEX_DIGITS = "0123456789abcdef".getBytes(StandardCharsets.US_ASCII);

    private static final String HMAC_SHA_256 = "HmacSHA256";

    /**
     * Headers the V2 SigV4 signer excludes from the canonical request and from {@code SignedHeaders}. Must stay in
     * sync with {@link V4CanonicalRequest#HEADERS_TO_IGNORE_IN_LOWER_CASE}. This set intentionally does not include
     * {@code content-length}; V2 signs it.
     */
    private static final String[] IGNORED_HEADERS_LOWERCASE = {
        "connection",
        "expect",
        "transfer-encoding",
        "user-agent",
        "x-amzn-trace-id",
        "x-forwarded-for"
    };

    private FastV4HeaderSigner() {
    }

    /**
     * Sign the given request through the fast path. Returns a {@link SignedRequest} whose {@code request()} carries
     * the {@code Authorization}, {@code X-Amz-Date}, {@code X-Amz-Content-Sha256}, optional {@code Host}, and
     * optional {@code X-Amz-Security-Token} headers, and whose {@code payload()} is the original payload unchanged
     * (the fast path doesn't transform payloads).
     */
    static SignedRequest sign(SignRequest<? extends AwsCredentialsIdentity> signRequest,
                              AwsCredentialsIdentity credentials,
                              String regionName,
                              String serviceName,
                              Clock signingClock,
                              boolean doubleUrlEncode,
                              boolean normalizePath,
                              boolean payloadSigningEnabled) {
        V4SigningResources r = V4SigningResources.acquire();
        try {
            return signInternal(signRequest, credentials, regionName, serviceName, signingClock,
                                doubleUrlEncode, normalizePath, payloadSigningEnabled, r);
        } finally {
            V4SigningResources.release(r);
        }
    }

    private static SignedRequest signInternal(SignRequest<? extends AwsCredentialsIdentity> signRequest,
                                              AwsCredentialsIdentity credentials,
                                              String regionName,
                                              String serviceName,
                                              Clock signingClock,
                                              boolean doubleUrlEncode,
                                              boolean normalizePath,
                                              boolean payloadSigningEnabled,
                                              V4SigningResources r) {
        SdkHttpRequest source = signRequest.request();
        ContentStreamProvider payload = signRequest.payload().orElse(null);
        Instant signingInstant = signingClock.instant();

        // 1. Body hash. Two cases match V2 exactly:
        //    - payload-signing enabled (the default for AWS SDK requests): SHA-256 of body bytes
        //    - payload-signing disabled + HTTPS: the literal sentinel "UNSIGNED-PAYLOAD"
        // The third V2 case (payload-signing disabled + HTTP + payload) is handled by the slow-path; the dispatcher
        // in DefaultAwsV4HttpSigner refuses the fast path in that case.
        String contentSha256 = payloadSigningEnabled ? hashPayload(payload, r) : UNSIGNED_PAYLOAD;

        // 2. Date strings. Built by manual digit append into the pooled StringBuilder; no DateTimeFormatter.
        LocalDateTime signingDateTime = signingInstant.atOffset(ZoneOffset.UTC).toLocalDateTime();
        String dateStamp = formatDate(signingDateTime, r.sb);
        String requestDateTime = formatDateTime(signingDateTime, dateStamp, r.sb);

        // 3. Build the strided header buffer. Pull every source header that survives the V2 ignore list, lowercasing
        //    on demand. Skip headers the signer manages itself; we add the signer-managed values explicitly below
        //    so the canonical block reflects what V2 would emit after putHeader-ing the request.
        boolean isSession = credentials instanceof AwsSessionCredentialsIdentity;
        boolean sourceHasHost = collectSourceHeaders(source, isSession, r);

        r.addHeaderCanonical(X_AMZ_CONTENT_SHA256, contentSha256);
        r.addHeaderCanonical("x-amz-date", requestDateTime);
        if (isSession) {
            String token = ((AwsSessionCredentialsIdentity) credentials).sessionToken();
            r.addHeaderCanonical("x-amz-security-token", token);
        }
        String hostValue = null;
        if (!sourceHasHost) {
            hostValue = computeHostHeader(source);
            r.addHeaderCanonical("host", hostValue);
        }

        r.sortHeadersByName();

        // 4. SignedHeaders + canonical request, both written into pooled buffers.
        String signedHeaders = buildSignedHeadersString(r);
        int canonicalLen = buildCanonicalRequest(source, signedHeaders, contentSha256,
                                                 doubleUrlEncode, normalizePath, r);

        // 5. Derive (or reuse) the signing key for this (secret, region, service, day) tuple.
        byte[] signingKey = deriveSigningKey(credentials, regionName, serviceName, dateStamp, signingInstant, r);

        // 6. Compute the signature: SHA-256(canonical) → string-to-sign → HMAC-SHA256 → hex.
        String signature = computeSignature(r.canonicalRequestBytes, canonicalLen, requestDateTime,
                                            dateStamp, regionName, serviceName, signingKey, r);

        // 7. Build the Authorization header value into the pooled StringBuilder.
        String scope = buildScope(dateStamp, regionName, serviceName, r.sb);
        String authorization = buildAuthorizationHeader(credentials.accessKeyId(), scope,
                                                        signedHeaders, signature, r.sb);

        // 8. Apply all signer-managed headers in one builder pass and return the signed request.
        SdkHttpRequest.Builder builder = source.toBuilder();
        builder.putHeader(X_AMZ_DATE, requestDateTime);
        builder.putHeader(X_AMZ_CONTENT_SHA256, contentSha256);
        if (isSession) {
            builder.putHeader(X_AMZ_SECURITY_TOKEN,
                              ((AwsSessionCredentialsIdentity) credentials).sessionToken());
        }
        if (!sourceHasHost) {
            builder.putHeader(HOST, hostValue);
        }
        builder.putHeader(AUTHORIZATION, authorization);

        return SignedRequest.builder()
                            .request(builder.build())
                            .payload(payload)
                            .build();
    }

    /**
     * Stream the request body through the pooled SHA-256 digest and return the lowercase-hex digest.
     *
     * <p>Mirrors {@link software.amazon.awssdk.http.auth.aws.internal.signer.FlexibleChecksummer}'s default
     * SHA-256-only behaviour: a null or empty payload hashes to the well-known empty-body digest. The shared 8 KB
     * read buffer in {@link V4SigningResources#readBuffer} replaces the per-call {@code byte[4096]} that
     * {@code ChecksumUtil.readAll} allocates today (the #1 allocation hot spot in profiling).
     */
    private static String hashPayload(ContentStreamProvider payload, V4SigningResources r) {
        if (payload == null) {
            return EMPTY_BODY_SHA256;
        }
        InputStream stream;
        try {
            stream = payload.newStream();
        } catch (RuntimeException e) {
            throw e;
        }
        if (stream == null) {
            return EMPTY_BODY_SHA256;
        }
        try {
            r.sha256Digest.reset();
            byte[] buf = r.readBuffer;
            int totalRead = 0;
            int read;
            while ((read = stream.read(buf)) >= 0) {
                if (read > 0) {
                    r.sha256Digest.update(buf, 0, read);
                    totalRead += read;
                }
            }
            if (totalRead == 0) {
                return EMPTY_BODY_SHA256;
            }
            r.sha256Digest.digest(r.hashBytes, 0, r.hashBytes.length);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read request payload to sign request: " + e.getMessage(), e);
        } catch (DigestException e) {
            throw new RuntimeException("Unable to compute SHA-256 of request payload: " + e.getMessage(), e);
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
                // closing is best-effort; the legacy code path also ignores close failures here
            }
        }

        byte[] hex = r.signatureHexBytes;
        writeHex(r.hashBytes, hex, 0);
        return new String(hex, 0, hex.length, StandardCharsets.US_ASCII);
    }

    /**
     * Iterate the source request's headers, lowercase the names on demand, drop the V2-ignored headers and any header
     * the signer will overwrite, and add them to the strided buffer. Returns true if the source already supplies a
     * Host header (any case) — the caller skips synthesizing one in that case.
     *
     * <p>Uses {@link SdkHttpRequest#forEachHeader} (rather than {@code headers().entrySet()}) to avoid the deep
     * defensive copy {@code headers()} performs on each call.
     */
    private static boolean collectSourceHeaders(SdkHttpRequest source, boolean isSession, V4SigningResources r) {
        boolean[] sawHost = {false};
        source.forEachHeader((rawName, values) -> {
            String name = V4SigningResources.lowercaseIfNeeded(rawName);
            // x-amz-content-sha256, x-amz-date, x-amz-security-token (when session), authorization: signer overwrites
            // these, so drop any user-supplied value before we add the signer's. Host is signer-managed too but the
            // signer keeps the user's value if present, so we still include it in the canonical buffer here.
            if (isSignerManaged(name, isSession)) {
                if ("host".equals(name)) {
                    sawHost[0] = true;
                    // fall through to include the user's Host value
                } else {
                    return;
                }
            }
            if (isIgnoredHeader(name)) {
                return;
            }
            for (int i = 0; i < values.size(); i++) {
                r.addHeaderCanonical(name, values.get(i));
            }
        });
        return sawHost[0];
    }

    /**
     * @return true if the signer will set this header itself, in which case any user-supplied value is dropped from
     *     canonicalization before the signer's value is added (host being the exception — see caller).
     */
    private static boolean isSignerManaged(String lowercaseName, boolean isSession) {
        switch (lowercaseName) {
            case "x-amz-content-sha256":
            case "x-amz-date":
            case "authorization":
                return true;
            case "x-amz-security-token":
                // Only managed when we're actually going to write a session token. A non-session credential should
                // not touch user-supplied tokens (rare but supported by V2).
                return isSession;
            case "host":
                return true;
            default:
                return false;
        }
    }

    /**
     * Constant-N comparison against the V2 ignore list. With six entries an unrolled equals is faster than a HashSet
     * lookup, and avoids the static-init cost of building one.
     */
    private static boolean isIgnoredHeader(String lowercaseName) {
        for (int i = 0; i < IGNORED_HEADERS_LOWERCASE.length; i++) {
            if (IGNORED_HEADERS_LOWERCASE[i].equals(lowercaseName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Compute the {@code Host} header value. Matches {@code SignerUtils.addHostHeader}: emit a port suffix only when
     * the request uses a non-standard port for its scheme.
     */
    private static String computeHostHeader(SdkHttpRequest source) {
        String host = source.host();
        if (SdkHttpUtils.isUsingStandardPort(source.protocol(), source.port())) {
            return host;
        }
        return host + ":" + source.port();
    }

    /**
     * Build the SignedHeaders semicolon list directly from the sorted strided buffer, deduplicating consecutive
     * duplicates produced by multi-valued headers.
     */
    private static String buildSignedHeadersString(V4SigningResources r) {
        StringBuilder sb = r.sb;
        sb.setLength(0);
        String previous = null;
        for (int i = 0; i < r.headerCount; i++) {
            String name = r.headers[i * 2];
            if (name.equals(previous)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(name);
            previous = name;
        }
        return sb.toString();
    }

    /**
     * Build the canonical request into the pooled byte buffer and return the byte length. The canonical request is
     * ASCII by construction (URL-encoded path, percent-encoded query, lowercased header names, trimmed values, hex
     * payload digest), so each char in the {@link StringBuilder} maps to one byte without going through the UTF-8
     * encoder.
     */
    private static int buildCanonicalRequest(SdkHttpRequest source,
                                             String signedHeaders,
                                             String contentSha256,
                                             boolean doubleUrlEncode,
                                             boolean normalizePath,
                                             V4SigningResources r) {
        StringBuilder sb = r.sb;
        sb.setLength(0);

        sb.append(source.method().name());
        sb.append('\n');
        appendCanonicalUri(source, doubleUrlEncode, normalizePath, sb);
        sb.append('\n');
        appendCanonicalQueryString(source, sb);
        sb.append('\n');
        appendCanonicalHeaders(r, sb);
        sb.append('\n');
        sb.append(signedHeaders);
        sb.append('\n');
        sb.append(contentSha256);

        int len = sb.length();
        byte[] dst = r.ensureCanonicalRequestCapacity(len);
        for (int i = 0; i < len; i++) {
            // Canonical request chars are ASCII; narrowing cast is safe and skips the UTF-8 encoder.
            dst[i] = (byte) sb.charAt(i);
        }
        return len;
    }

    /**
     * Mirror {@link V4CanonicalRequest#getCanonicalUri} for the fast path. Allocates the same intermediate strings
     * as the legacy path; the savings here are not in URI processing itself but in not allocating the surrounding
     * {@code V4CanonicalRequest} object.
     */
    private static void appendCanonicalUri(SdkHttpRequest source,
                                           boolean doubleUrlEncode,
                                           boolean normalizePath,
                                           StringBuilder out) {
        String path = source.encodedPath();
        if (path == null || path.isEmpty() || "/".equals(path)) {
            out.append('/');
            return;
        }

        URI uri = null;
        if (normalizePath) {
            uri = source.getUri();
            path = uri.normalize().getRawPath();
        }
        if (doubleUrlEncode) {
            path = SdkHttpUtils.urlEncodeIgnoreSlashes(path);
        }
        if (path.isEmpty()) {
            out.append('/');
            return;
        }
        boolean leadingSlash = path.charAt(0) == '/';
        if (!leadingSlash) {
            out.append('/');
        }

        // Drop a trailing slash that came from URI normalization but wasn't in the original path. Matches the
        // {@code trimTrailingSlash} logic in V4CanonicalRequest.
        boolean trimTrailing = normalizePath
                               && path.length() > 1
                               && path.charAt(path.length() - 1) == '/'
                               && uri != null
                               && !uri.getPath().endsWith("/");
        int end = trimTrailing ? path.length() - 1 : path.length();
        out.append(path, 0, end);
    }

    /**
     * Walk the request's raw query parameters, percent-encode each name and value, sort, and emit the canonical
     * query string. Uses {@link SdkHttpRequest#forEachRawQueryParameter} to avoid the defensive copy that
     * {@code rawQueryParameters()} would do.
     *
     * <p>Optimizing this further (avoiding the {@code TreeMap}+{@code ArrayList}) is a future improvement; the
     * production hot path for service operations rarely has query parameters, so this stays close to V2's existing
     * code shape.
     */
    private static void appendCanonicalQueryString(SdkHttpRequest source, StringBuilder out) {
        if (source.numRawQueryParameters() == 0) {
            return;
        }
        java.util.SortedMap<String, java.util.List<String>> sorted = new java.util.TreeMap<>();
        source.forEachRawQueryParameter((key, values) -> {
            if (key == null || key.isEmpty()) {
                return;
            }
            String encodedKey = SdkHttpUtils.urlEncode(key);
            java.util.List<String> encoded = new java.util.ArrayList<>(values.size());
            for (int i = 0; i < values.size(); i++) {
                String v = values.get(i);
                String enc = v == null ? "" : SdkHttpUtils.urlEncode(v);
                encoded.add(enc == null ? "" : enc);
            }
            java.util.Collections.sort(encoded);
            sorted.put(encodedKey, encoded);
        });
        SdkHttpUtils.flattenQueryParameters(out, sorted);
    }

    /**
     * Emit one canonical-header line per distinct name; comma-join multi-valued headers (consecutive in the sorted
     * strided buffer); skip ignored headers.
     */
    private static void appendCanonicalHeaders(V4SigningResources r, StringBuilder out) {
        int i = 0;
        while (i < r.headerCount) {
            String name = r.headers[i * 2];
            int next = i + 1;
            while (next < r.headerCount && name.equals(r.headers[next * 2])) {
                next++;
            }
            // Note: ignored headers were already filtered out of the strided buffer in collectSourceHeaders, so we
            // don't re-check here. Signer-managed headers are always emitted.
            out.append(name);
            out.append(':');
            for (int j = i; j < next; j++) {
                appendAndTrim(out, r.headers[j * 2 + 1]);
                out.append(',');
            }
            out.setLength(out.length() - 1);
            out.append('\n');
            i = next;
        }
    }

    /**
     * Trim leading/trailing whitespace and collapse interior runs of whitespace to a single space. Matches V2's
     * {@code V4CanonicalRequest.addAndTrim}.
     */
    private static void appendAndTrim(StringBuilder out, String value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        int len = value.length();
        // Fast path: no whitespace anywhere (the common case for SDK-emitted headers).
        boolean anyWhitespace = false;
        for (int i = 0; i < len; i++) {
            if (isWhitespace(value.charAt(i))) {
                anyWhitespace = true;
                break;
            }
        }
        if (!anyWhitespace) {
            out.append(value);
            return;
        }

        int start = 0;
        while (start < len && isWhitespace(value.charAt(start))) {
            start++;
        }
        int end = len;
        while (end > start && isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        boolean previousWhitespace = false;
        for (int i = start; i < end; i++) {
            char c = value.charAt(i);
            if (isWhitespace(c)) {
                if (!previousWhitespace) {
                    out.append(' ');
                    previousWhitespace = true;
                }
            } else {
                out.append(c);
                previousWhitespace = false;
            }
        }
    }

    private static boolean isWhitespace(char c) {
        // Matches V2: ' ', '\t', '\n', '\u000b', '\r', '\f'
        return c == ' ' || (c >= '\t' && c <= '\f');
    }

    /**
     * Format yyyyMMdd directly into the pooled builder.
     */
    private static String formatDate(LocalDateTime dt, StringBuilder sb) {
        sb.setLength(0);
        sb.append(dt.getYear());
        appendTwoDigit(sb, dt.getMonthValue());
        appendTwoDigit(sb, dt.getDayOfMonth());
        return sb.toString();
    }

    /**
     * Format yyyyMMdd'T'HHmmss'Z' directly. The date prefix is reused from {@link #formatDate}.
     */
    private static String formatDateTime(LocalDateTime dt, String dateStamp, StringBuilder sb) {
        sb.setLength(0);
        sb.append(dateStamp);
        sb.append('T');
        appendTwoDigit(sb, dt.getHour());
        appendTwoDigit(sb, dt.getMinute());
        appendTwoDigit(sb, dt.getSecond());
        sb.append('Z');
        return sb.toString();
    }

    private static void appendTwoDigit(StringBuilder sb, int value) {
        if (value < 10) {
            sb.append('0');
        }
        sb.append(value);
    }

    /**
     * Build the credential scope: {@code <date>/<region>/<service>/aws4_request}.
     */
    private static String buildScope(String dateStamp, String region, String service, StringBuilder sb) {
        sb.setLength(0);
        sb.append(dateStamp).append('/').append(region).append('/').append(service).append('/').append(AWS4_TERMINATOR);
        return sb.toString();
    }

    /**
     * Build the Authorization header value:
     * {@code AWS4-HMAC-SHA256 Credential=<accessKey>/<scope>, SignedHeaders=<list>, Signature=<hex>}.
     */
    private static String buildAuthorizationHeader(String accessKey, String scope, String signedHeaders,
                                                   String signature, StringBuilder sb) {
        sb.setLength(0);
        sb.append(AWS4_SIGNING_ALGORITHM)
          .append(" Credential=").append(accessKey).append('/').append(scope)
          .append(", SignedHeaders=").append(signedHeaders)
          .append(", Signature=").append(signature);
        return sb.toString();
    }

    /**
     * Look up or derive the SigV4 signing key for the given (secret, region, service) tuple. Hits the same shared
     * cache that {@link software.amazon.awssdk.http.auth.aws.internal.signer.util.SignerUtils} uses so cached keys
     * are reused across the fast and legacy paths. The cache holds raw {@code byte[]} keys; callers must treat them
     * as immutable.
     */
    private static byte[] deriveSigningKey(AwsCredentialsIdentity credentials, String region, String service,
                                           String dateStamp, Instant signingInstant, V4SigningResources r) {
        V4SigningKeyCache.CacheKey key = new V4SigningKeyCache.CacheKey(
            credentials.secretAccessKey(), region, service);
        byte[] cached = V4SigningKeyCache.sharedGet(key, signingInstant);
        if (cached != null) {
            return cached;
        }
        byte[] newKey = newSigningKey(credentials.secretAccessKey(), dateStamp, region, service, r);
        V4SigningKeyCache.sharedPut(key, newKey, signingInstant);
        return newKey;
    }

    /**
     * The four-step HMAC chain that derives the signing key. All four HMAC operations reuse the pooled
     * {@link V4SigningResources#sha256Mac}, with one {@link SecretKeySpec} allocation per step (unavoidable; HMAC's
     * key is supplied as a Key object).
     */
    private static byte[] newSigningKey(String secretKey, String dateStamp, String region, String service,
                                        V4SigningResources r) {
        byte[] kSecret = ("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8);
        byte[] kDate = hmac(dateStamp.getBytes(StandardCharsets.UTF_8), kSecret, r);
        byte[] kRegion = hmac(region.getBytes(StandardCharsets.UTF_8), kDate, r);
        byte[] kService = hmac(service.getBytes(StandardCharsets.UTF_8), kRegion, r);
        return hmac(AWS4_TERMINATOR.getBytes(StandardCharsets.UTF_8), kService, r);
    }

    /**
     * One HMAC-SHA256 step using the pooled Mac. Returns a fresh 32-byte array (small, intentional: signing key
     * derivation is rarely on the per-call hot path because the cache absorbs it).
     */
    private static byte[] hmac(byte[] data, byte[] key, V4SigningResources r) {
        try {
            r.sha256Mac.reset();
            r.sha256Mac.init(new SecretKeySpec(key, HMAC_SHA_256));
            return r.sha256Mac.doFinal(data);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Unable to derive SigV4 signing key: " + e.getMessage(), e);
        }
    }

    /**
     * Compute the signature: SHA-256(canonicalRequest) → string-to-sign → HMAC-SHA256(stringToSign, signingKey) →
     * lowercase hex.
     *
     * <p>The canonical request bytes have already been written into {@code r.canonicalRequestBytes} by
     * {@link #buildCanonicalRequest}. The string-to-sign is composed directly into {@code r.stringToSignBytes} via
     * {@link #writeAscii} and {@link #writeHex}, never going through {@code String.getBytes(UTF_8)}.
     */
    private static String computeSignature(byte[] canonicalRequest, int canonicalLen, String requestDateTime,
                                           String dateStamp, String region, String service, byte[] signingKey,
                                           V4SigningResources r) {
        try {
            r.sha256Digest.reset();
            r.sha256Digest.update(canonicalRequest, 0, canonicalLen);
            r.sha256Digest.digest(r.hashBytes, 0, r.hashBytes.length);
        } catch (DigestException e) {
            throw new RuntimeException("Unable to hash SigV4 canonical request: " + e.getMessage(), e);
        }

        // Layout of string-to-sign:
        //   AWS4-HMAC-SHA256\n<requestDateTime>\n<scope>\n<canonicalRequestHashHex>
        // scope is yyyyMMdd/region/service/aws4_request (3 slashes + AWS4_TERMINATOR(12 chars) = 15 + dateStamp(8)
        //   + region.length + service.length).
        // canonical request hash hex is 64 chars.
        int scopeLen = dateStamp.length() + region.length() + service.length() + AWS4_TERMINATOR.length() + 3;
        int stringToSignLen = AWS4_SIGNING_ALGORITHM.length() + 1
                              + requestDateTime.length() + 1
                              + scopeLen + 1
                              + 64;
        byte[] stringToSign = r.ensureStringToSignCapacity(stringToSignLen);

        int pos = 0;
        pos = writeAscii(AWS4_SIGNING_ALGORITHM, stringToSign, pos);
        stringToSign[pos++] = '\n';
        pos = writeAscii(requestDateTime, stringToSign, pos);
        stringToSign[pos++] = '\n';
        pos = writeAscii(dateStamp, stringToSign, pos);
        stringToSign[pos++] = '/';
        pos = writeAscii(region, stringToSign, pos);
        stringToSign[pos++] = '/';
        pos = writeAscii(service, stringToSign, pos);
        stringToSign[pos++] = '/';
        pos = writeAscii(AWS4_TERMINATOR, stringToSign, pos);
        stringToSign[pos++] = '\n';
        pos = writeHex(r.hashBytes, stringToSign, pos);

        try {
            r.sha256Mac.reset();
            r.sha256Mac.init(new SecretKeySpec(signingKey, HMAC_SHA_256));
            r.sha256Mac.update(stringToSign, 0, pos);
            r.sha256Mac.doFinal(r.signatureBytes, 0);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Unable to compute SigV4 signature: " + e.getMessage(), e);
        } catch (ShortBufferException e) {
            throw new RuntimeException("Internal SigV4 signature buffer too small", e);
        }

        byte[] hex = r.signatureHexBytes;
        writeHex(r.signatureBytes, hex, 0);
        return new String(hex, 0, hex.length, StandardCharsets.US_ASCII);
    }

    private static int writeAscii(String value, byte[] dst, int offset) {
        for (int i = 0; i < value.length(); i++) {
            dst[offset++] = (byte) value.charAt(i);
        }
        return offset;
    }

    private static int writeHex(byte[] src, byte[] dst, int offset) {
        for (int i = 0; i < src.length; i++) {
            byte b = src[i];
            dst[offset++] = HEX_DIGITS[(b >>> 4) & 0x0F];
            dst[offset++] = HEX_DIGITS[b & 0x0F];
        }
        return offset;
    }
}
