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

package software.amazon.awssdk.services.s3.endpoints.authscheme;

import software.amazon.awssdk.annotations.NotThreadSafe;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;
import software.amazon.awssdk.awscore.endpoints.authscheme.SigV4AuthScheme;
import software.amazon.awssdk.core.exception.SdkClientException;

/**
 * Builds an {@link EndpointAuthScheme} whose scheme name is only known at runtime.
 *
 * <p>Endpoint rulesets normally declare the auth scheme name as a string literal, which lets codegen emit a direct call to the
 * matching concrete builder (for example {@code SigV4AuthScheme.builder()}). S3 is the one exception: its BDD-based ruleset
 * merges two otherwise identical results whose auth scheme names differ, lifting the difference into a runtime conditional:
 *
 * {@snippet :
 * _s3e_auth = ite(coalesce(DisableS3ExpressSessionAuth, false), "sigv4", "sigv4-s3express");
 * }
 *
 * <p>The merged result then refers to the name indirectly, so the concrete type cannot be selected at codegen time. This factory
 * collects the auth scheme properties up front and defers the type selection to {@link #create(String)}.
 *
 * <p>Only {@code sigv4} and {@code sigv4-s3express} are supported, since those are the only names the S3 ruleset can produce
 * here. The two schemes share the same property set ({@code signingName}, {@code signingRegion},
 * {@code disableDoubleEncoding}), so the properties are modelled directly rather than generically. Any other name is a
 * programming error and fails fast.
 *
 * <p>Instances are mutable and are not safe for concurrent use. Generated endpoint providers create a fresh instance per
 * resolution, so each instance is confined to a single resolution.
 */
@SdkProtectedApi
@NotThreadSafe
public final class DynamicEndpointAuthSchemeFactory {
    private static final String SIGV4_NAME = "sigv4";
    private static final String S3EXPRESS_NAME = "sigv4-s3express";

    private String signingName;
    private String signingRegion;
    private Boolean disableDoubleEncoding;

    private DynamicEndpointAuthSchemeFactory() {
    }

    public static DynamicEndpointAuthSchemeFactory builder() {
        return new DynamicEndpointAuthSchemeFactory();
    }

    public DynamicEndpointAuthSchemeFactory signingName(String signingName) {
        this.signingName = signingName;
        return this;
    }

    public DynamicEndpointAuthSchemeFactory signingRegion(String signingRegion) {
        this.signingRegion = signingRegion;
        return this;
    }

    public DynamicEndpointAuthSchemeFactory disableDoubleEncoding(Boolean disableDoubleEncoding) {
        this.disableDoubleEncoding = disableDoubleEncoding;
        return this;
    }

    /**
     * Create the endpoint auth scheme matching {@code name}, applying the properties collected on this factory.
     *
     * <p>Unset properties are passed through as {@code null}, which the concrete auth schemes treat as "not set" in the same way
     * they do when codegen emits a direct builder call.
     *
     * @param name the auth scheme name; must be {@code sigv4} or {@code sigv4-s3express}
     * @return the constructed endpoint auth scheme
     * @throws SdkClientException if {@code name} is not a supported auth scheme name, including when it is {@code null}
     */
    public EndpointAuthScheme create(String name) {
        if (SIGV4_NAME.equals(name)) {
            return SigV4AuthScheme.builder()
                                  .signingName(signingName)
                                  .signingRegion(signingRegion)
                                  .disableDoubleEncoding(disableDoubleEncoding)
                                  .build();
        }
        if (S3EXPRESS_NAME.equals(name)) {
            return S3ExpressEndpointAuthScheme.builder()
                                              .signingName(signingName)
                                              .signingRegion(signingRegion)
                                              .disableDoubleEncoding(disableDoubleEncoding)
                                              .build();
        }
        throw SdkClientException.create("Unsupported dynamic endpoint auth scheme name: '" + name + "'. Expected '"
                                        + SIGV4_NAME + "' or '" + S3EXPRESS_NAME + "'.");
    }
}
