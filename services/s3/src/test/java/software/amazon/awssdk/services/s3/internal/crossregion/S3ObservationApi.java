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

package software.amazon.awssdk.services.s3.internal.crossregion;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

public class S3ObservationApi {

    @Test
    void s3Observation(){
        // Create registries
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        // Register handler to convert observations to metrics
        observationRegistry.observationConfig().observationHandler(
            new io.micrometer.core.instrument.observation.DefaultMeterObservationHandler(meterRegistry)
        );

        // Register our convention
        observationRegistry.observationConfig().observationConvention(new S3OperationConvention());


        // Create the S3 client with observation support
        S3Client baseClient = S3Client.builder().region(Region.US_WEST_2).build();


        S3CrossRegionSyncClient s3Client = new S3CrossRegionSyncClient(baseClient, observationRegistry);

        try {
            s3Client.listObjects(l -> l.bucket("metric-analysis"));

        }catch (Exception e){

        }
        // Use the client

        // Print metrics
        System.out.println("--- Metrics ---");
        meterRegistry.getMeters().forEach(meter -> {
            System.out.println(meter.getId() + " - " + meter.measure());
        });
    }


}
