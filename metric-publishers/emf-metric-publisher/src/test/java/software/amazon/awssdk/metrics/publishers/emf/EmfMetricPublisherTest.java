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

package software.amazon.awssdk.metrics.publishers.emf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import com.fasterxml.jackson.core.JsonGenerator;
import software.amazon.awssdk.core.metrics.CoreMetric;
import software.amazon.awssdk.http.HttpMetric;
import software.amazon.awssdk.metrics.MetricCategory;
import software.amazon.awssdk.metrics.MetricCollector;
import software.amazon.awssdk.metrics.MetricRecord;
import software.amazon.awssdk.metrics.SdkMetric;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


public class EmfMetricPublisherTest {

    private EmfMetricPublisher.Builder PublisherBuilder;


    @Mock
    private JsonGenerator jsonGenerator;

    @Mock
    private MetricRecord metricRecord;

    @BeforeEach
    void setUp() {
        PublisherBuilder = EmfMetricPublisher.builder();
    }

    @Test
    void testconvertMetricCollectionToEMF_EmptyCollection(){
        EmfMetricPublisher publisher = PublisherBuilder.build();

        Object emf = publisher.convertMetricCollectionToEMF(MetricCollector.create("test").collect());
        System.out.println(emf);
    }

    @Test
    void testconvertMetricCollectionToEMF_SingleMetric(){
        EmfMetricPublisher publisher = PublisherBuilder.build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        Object emf = publisher.convertMetricCollectionToEMF(metricCollector.collect());
        System.out.println(emf);
    }

    @Test
    void testconvertMetricCollectionToEMF_MultipleMetrics(){
        EmfMetricPublisher publisher = PublisherBuilder.dimensions(CoreMetric.SERVICE_ID)
                                                       .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        metricCollector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, true);
        metricCollector.reportMetric(CoreMetric.SIGNING_DURATION, java.time.Duration.ofMillis(100));
        Object emf = publisher.convertMetricCollectionToEMF(metricCollector.collect());
        System.out.println(emf);
    }

    @Test
    void testconvertMetricCollectionToEMF_Dimensions(){
        EmfMetricPublisher publisher = PublisherBuilder.dimensions(CoreMetric.SERVICE_ID)
                                                       .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(CoreMetric.SERVICE_ID, "ServiceId1234");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        Object emf = publisher.convertMetricCollectionToEMF(metricCollector.collect());
        System.out.println(emf);
    }

    @Test
    void testconvertMetricCollectionToEMF_nonExistDimensions(){
        EmfMetricPublisher publisher = PublisherBuilder.dimensions(CoreMetric.SERVICE_ID)
                                                       .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        //metricCollector.reportMetric(CoreMetric.SERVICE_ID, "ServiceId1234");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        Object emf = publisher.convertMetricCollectionToEMF(metricCollector.collect());
        System.out.println(emf);
    }

    @Test
    void testconvertMetricCollectionToEMF_extraDimensions(){
        EmfMetricPublisher publisher = PublisherBuilder.build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(CoreMetric.SERVICE_ID, "ServiceId1234");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        Object emf = publisher.convertMetricCollectionToEMF(metricCollector.collect());
        System.out.println(emf);
    }

    @Test
    void testconvertMetricCollectionToEMF_metricCatagory(){
        EmfMetricPublisher publisher = PublisherBuilder.metricCategories(MetricCategory.HTTP_CLIENT)
                                                       .build();
        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(CoreMetric.API_CALL_SUCCESSFUL, true);
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);
        Object emf = publisher.convertMetricCollectionToEMF(metricCollector.collect());
        System.out.println(emf);
    }


    @Test
    void testconvertMetricCollectionToEMF_ChildCollections(){
        EmfMetricPublisher publisher = PublisherBuilder.dimensions(CoreMetric.SERVICE_ID)
                                                       .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(CoreMetric.SERVICE_ID, "ServiceId1234");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);

        MetricCollector childMetricCollector = metricCollector.createChild("child");
        childMetricCollector.reportMetric(HttpMetric.HTTP_STATUS_CODE, 404);
        Object emf = publisher.convertMetricCollectionToEMF(metricCollector.collect());
        System.out.println(emf);
    }

    @Test
    void testconvertMetricCollectionToEMF_MultiChildCollections(){
        EmfMetricPublisher publisher = PublisherBuilder.dimensions(CoreMetric.SERVICE_ID)
                                                       .build();

        MetricCollector metricCollector = MetricCollector.create("test");
        metricCollector.reportMetric(CoreMetric.SERVICE_ID, "ServiceId1234");
        metricCollector.reportMetric(HttpMetric.AVAILABLE_CONCURRENCY, 5);

        MetricCollector childMetricCollector1 = metricCollector.createChild("child1");
        childMetricCollector1.reportMetric(CoreMetric.SIGNING_DURATION, java.time.Duration.ofMillis(100));
        MetricCollector childMetricCollector2 = metricCollector.createChild("child2");
        childMetricCollector2.reportMetric(CoreMetric.SIGNING_DURATION, java.time.Duration.ofMillis(200));
        Object emf = publisher.convertMetricCollectionToEMF(metricCollector.collect());
        System.out.println(emf);
    }


}
