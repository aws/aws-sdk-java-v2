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

package software.amazon.awssdk.jmx;

import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.jmx.spi.SdkMBeanRegistry;
import software.amazon.awssdk.metrics.MetricAdmin;

public class SdkMBeanRegistrySupport implements SdkMBeanRegistry {
    ;

    @Override
    public boolean registerMetricAdminMBean(String objectName) {
        try {
            return MBeans.registerMBean(objectName, new MetricAdmin());
        } catch (Exception ex) {
            LogFactory.getLog(SdkMBeanRegistrySupport.class).warn("", ex);
        }
        return false;
    }

    @Override
    public boolean unregisterMBean(String objectName) {
        try {
            return MBeans.unregisterMBean(objectName);
        } catch (Exception ex) {
            LogFactory.getLog(SdkMBeanRegistrySupport.class).warn("", ex);
        }
        return false;
    }

    @Override
    public boolean isMBeanRegistered(String objectName) {
        return MBeans.isRegistered(objectName);
    }
}
