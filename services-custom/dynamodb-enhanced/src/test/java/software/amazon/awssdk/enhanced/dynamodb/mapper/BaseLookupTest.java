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

package software.amazon.awssdk.enhanced.dynamodb.mapper;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;

public abstract class BaseLookupTest {
    private static final String BEANS_JARS_RESOURCE = "software/amazon/awssdk/enhanced/dynamodb/mapper/test-pojos.jar";
    private static final String LOOKUP_FACTORY_FQCN = "software.amazon.awssdk.test.beans.LookupFactory";
    private static final String BEAN_FACTORY_FQCN = "software.amazon.awssdk.test.beans.BeanFactory";
    private static final String CAT_FQCN = "software.amazon.awssdk.test.beans.Cat";
    private static final String CAT_IMMUTABLE_FQCN = "software.amazon.awssdk.test.beans.CatImmutable";
    private static final String RECURSIVE_RECORD_BEAN_FQCN = "software.amazon.awssdk.test.beans.RecursiveRecordBean";
    private static final String RECURSIVE_RECORD_IMMUTABLE_FQCN = "software.amazon.awssdk.test.beans.RecursiveRecordImmutable";

    private static ClassLoader pojosLoader;
    private static MethodHandles.Lookup pojosLookup;
    private static Class<?> factoryClass;
    private static Class<?> catClass;
    private static Class<?> catImmutableClass;
    private static Class<?> recursiveRecordBeanClass;
    private static Class<?> recursiveRecordImmutableClass;

    private static Object factoryInstance;

    @BeforeAll
    static void setup() throws Exception{
        ClassLoader parent = BeanTableSchemaLookupTest.class.getClassLoader();
        URL resource = parent.getResource(BEANS_JARS_RESOURCE);

        pojosLoader = new URLClassLoader(new URL[]{resource}, parent);

        // The Lookup should have visibility to the POJO classes, so that means it must come from the same ClassLoader as the
        // POJOs, so it needs to be a class loaded by pojosLoader.
        pojosLookup = getPojosLookup();

        factoryClass = pojosLoader.loadClass(BEAN_FACTORY_FQCN);
        catClass = pojosLoader.loadClass(CAT_FQCN);
        catImmutableClass = pojosLoader.loadClass(CAT_IMMUTABLE_FQCN);
        recursiveRecordBeanClass = pojosLoader.loadClass(RECURSIVE_RECORD_BEAN_FQCN);
        recursiveRecordImmutableClass = pojosLoader.loadClass(RECURSIVE_RECORD_IMMUTABLE_FQCN);
        factoryInstance = factoryClass.getConstructor().newInstance();
    }

    protected static MethodHandles.Lookup getPojosLookup() throws Exception {
        Class<?> factoryClass = pojosLoader.loadClass(LOOKUP_FACTORY_FQCN);
        Object factory = factoryClass.getConstructor().newInstance();

        Method getLookup = factoryClass.getMethod("getLookup");
        return (MethodHandles.Lookup) getLookup.invoke(factory);
    }

    protected static Class<?> getCatClass() {
        return catClass;
    }

    protected static Class<?> getCatImmutableClass() {
        return catImmutableClass;
    }

    protected static Class<?> getRecursiveRecordBeanClass() {
        return recursiveRecordBeanClass;
    }

    protected static Class<?> getRecursiveRecordImmutableClass() {
        return recursiveRecordImmutableClass;
    }

    protected static Object getFactoryInstance() {
        return factoryInstance;
    }

    protected static Object makeCat(String id, String name) throws Exception {
        Method makeCat = factoryClass.getMethod("makeCat", String.class, String.class);
        return makeCat.invoke(factoryInstance, id, name);
    }

    protected static Object makeCatImmutable(String id, String name) throws Exception {
        Method makeCatImmutable = factoryClass.getMethod("makeCatImmutable", String.class, String.class);
        return makeCatImmutable.invoke(factoryInstance, id, name);
    }

    protected static Object makeRecursiveRecord(int attribute,
                                              Object recursiveRecordBean,
                                              Object recursiveRecordImmutable,
                                              List<Object> recursiveRecordList) throws Exception {
        Method makeRecursiveRecord = factoryClass.getMethod("makeRecursiveRecord",
                                                            int.class,
                                                            recursiveRecordBeanClass,
                                                            recursiveRecordImmutableClass,
                                                            List.class);

        return makeRecursiveRecord.invoke(factoryInstance,
                                          attribute,
                                          recursiveRecordBean,
                                          recursiveRecordImmutable,
                                          recursiveRecordList);
    }

    protected static Object makeRecursiveRecordImmutable(int attribute,
                                                       Object recursiveRecordBean,
                                                       Object recursiveRecordImmutable,
                                                       List<Object> recursiveRecordImmutableList) throws Exception {
        Method makeRecursiveRecord = factoryClass.getMethod("makeRecursiveRecordImmutable",
                                                            int.class,
                                                            recursiveRecordBeanClass,
                                                            recursiveRecordImmutableClass,
                                                            List.class);

        return makeRecursiveRecord.invoke(factoryInstance,
                                          attribute,
                                          recursiveRecordBean,
                                          recursiveRecordImmutable,
                                          recursiveRecordImmutableList);
    }
}
