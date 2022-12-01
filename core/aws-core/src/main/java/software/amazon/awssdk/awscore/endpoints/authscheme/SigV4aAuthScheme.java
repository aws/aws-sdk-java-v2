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

package software.amazon.awssdk.awscore.endpoints.authscheme;

import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.annotations.SdkProtectedApi;

/**
 * A Signature Version 4A  authentication scheme.
 */
@SdkProtectedApi
public final class SigV4aAuthScheme implements EndpointAuthScheme {
    private final String signingName;
    private final List<String> signingRegionSet;
    private final boolean disableDoubleEncoding;

    private SigV4aAuthScheme(Builder b) {
        this.signingName = b.signingName;
        this.signingRegionSet = b.signingRegionSet;
        this.disableDoubleEncoding = b.disableDoubleEncoding == null ? false : b.disableDoubleEncoding;
    }

    public String signingName() {
        return signingName;
    }

    public boolean disableDoubleEncoding() {
        return disableDoubleEncoding;
    }

    public List<String> signingRegionSet() {
        return signingRegionSet;
    }

    @Override
    public String name() {
        return "sigv4a";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SigV4aAuthScheme that = (SigV4aAuthScheme) o;

        if (disableDoubleEncoding != that.disableDoubleEncoding) {
            return false;
        }
        if (signingName != null ? !signingName.equals(that.signingName) : that.signingName != null) {
            return false;
        }
        return signingRegionSet != null ? signingRegionSet.equals(that.signingRegionSet) : that.signingRegionSet == null;
    }

    @Override
    public int hashCode() {
        int result = signingName != null ? signingName.hashCode() : 0;
        result = 31 * result + (signingRegionSet != null ? signingRegionSet.hashCode() : 0);
        result = 31 * result + (disableDoubleEncoding ? 1 : 0);
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<String> signingRegionSet = new ArrayList<>();
        private String signingName;
        private Boolean disableDoubleEncoding;

        public Builder addSigningRegion(String signingRegion) {
            this.signingRegionSet.add(signingRegion);
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

        public SigV4aAuthScheme build() {
            return new SigV4aAuthScheme(this);
        }
    }
}
