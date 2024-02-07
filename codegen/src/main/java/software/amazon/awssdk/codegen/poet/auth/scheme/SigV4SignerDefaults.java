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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import software.amazon.awssdk.utils.Validate;

/**
 * Tracks a set of explicitly enabled signer properties for the family of AWS SigV4 signers. The currently supported attributes
 * are {@code doubleUrlEncode}, {@code normalizePath}, {@code payloadSigningEnabled}, {@code chunkEncodingEnabled}. If the
 * value is null then is not overridden. An auth type can also represent a service-wide set of defaults.
 */
public final class SigV4SignerDefaults {
    private final String service;
    private final String authType;
    private final String schemeId;
    private final Boolean doubleUrlEncode;
    private final Boolean normalizePath;
    private final Boolean payloadSigningEnabled;
    private final Boolean chunkEncodingEnabled;
    private final Map<String, SigV4SignerDefaults> operations;

    private SigV4SignerDefaults(Builder builder) {
        this.service = builder.service;
        this.authType = Validate.notNull(builder.authType, "authType");
        this.schemeId = Validate.notNull(builder.schemeId, "schemeId");
        this.doubleUrlEncode = builder.doubleUrlEncode;
        this.normalizePath = builder.normalizePath;
        this.payloadSigningEnabled = builder.payloadSigningEnabled;
        this.chunkEncodingEnabled = builder.chunkEncodingEnabled;
        this.operations = Collections.unmodifiableMap(new HashMap<>(builder.operations));
    }

    public boolean isServiceOverrideAuthScheme() {
        return service != null;
    }

    public String service() {
        return service;
    }

    public String authType() {
        return authType;
    }

    public String schemeId() {
        return schemeId;
    }

    public Boolean doubleUrlEncode() {
        return doubleUrlEncode;
    }

    public Boolean normalizePath() {
        return normalizePath;
    }

    public Boolean payloadSigningEnabled() {
        return payloadSigningEnabled;
    }

    public Boolean chunkEncodingEnabled() {
        return chunkEncodingEnabled;
    }

    public Map<String, SigV4SignerDefaults> operations() {
        return operations;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SigV4SignerDefaults defaults = (SigV4SignerDefaults) o;

        if (!Objects.equals(service, defaults.service)) {
            return false;
        }
        if (!authType.equals(defaults.authType)) {
            return false;
        }
        if (!schemeId.equals(defaults.schemeId)) {
            return false;
        }
        if (!Objects.equals(doubleUrlEncode, defaults.doubleUrlEncode)) {
            return false;
        }
        if (!Objects.equals(normalizePath, defaults.normalizePath)) {
            return false;
        }
        if (!Objects.equals(payloadSigningEnabled, defaults.payloadSigningEnabled)) {
            return false;
        }
        if (!Objects.equals(chunkEncodingEnabled, defaults.chunkEncodingEnabled)) {
            return false;
        }
        return operations.equals(defaults.operations);
    }

    @Override
    public int hashCode() {
        int result = service != null ? service.hashCode() : 0;
        result = 31 * result + authType.hashCode();
        result = 31 * result + schemeId.hashCode();
        result = 31 * result + (doubleUrlEncode != null ? doubleUrlEncode.hashCode() : 0);
        result = 31 * result + (normalizePath != null ? normalizePath.hashCode() : 0);
        result = 31 * result + (payloadSigningEnabled != null ? payloadSigningEnabled.hashCode() : 0);
        result = 31 * result + (chunkEncodingEnabled != null ? chunkEncodingEnabled.hashCode() : 0);
        result = 31 * result + operations.hashCode();
        return result;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String authType;
        private String service;
        private String schemeId;
        private Boolean doubleUrlEncode;
        private Boolean normalizePath;
        private Boolean payloadSigningEnabled;
        private Boolean chunkEncodingEnabled;

        private Map<String, SigV4SignerDefaults> operations = new HashMap<>();

        public Builder() {
        }

        public Builder(SigV4SignerDefaults other) {
            this.service = other.service;
            this.authType = Validate.notNull(other.authType, "name");
            this.schemeId = Validate.notNull(other.schemeId, "schemeId");
            this.doubleUrlEncode = other.doubleUrlEncode;
            this.normalizePath = other.normalizePath;
            this.payloadSigningEnabled = other.payloadSigningEnabled;
            this.chunkEncodingEnabled = other.chunkEncodingEnabled;
            this.operations.putAll(other.operations);
        }

        public String service() {
            return service;
        }

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public String authType() {
            return authType;
        }

        public Builder authType(String authType) {
            this.authType = authType;
            return this;
        }

        public String schemeId() {
            return schemeId;
        }

        public Builder schemeId(String schemeId) {
            this.schemeId = schemeId;
            return this;
        }

        public Boolean doubleUrlEncode() {
            return doubleUrlEncode;
        }

        public Builder doubleUrlEncode(Boolean doubleUrlEncode) {
            this.doubleUrlEncode = doubleUrlEncode;
            return this;
        }

        public Boolean normalizePath() {
            return normalizePath;
        }

        public Builder normalizePath(Boolean normalizePath) {
            this.normalizePath = normalizePath;
            return this;
        }

        public Boolean payloadSigningEnabled() {
            return payloadSigningEnabled;
        }

        public Builder payloadSigningEnabled(Boolean payloadSigningEnabled) {
            this.payloadSigningEnabled = payloadSigningEnabled;
            return this;
        }

        public Boolean chunkEncodingEnabled() {
            return chunkEncodingEnabled;
        }

        public Builder chunkEncodingEnabled(Boolean chunkEncodingEnabled) {
            this.chunkEncodingEnabled = chunkEncodingEnabled;
            return this;
        }

        public Map<String, SigV4SignerDefaults> operations() {
            return operations;
        }

        public Builder putOperation(String name, SigV4SignerDefaults constants) {
            this.operations.put(name, constants);
            return this;
        }

        public SigV4SignerDefaults build() {
            return new SigV4SignerDefaults(this);
        }
    }
}
