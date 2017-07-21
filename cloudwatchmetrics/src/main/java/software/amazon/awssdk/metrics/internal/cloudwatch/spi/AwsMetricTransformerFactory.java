/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.awssdk.metrics.internal.cloudwatch.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service specific metric transformer factory.
 */
public enum AwsMetricTransformerFactory {
    DynamoDb;

    public static final String DEFAULT_METRIC_TRANSFORM_PROVIDER_PACKAGE =
            "software.amazon.awssdk.metrics.internal.cloudwatch.transform";
    private static final String REQUEST_TRANSFORMER_CLASSNAME_SUFFIX = "RequestMetricTransformer";
    public static volatile String transformerPackage = DEFAULT_METRIC_TRANSFORM_PROVIDER_PACKAGE;
    /**
     * By default, the transformer class for each AWS specific service is
     * assumed to reside in the Java package
     * {@link #DEFAULT_METRIC_TRANSFORM_PROVIDER_PACKAGE} and follow the naming
     * convention of &lt;AwsPrefix>MetricTransformer. The "AwsPrefix" is the
     * same as the enum literal name. Since each service specific request metric
     * transformer internally contains static reference to service specific
     * classes, this dynamic class loading mechansim allows service specific
     * transformers to be skipped in case some service specific class files are
     * absent in the classpath.
     */
    private volatile RequestMetricTransformer requestMetricTransformer;

    public static String getTransformerPackage() {
        return transformerPackage;
    }

    public static void setTransformerPackage(
            String transformPackage) {
        if (transformPackage == null) {
            throw new IllegalArgumentException();
        }
        AwsMetricTransformerFactory.transformerPackage = transformPackage;
    }

    /**
     * Returns the fully qualified class name of the request metric
     * transformer, given the specific AWS prefix.
     */
    public static String buildRequestMetricTransformerFqcn(String awsPrefix, String packageName) {
        return packageName + "."
               + awsPrefix + REQUEST_TRANSFORMER_CLASSNAME_SUFFIX
                ;
    }

    /**
     * @param fqcn fully qualified class name.
     */
    private RequestMetricTransformer loadRequestMetricTransformer(String fqcn) {
        Logger log = LoggerFactory.getLogger(AwsMetricTransformerFactory.class);
        if (log.isDebugEnabled()) {
            log.debug("Loading " + fqcn);
        }
        try {
            Class<?> c = Class.forName(fqcn);
            return (RequestMetricTransformer) c.newInstance();
        } catch (Throwable e) {
            if (log.isDebugEnabled()) {
                log.debug("Failed to load " + fqcn
                          + "; therefore ignoring " + this.name()
                          + " specific predefined metrics", e);
            }
        }
        return RequestMetricTransformer.NONE;
    }

    public RequestMetricTransformer getRequestMetricTransformer() {
        RequestMetricTransformer transformer = requestMetricTransformer;
        String packageName = transformerPackage;
        if (transformer != null
            && packageName.equals(transformer.getClass().getPackage().getName())) {
            return transformer;
        }
        String fqcn = AwsMetricTransformerFactory.buildRequestMetricTransformerFqcn(name(), packageName);
        this.requestMetricTransformer = loadRequestMetricTransformer(fqcn);
        return this.requestMetricTransformer;
    }
}
