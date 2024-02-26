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

package software.amazon.awssdk.codegen.poet.auth.scheme;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.codegen.model.service.AuthType;
import software.amazon.awssdk.http.auth.aws.scheme.AwsV4AuthScheme;
import software.amazon.awssdk.utils.Lazy;

/**
 * Contains maps from all sigv4 based {@link AuthType} to {@link SigV4SignerDefaults} that we can then transform for use in
 * codegen.
 */
public final class AuthTypeToSigV4Default {

    public static final SigV4SignerDefaults SIGV4_DEFAULT = SigV4SignerDefaults
        .builder()
        .authType("v4")
        .schemeId(AwsV4AuthScheme.SCHEME_ID)
        .build();

    private static final Lazy<Map<String, SigV4SignerDefaults>> AUTH_TYPE_TO_DEFAULTS = new Lazy<>(
        () -> {
            Map<String, SigV4SignerDefaults> map = new LinkedHashMap<>();
            for (SigV4SignerDefaults sigv4FamilySignerConstants : knownAuthTypes()) {
                if (map.put(sigv4FamilySignerConstants.authType(), sigv4FamilySignerConstants) != null) {
                    throw new IllegalStateException("Duplicate key: " + sigv4FamilySignerConstants.authType());
                }
            }
            return map;
        });

    private AuthTypeToSigV4Default() {
    }

    /**
     * Returns a mapping from an auth-type name to a set of AWS sigV4 default values.The auth-type names are the same as the
     * {@link AuthType} enum values.
     *
     * @see SigV4SignerDefaults
     */
    public static Map<String, SigV4SignerDefaults> authTypeToDefaults() {
        return AUTH_TYPE_TO_DEFAULTS.getValue();
    }

    /**
     * Returns the list fo all known auth types to s3v4Defaults instances.
     *
     * @return
     */
    public static List<SigV4SignerDefaults> knownAuthTypes() {
        return Arrays.asList(
            sigv4Default(),
            s3Defaults(),
            s3v4Defaults(),
            sigv4UnsignedPayload()
        );
    }

    /**
     * Set of default signer defaults. None is set by default.
     */
    private static SigV4SignerDefaults sigv4Default() {
        return SIGV4_DEFAULT;
    }

    /**
     * Set of default signer defaults for S3. Sets the following defaults signer properties
     *
     * <ul>
     *     <li>{@code doubleUrlEncode(false)}
     *     <li>{@code normalizePath(false)}
     *     <li>{@code payloadSigningEnabled(false)}
     * </ul>
     * <p>
     * Also overrides for the following operations
     *
     * <ul>
     *     <li>{@code UploadParts} Sets the defaults and also {@code chunkEncodingEnabled(true)}</li>
     *     <li>{@code PutObject} Sets the defaults and also {@code chunkEncodingEnabled(true)}</li>
     * </ul>
     */
    private static SigV4SignerDefaults s3Defaults() {
        return sigv4Default()
            .toBuilder()
            .authType("s3")
            .service("S3")
            .doubleUrlEncode(Boolean.FALSE)
            .normalizePath(Boolean.FALSE)
            .payloadSigningEnabled(Boolean.FALSE)
            .putOperation("UploadPart",
                          sigv4Default()
                              .toBuilder()
                              // Default S3 signer properties
                              .doubleUrlEncode(Boolean.FALSE)
                              .normalizePath(Boolean.FALSE)
                              .payloadSigningEnabled(Boolean.FALSE)
                              // Including chunkEncodingEnabled TRUE
                              .chunkEncodingEnabled(Boolean.TRUE)
                              .build())
            .putOperation("PutObject",
                          sigv4Default()
                              .toBuilder()
                              // Default S3 signer properties
                              .doubleUrlEncode(Boolean.FALSE)
                              .normalizePath(Boolean.FALSE)
                              .payloadSigningEnabled(Boolean.FALSE)
                              // Including chunkEncodingEnabled TRUE
                              .chunkEncodingEnabled(Boolean.TRUE)
                              .build())
            .build();
    }


    /**
     * Set of default signer defaults for auth-type s3v4. Currently only used by S3Control.
     */
    private static SigV4SignerDefaults s3v4Defaults() {
        return sigv4Default().toBuilder()
                             .authType("s3v4")
                             .doubleUrlEncode(false)
                             .normalizePath(false)
                             .build();
    }


    /**
     * Set of default signer defaults for auth-type s3v4. Currently only used by disable payload signing for some operations. Sets
     * the following default signer property
     *
     * <ul>
     *     <li>{@code payloadSigningEnabled(false)}
     * </ul>
     */
    private static SigV4SignerDefaults sigv4UnsignedPayload() {
        return sigv4Default().toBuilder()
                             .authType("v4-unsigned-body")
                             .payloadSigningEnabled(false)
                             .build();
    }
}
