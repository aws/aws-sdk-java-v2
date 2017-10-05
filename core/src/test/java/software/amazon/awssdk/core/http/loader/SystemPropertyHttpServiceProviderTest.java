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

package software.amazon.awssdk.core.http.loader;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.core.SdkClientException;
import software.amazon.awssdk.http.SdkHttpClientFactory;
import software.amazon.awssdk.http.SdkHttpService;
import software.amazon.awssdk.utils.SdkSystemSetting;

public class SystemPropertyHttpServiceProviderTest {

    private SdkHttpServiceProvider<SdkHttpService> provider;

    @Before
    public void setup() {
        provider = SystemPropertyHttpServiceProvider.syncProvider();
    }

    @After
    public void tearDown() {
        System.clearProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property());
    }

    @Test
    public void systemPropertyNotSet_ReturnsEmptyOptional() {
        assertThat(provider.loadService()).isEmpty();
    }

    @Test(expected = SdkClientException.class)
    public void systemPropertySetToInvalidClassName_ThrowsException() {
        System.setProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property(), "com.invalid.ClassName");
        provider.loadService();
    }

    @Test(expected = SdkClientException.class)
    public void systemPropertySetToNonServiceClass_ThrowsException() {
        System.setProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property(), getClass().getName());
        provider.loadService();
    }

    @Test(expected = SdkClientException.class)
    public void systemPropertySetToServiceClassWithNoDefaultCtor_ThrowsException() {
        System.setProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property(), HttpServiceWithNoDefaultCtor.class.getName());
        provider.loadService();
    }

    @Test
    public void systemPropertySetToValidClass_ReturnsFulfulledOptional() {
        System.setProperty(SdkSystemSetting.SYNC_HTTP_SERVICE_IMPL.property(), MockHttpService.class.getName());
        assertThat(provider.loadService()).isPresent();
    }

    public static final class MockHttpService implements SdkHttpService {

        @Override
        public SdkHttpClientFactory createHttpClientFactory() {
            return null;
        }
    }

    public static final class HttpServiceWithNoDefaultCtor implements SdkHttpService {

        HttpServiceWithNoDefaultCtor(String foo) {
        }

        @Override
        public SdkHttpClientFactory createHttpClientFactory() {
            return null;
        }
    }

}