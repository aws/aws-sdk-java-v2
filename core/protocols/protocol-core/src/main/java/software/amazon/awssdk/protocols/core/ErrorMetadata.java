/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.protocols.core;

import java.util.function.Supplier;
import software.amazon.awssdk.core.SdkPojo;

/**
 * Metadata needed to unmarshall the exceptions.
 */
public class ErrorMetadata {

    private final Supplier<SdkPojo> exceptionBuilderSupplier;
    private final Integer httpStatusCode;

    public ErrorMetadata(Builder builder) {
        this.exceptionBuilderSupplier = builder.exceptionBuilderSupplier;
        this.httpStatusCode = builder.httpStatusCode;
    }

    /**
     * Return the Supplier to get the builder class for the exception.
     */
    public Supplier<SdkPojo> exceptionBuilderSupplier() {
        return exceptionBuilderSupplier;
    }

    /**
     * Returns the http status code for the exception.
     * For modeled exceptions, this value is populated from the c2j model
     */
    public Integer httpStatusCode() {
        return httpStatusCode;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ErrorMetadata}
     */
    public static final class Builder {
        private Supplier<SdkPojo> exceptionBuilderSupplier;
        private Integer httpStatusCode;

        public Builder exceptionBuilderSupplier(Supplier<SdkPojo> exceptionBuilderSupplier) {
            this.exceptionBuilderSupplier = exceptionBuilderSupplier;
            return this;
        }

        public Builder httpStatusCode(Integer httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
            return this;
        }

        public ErrorMetadata build() {
            return new ErrorMetadata(this);
        }
    }
}
