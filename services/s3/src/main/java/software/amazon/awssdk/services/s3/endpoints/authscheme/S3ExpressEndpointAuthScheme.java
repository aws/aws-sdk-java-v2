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

import java.util.Objects;
import software.amazon.awssdk.annotations.SdkProtectedApi;
import software.amazon.awssdk.awscore.endpoints.authscheme.EndpointAuthScheme;

/**
 * A Signature Version 4 authentication scheme.
 */
@SdkProtectedApi
public final class S3ExpressEndpointAuthScheme implements EndpointAuthScheme {
    private final String signingRegion;
    private final String signingName;
    private final Boolean disableDoubleEncoding;

    private S3ExpressEndpointAuthScheme(Builder b) {
        this.signingRegion = b.signingRegion;
        this.signingName = b.signingName;
        this.disableDoubleEncoding = b.disableDoubleEncoding;
    }

    @Override
    public String name() {
        return "sigv4-s3express";
    }

    @Override
    public String schemeId() {
        return "aws.auth#sigv4-s3express";
    }

    public String signingRegion() {
        return signingRegion;
    }

    public String signingName() {
        return signingName;
    }

    public boolean disableDoubleEncoding() {
        return disableDoubleEncoding == null ? false : disableDoubleEncoding;
    }

    public boolean isDisableDoubleEncodingSet() {
        return disableDoubleEncoding != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        S3ExpressEndpointAuthScheme that = (S3ExpressEndpointAuthScheme) o;

        if (!Objects.equals(disableDoubleEncoding, that.disableDoubleEncoding)) {
            return false;
        }
        if (signingRegion != null ? !signingRegion.equals(that.signingRegion) : that.signingRegion != null) {
            return false;
        }
        return signingName != null ? signingName.equals(that.signingName) : that.signingName == null;
    }

    @Override
    public int hashCode() {
        int result = signingRegion != null ? signingRegion.hashCode() : 0;
        result = 31 * result + (signingName != null ? signingName.hashCode() : 0);
        result = 31 * result + (disableDoubleEncoding ? 1 : 0);
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String signingRegion;
        private String signingName;
        private Boolean disableDoubleEncoding;

        public Builder signingRegion(String signingRegion) {
            this.signingRegion = signingRegion;
            return this;
        }

        public Builder signingName(String signingName) {
            this.signingName = signingName;
            return this;
        }

        public Builder disableDoubleEncoding(Boolean disableDoubleEncoding) {
            this.disableDoubleEncoding = disableDoubleEncoding;
            return this;
        }

        public S3ExpressEndpointAuthScheme build() {
            return new S3ExpressEndpointAuthScheme(this);
        }
    }
}
