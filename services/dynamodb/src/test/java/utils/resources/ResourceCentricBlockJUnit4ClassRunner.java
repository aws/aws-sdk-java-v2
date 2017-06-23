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

package utils.resources;

import java.util.HashSet;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Ignore;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import utils.resources.RequiredResources.RequiredResource;
import utils.resources.RequiredResources.ResourceRetentionPolicy;

public class ResourceCentricBlockJUnit4ClassRunner extends BlockJUnit4ClassRunner {

    private final Set<TestResource> resourcesToBeDestroyedAfterAllTests;

    private final RequiredResources classRequiredResourcesAnnotation;

    private final Log log = LogFactory.getLog(ResourceCentricBlockJUnit4ClassRunner.class);

    public ResourceCentricBlockJUnit4ClassRunner(Class<?> klass)
            throws InitializationError {
        super(klass);

        classRequiredResourcesAnnotation = klass.getAnnotation(RequiredResources.class);
        resourcesToBeDestroyedAfterAllTests = new HashSet<TestResource>();
    }

    /**
     *
     */
    private static TestResource createResourceInstance(RequiredResource resourceAnnotation)
            throws InstantiationException, IllegalAccessException {
        Class<? extends TestResource> resourceClazz = resourceAnnotation.resource();
        if (resourceClazz == null) {
            throw new IllegalArgumentException(
                    "resource parameter is missing for the @RequiredResource annotation.");
        }
        return resourceClazz.newInstance();
    }

    @Override
    protected void runChild(final FrameworkMethod method, RunNotifier notifier) {
        Description description = describeChild(method);
        if (method.getAnnotation(Ignore.class) != null) {
            notifier.fireTestIgnored(description);
        } else {
            RequiredResources annotation = method.getAnnotation(RequiredResources.class);
            if (annotation != null) {
                try {
                    beforeRunLeaf(annotation.value());
                } catch (Exception e) {
                    notifier.fireTestFailure(new Failure(description, e));
                }

            }

            runLeaf(methodBlock(method), description, notifier);

            if (annotation != null) {
                try {
                    afterRunLeaf(annotation.value());
                } catch (Exception e) {
                    notifier.fireTestFailure(new Failure(description, e));
                }
            }
        }
    }

    /**
     * Override the withBeforeClasses method to inject executing resource
     * creation between @BeforeClass methods and test methods.
     */
    @Override
    protected Statement withBeforeClasses(final Statement statement) {
        Statement withRequiredResourcesCreation = new Statement() {

            @Override
            public void evaluate() throws Throwable {
                if (classRequiredResourcesAnnotation != null) {
                    beforeRunClass(classRequiredResourcesAnnotation.value());
                }
                statement.evaluate();
            }
        };
        return super.withBeforeClasses(withRequiredResourcesCreation);
    }

    /**
     * Override the withAfterClasses method to inject executing resource
     * creation between test methods and the @AfterClass methods.
     */
    @Override
    protected Statement withAfterClasses(final Statement statement) {
        Statement withRequiredResourcesDeletion = new Statement() {

            @Override
            public void evaluate() throws Throwable {
                statement.evaluate();
                afterRunClass();
            }
        };
        return super.withAfterClasses(withRequiredResourcesDeletion);
    }

    private void beforeRunClass(RequiredResource[] resourcesAnnotation)
            throws InstantiationException, IllegalAccessException, InterruptedException {
        log.debug("Processing @RequiredResources before running the test class...");
        for (RequiredResource resourceAnnotation : resourcesAnnotation) {
            TestResource resource = createResourceInstance(resourceAnnotation);
            TestResourceUtils.createResource(resource, resourceAnnotation.creationPolicy());

            if (resourceAnnotation.retentionPolicy() != ResourceRetentionPolicy.KEEP) {
                resourcesToBeDestroyedAfterAllTests.add(resource);
            }
        }
    }

    private void afterRunClass()
            throws InstantiationException, IllegalAccessException, InterruptedException {
        log.debug("Processing @RequiredResources after running the test class...");
        for (TestResource resource : resourcesToBeDestroyedAfterAllTests) {
            TestResourceUtils.deleteResource(resource);
        }
    }

    private void beforeRunLeaf(RequiredResource[] resourcesAnnotation)
            throws InstantiationException, IllegalAccessException, InterruptedException {
        log.debug("Processing @RequiredResources before running the test...");
        for (RequiredResource resourceAnnotation : resourcesAnnotation) {
            TestResource resource = createResourceInstance(resourceAnnotation);
            TestResourceUtils.createResource(resource, resourceAnnotation.creationPolicy());

            if (resourceAnnotation.retentionPolicy() == ResourceRetentionPolicy.DESTROY_AFTER_ALL_TESTS) {
                resourcesToBeDestroyedAfterAllTests.add(resource);
            }
        }
    }

    private void afterRunLeaf(RequiredResource[] resourcesAnnotation)
            throws InstantiationException, IllegalAccessException, InterruptedException {
        log.debug("Processing @RequiredResources after running the test...");
        for (RequiredResource resourceAnnotation : resourcesAnnotation) {
            TestResource resource = createResourceInstance(resourceAnnotation);

            if (resourceAnnotation.retentionPolicy() == ResourceRetentionPolicy.DESTROY_IMMEDIATELY) {
                TestResourceUtils.deleteResource(resource);
            }
        }
    }
}
