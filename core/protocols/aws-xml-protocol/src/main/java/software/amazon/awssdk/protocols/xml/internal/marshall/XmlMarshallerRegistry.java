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

package software.amazon.awssdk.protocols.xml.internal.marshall;

import software.amazon.awssdk.annotations.SdkInternalApi;
import software.amazon.awssdk.core.protocol.MarshallLocation;
import software.amazon.awssdk.core.protocol.MarshallingType;
import software.amazon.awssdk.protocols.core.AbstractMarshallingRegistry;

@SdkInternalApi
public final class XmlMarshallerRegistry extends AbstractMarshallingRegistry {

    private XmlMarshallerRegistry(Builder builder) {
        super(builder);
    }

    @SuppressWarnings("unchecked")
    public <T> XmlMarshaller<T> getMarshaller(MarshallLocation marshallLocation, T val) {
        return (XmlMarshaller<T>) get(marshallLocation, toMarshallingType(val));
    }

    @SuppressWarnings("unchecked")
    public <T> XmlMarshaller<Object> getMarshaller(MarshallLocation marshallLocation,
                                                   MarshallingType<T> marshallingType,
                                                   Object val) {
        return (XmlMarshaller<Object>) get(marshallLocation,
                                           val == null ? MarshallingType.NULL : marshallingType);
    }

    /**
     * @return Builder instance to construct a {@link XmlMarshallerRegistry}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for a {@link XmlMarshallerRegistry}.
     */
    public static final class Builder extends AbstractMarshallingRegistry.Builder {
        private Builder() {
        }

        public <T> Builder payloadMarshaller(MarshallingType<T> marshallingType,
                                             XmlMarshaller<T> marshaller) {
            register(MarshallLocation.PAYLOAD, marshallingType, marshaller);
            return this;
        }

        public <T> Builder headerMarshaller(MarshallingType<T> marshallingType,
                                            XmlMarshaller<T> marshaller) {
            register(MarshallLocation.HEADER, marshallingType, marshaller);
            return this;
        }

        public <T> Builder queryParamMarshaller(MarshallingType<T> marshallingType,
                                                XmlMarshaller<T> marshaller) {
            register(MarshallLocation.QUERY_PARAM, marshallingType, marshaller);
            return this;
        }

        public <T> Builder pathParamMarshaller(MarshallingType<T> marshallingType,
                                               XmlMarshaller<T> marshaller) {
            register(MarshallLocation.PATH, marshallingType, marshaller);
            return this;
        }

        public <T> Builder greedyPathParamMarshaller(MarshallingType<T> marshallingType,
                                                     XmlMarshaller<T> marshaller) {
            register(MarshallLocation.GREEDY_PATH, marshallingType, marshaller);
            return this;
        }

        /**
         * @return An immutable {@link XmlMarshallerRegistry} object.
         */
        public XmlMarshallerRegistry build() {
            return new XmlMarshallerRegistry(this);
        }
    }
}
