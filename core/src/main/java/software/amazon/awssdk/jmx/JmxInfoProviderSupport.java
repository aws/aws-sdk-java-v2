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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.jmx.spi.JmxInfoProvider;

public class JmxInfoProviderSupport implements JmxInfoProvider {
    @Override
    public long[] getFileDecriptorInfo() {
        MBeanServer mbsc = MBeans.getMBeanServer();
        AttributeList attributes;
        try {
            attributes = mbsc.getAttributes(
                    new ObjectName("java.lang:type=OperatingSystem"),
                    new String[] {"OpenFileDescriptorCount", "MaxFileDescriptorCount"});
            List<Attribute> attrList = attributes.asList();
            long openFdCount = (Long) attrList.get(0).getValue();
            long maxFdCount = (Long) attrList.get(1).getValue();
            long[] fdCounts = {openFdCount, maxFdCount};
            return fdCounts;
        } catch (Exception e) {
            LogFactory.getLog(SdkMBeanRegistrySupport.class).debug(
                    "Failed to retrieve file descriptor info", e);
        }
        return null;
    }

    @Override
    public int getThreadCount() {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        return threadMxBean.getThreadCount();
    }

    @Override
    public int getDaemonThreadCount() {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        return threadMxBean.getDaemonThreadCount();
    }

    @Override
    public int getPeakThreadCount() {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        return threadMxBean.getPeakThreadCount();
    }

    @Override
    public long getTotalStartedThreadCount() {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        return threadMxBean.getTotalStartedThreadCount();
    }

    @Override
    public long[] findDeadlockedThreads() {
        ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
        return threadMxBean.findDeadlockedThreads();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
