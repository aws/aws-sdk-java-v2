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

package software.amazon.awssdk.services.s3.micro;

import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;

import io.micrometer.common.KeyValues;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import software.amazon.awssdk.services.s3.S3Client;

public class MicrometerObservationDemo {

    public static void main(String[] args) {

        S3Client s3Client = S3Client.create();
        // Create registries
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        // Register the meter registry with the observation registry
        observationRegistry.observationConfig().observationHandler(
            new io.micrometer.core.instrument.observation.DefaultMeterObservationHandler(meterRegistry)
        );

        // Example 1: Basic observation
        Observation basicObservation = Observation.createNotStarted("basic.operation", observationRegistry)
                                                  .lowCardinalityKeyValue("service", "demo-service")
                                                  .highCardinalityKeyValue("userId", "user-123")
                                                  .contextualName("Basic Operation");

        basicObservation.observe(() -> {
            System.out.println("Executing basic operation...");
            try {
                s3Client.listBuckets();
            } catch (Exception e) {
                Thread.currentThread().interrupt();
            }
        });

        // Example 2: Observation with scopes
        Observation complexObservation = Observation.start("complex.operation", observationRegistry);

        try (Observation.Scope scope = complexObservation.openScope()) {
            System.out.println("Starting complex operation...");

            // Add tags during execution
            complexObservation.highCardinalityKeyValue("requestId", "req-456");

            try {
                Thread.sleep(150); // Simulate work

                // Nested observation - CORRECTED
                Observation nestedObservation = Observation.createNotStarted("nested.operation", observationRegistry)
                                                           .parentObservation(complexObservation);

                nestedObservation.observe(() -> {
                    System.out.println("Executing nested operation...");
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });

            } catch (InterruptedException e) {
                // Record error
                complexObservation.error(e);
                Thread.currentThread().interrupt();
            }
        } finally {
            complexObservation.stop();
        }

        // Print collected metrics
        System.out.println("\n--- Collected Metrics ---");
        meterRegistry.getMeters().forEach(meter -> {
            System.out.println(meter.getId() + " - " + meter.measure());
        });
    }

    // Example of using the @Observed annotation (requires AOP setup in real applications)
    @Observed(name = "annotated.method",
        contextualName = "annotatedMethod",
        lowCardinalityKeyValues = "service=demo-service")
    public void annotatedMethod(String input) {
        System.out.println("Processing: " + input);
    }
}

