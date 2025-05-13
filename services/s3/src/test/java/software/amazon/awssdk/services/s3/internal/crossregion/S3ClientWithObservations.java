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
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.services.s3.S3Client;

public class S3ClientWithObservations {
    public static void main(String[] args) {
        ObservationRegistry observationRegistry = ObservationRegistry.create();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        observationRegistry.observationConfig()
                           .observationHandler(new io.micrometer.core.instrument.observation.DefaultMeterObservationHandler(meterRegistry))
                           .observationConvention(new ObservingExecutionInterceptor.AwsSdkObservationConvention());

        S3Client s3Client = S3Client.builder()
                                    .addPlugin(S3MicrometerPlugin.create(observationRegistry))
                                    .build();

        try{
            s3Client.listBuckets();

        }catch (Exception e){

        }
        try{
            s3Client.listObjects(l -> l.bucket("metric-analysis"));

        }catch (Exception e){

        }
        try{

        }catch (Exception e){
            s3Client.listObjects(l -> l.bucket("joviegas-test-file-stuck"));

        }


        System.out.println("--- Metrics ---");
        meterRegistry.getMeters().forEach(meter ->
                                              System.out.println(meter.getId() + " - " + meter.measure()));
    }
}
