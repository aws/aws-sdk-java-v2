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
 * SPI used to retrieve JMX information and can survive the absence of JMX.
 */
public interface JmxInfoProvider {
    static final JmxInfoProvider NONE = new JmxInfoProvider() {
        @Override
        public long[] getFileDecriptorInfo() {
            return null;
        }

        @Override
        public int getThreadCount() {
            return 0;
        }

        @Override
        public int getDaemonThreadCount() {
            return 0;
        }

        @Override
        public int getPeakThreadCount() {
            return 0;
        }

        @Override
        public long getTotalStartedThreadCount() {
            return 0;
        }

        @Override
        public long[] findDeadlockedThreads() {
            return null;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    };

    public long[] getFileDecriptorInfo();

    public int getThreadCount();

    public int getDaemonThreadCount();

    public int getPeakThreadCount();

    public long getTotalStartedThreadCount();

    public long[] findDeadlockedThreads();

    public boolean isEnabled();

    public static class Factory {
        private static final JmxInfoProvider PROVIDER;

        static {
            JmxInfoProvider p;
            try {
                Class<?> c = Class.forName("software.amazon.awssdk.jmx.JmxInfoProviderSupport");
                p = (JmxInfoProvider) c.newInstance();
            } catch (Exception e) {
                LogFactory
                        .getLog(JmxInfoProvider.class)
                        .debug("Failed to load the JMX implementation module - JMX is disabled", e);
                p = NONE;
            }
            PROVIDER = p;
        }

        public static JmxInfoProvider getJmxInfoProvider() {
            return PROVIDER;
        }
    }
}
