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

package software.amazon.awssdk.jmx.spi;

import org.apache.commons.logging.LogFactory;

/**
 * SPI used to register MBeans and can survive the absence of JMX.
 */
public interface SdkMBeanRegistry {
    static final SdkMBeanRegistry NONE = new SdkMBeanRegistry() {
        @Override
        public boolean registerMetricAdminMBean(String objectName) {
            return false;
        }

        @Override
        public boolean unregisterMBean(String objectName) {
            return false;
        }

        @Override
        public boolean isMBeanRegistered(String objectName) {
            return false;
        }
    };

    /**
     * Returns true if the registration of the admin MBean under the given
     * object name succeeded; false otherwise.
     */
    public boolean registerMetricAdminMBean(String objectName);

    /**
     * Returns true if the unregistration of the MBean under the given object
     * name succeeded; false otherwise.
     */
    public boolean unregisterMBean(String objectName);

    /**
     * Returns true if the the MBean under the given object name is currently
     * registered; false otherwise.
     */
    public boolean isMBeanRegistered(String objectName);

    public static class Factory {
        private static final SdkMBeanRegistry REGISTRY;

        static {
            SdkMBeanRegistry rego;
            try {
                Class<?> c = Class.forName("software.amazon.awssdk.jmx.SdkMBeanRegistrySupport");
                rego = (SdkMBeanRegistry) c.newInstance();
            } catch (Exception e) {
                LogFactory
                        .getLog(SdkMBeanRegistry.class)
                        .debug("Failed to load the JMX implementation module - JMX is disabled", e);
                rego = NONE;
            }
            REGISTRY = rego;
        }

        public static SdkMBeanRegistry getMBeanRegistry() {
            return REGISTRY;
        }
    }
}
