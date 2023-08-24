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

package software.amazon.awssdk.http.auth.spi;

import java.time.Clock;
import software.amazon.awssdk.annotations.SdkPublicApi;

/**
 * Signer properties that are common to every possible current and future signer. See the individual signer interfaces
 * for properties that are not universal across signers.
 */
@SdkPublicApi
public final class SharedSignerProperty {
    /**
     * A {@link Clock} to be used at the time of signing. This property defaults to the time at which signing occurs.
     */
    public static final SignerProperty<Clock> SIGNING_CLOCK = SignerProperty.create(Clock.class, "SigningClock");

    private SharedSignerProperty() {
    }
}
