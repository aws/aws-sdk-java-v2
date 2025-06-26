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

package software.amazon.awssdk.codegen.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown during code generation to signal that the model is invalid.
 */
public class ModelInvalidException extends RuntimeException {
    private final List<ValidationEntry> validationEntries;

    private ModelInvalidException(Builder b) {
        super("Validation failed with the following errors: " + b.validationEntries);
        this.validationEntries = Collections.unmodifiableList(new ArrayList<>(b.validationEntries));
    }

    public List<ValidationEntry> validationEntries() {
        return validationEntries;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<ValidationEntry> validationEntries;

        public Builder validationEntries(List<ValidationEntry> validationEntries) {
            if (validationEntries == null) {
                this.validationEntries = Collections.emptyList();
            } else {
                this.validationEntries = validationEntries;
            }

            return this;
        }

        public ModelInvalidException build() {
            return new ModelInvalidException(this);
        }
    }
}
