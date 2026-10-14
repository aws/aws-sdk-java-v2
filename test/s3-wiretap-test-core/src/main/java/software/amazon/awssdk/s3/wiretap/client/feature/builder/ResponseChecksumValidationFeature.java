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

package software.amazon.awssdk.s3.wiretap.client.feature.builder;

import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.s3.wiretap.client.feature.AbstractFeature;
import software.amazon.awssdk.s3.wiretap.client.feature.Category;

public final class ResponseChecksumValidationFeature extends AbstractFeature<ResponseChecksumValidation> {

    public ResponseChecksumValidationFeature() {
        super("checksumValidate", ResponseChecksumValidation.class);
        onSync((b, v) -> b.client().responseChecksumValidation(v));
        onAsync((b, v) -> b.asyncClient().responseChecksumValidation(v));
        onCrt((b, v) -> b.crtClient().responseChecksumValidation(v));
    }

    @Override
    public Category<ResponseChecksumValidation> defaultCategory(Class<?> paramType) {
        return new Unset();
    }

    public static final class WhenSupported implements Category<ResponseChecksumValidation> {
        @Override
        public ResponseChecksumValidation value() {
            return ResponseChecksumValidation.WHEN_SUPPORTED;
        }
    }

    public static final class WhenRequired implements Category<ResponseChecksumValidation> {
        @Override
        public ResponseChecksumValidation value() {
            return ResponseChecksumValidation.WHEN_REQUIRED;
        }
    }

    public static final class Unset implements Category<ResponseChecksumValidation> {
        @Override
        public ResponseChecksumValidation value() {
            return null;
        }
    }
}
