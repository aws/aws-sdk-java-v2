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

package software.amazon.awssdk.s3.wiretap.client;

import java.util.List;
import lombok.Getter;
import lombok.experimental.Accessors;
import software.amazon.awssdk.s3.wiretap.client.feature.Feature;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.utils.SdkAutoCloseable;

@Accessors(fluent = true)
public final class ClientSuccessOrFailure implements SdkAutoCloseable {

    public enum Outcome {
        SUCCESS,
        BUILD_FAILURE,
        NOT_APPLICABLE
    }

    @Getter
    private final Outcome outcome;
    @Getter
    private final ClientConfig config;
    private final S3Client client;
    private final ClientBuildFailed failure;
    @Getter
    private final List<Feature<?>> inapplicableFeatures;

    private ClientSuccessOrFailure(Outcome outcome, ClientConfig config, S3Client client,
            ClientBuildFailed failure, List<Feature<?>> inapplicableFeatures) {
        this.outcome = outcome;
        this.config = config;
        this.client = client;
        this.failure = failure;
        this.inapplicableFeatures = List.copyOf(inapplicableFeatures);
    }

    public static ClientSuccessOrFailure success(S3Client client, ClientConfig config) {
        return new ClientSuccessOrFailure(Outcome.SUCCESS, config, client, null, List.of());
    }

    public static ClientSuccessOrFailure failure(ClientBuildFailed failure) {
        return new ClientSuccessOrFailure(Outcome.BUILD_FAILURE, failure.config(), null, failure, List.of());
    }

    public static ClientSuccessOrFailure notApplicable(ClientConfig config, List<Feature<?>> inapplicable) {
        return new ClientSuccessOrFailure(Outcome.NOT_APPLICABLE, config, null, null, inapplicable);
    }

    public <T, F extends Feature<T>> T value(Class<F> featureType) {
        return config.value(featureType);
    }

    public boolean succeeded() {
        return outcome == Outcome.SUCCESS;
    }

    public boolean failed() {
        return outcome == Outcome.BUILD_FAILURE;
    }

    public boolean notApplicable() {
        return outcome == Outcome.NOT_APPLICABLE;
    }

    public S3Client client() {
        if (client == null) {
            throw new IllegalStateException("no client for config [" + config.describe()
                    + "]: outcome was " + outcome
                    + (failure != null ? " (" + failure.failure() + ")" : ""));
        }
        return client;
    }

    public ClientBuildFailed failure() {
        if (failure == null) {
            throw new IllegalStateException("no build failure for config [" + config.describe()
                    + "]: outcome was " + outcome);
        }
        return failure;
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public String toString() {
        String detail = switch (outcome) {
            case SUCCESS -> "success";
            case BUILD_FAILURE -> "failure=" + failure.failure();
            case NOT_APPLICABLE -> "notApplicable=" + inapplicableFeatures.stream()
                    .map(Feature::name).toList();
        };
        return "ClientSuccessOrFailure[" + config.describe() + ": " + detail + "]";
    }
}
