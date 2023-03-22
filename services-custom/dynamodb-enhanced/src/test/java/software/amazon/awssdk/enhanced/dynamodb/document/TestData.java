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

package software.amazon.awssdk.enhanced.dynamodb.document;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import software.amazon.awssdk.enhanced.dynamodb.AttributeConverterProvider;
import software.amazon.awssdk.enhanced.dynamodb.EnhancedType;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

public class TestData {
    private EnhancedDocument enhancedDocument;
    private String scenario;
    private Map<String, AttributeValue> ddbItemMap;
    private TypeMap typeMap;
    private AttributeConverterProvider attributeConverterProvider;

    public String getScenario() {
        return scenario;
    }

    private String json;
    private boolean isGeneric;

    public static Builder dataBuilder(){
        return new Builder();
    }

    public boolean isGeneric() {
        return isGeneric;
    }

    public EnhancedDocument getEnhancedDocument() {
        return enhancedDocument;
    }

    public Map<String, AttributeValue> getDdbItemMap() {
        return ddbItemMap;
    }

    public TypeMap getTypeMap() {
        return typeMap;
    }

    public AttributeConverterProvider getAttributeConverterProvider() {
        return attributeConverterProvider;
    }

    public String getJson() {
        return json;
    }

    public TestData(Builder builder) {
        this.enhancedDocument = builder.enhancedDocument;
        this.ddbItemMap = builder.ddbItemMap;
        this.typeMap = builder.typeMap;
        this.attributeConverterProvider = builder.attributeConverterProvider;
        this.json = builder.json;
        this.isGeneric = builder.isGeneric;
        this.scenario = builder.scenario;
    }

    public static class Builder{

        private String scenario;

        private Builder() {
        }

        private EnhancedDocument enhancedDocument;
        private boolean isGeneric = true;
        private Map<String, AttributeValue> ddbItemMap;
        private TypeMap typeMap = new TypeMap();
        private AttributeConverterProvider attributeConverterProvider;

        private String json;

        public Builder enhancedDocument(EnhancedDocument enhancedDocument) {
            this.enhancedDocument = enhancedDocument;
            return this;
        }

        public Builder ddbItemMap(Map<String, AttributeValue> ddbItemMap) {
            this.ddbItemMap = ddbItemMap;
            return this;
        }

        public Builder typeMap(TypeMap typeMap) {
            this.typeMap = typeMap;
            return this;
        }

        public Builder attributeConverterProvider(AttributeConverterProvider attributeConverterProvider) {
            this.attributeConverterProvider = attributeConverterProvider;
            return this;
        }

        public Builder isGeneric(boolean isGeneric) {
            this.isGeneric = isGeneric;
            return this;
        }

        public Builder scenario(String scenario) {
            this.scenario = scenario;
            return this;
        }

        public Builder json(String json) {
            this.json = json;
            return this;
        }

        public TestData build(){
            return new TestData(this);
        }

    }

    public  static class TypeMap {
        private TypeMap() {
        }

        public static TypeMap typeMap(){
            return new TypeMap();
        }

        Map<String, List<EnhancedType>> enhancedTypeMap = new LinkedHashMap<>();

        public Map<String, List<EnhancedType>> getEnhancedTypeMap() {
            return enhancedTypeMap;
        }

        public TypeMap addAttribute(String attribute, EnhancedType... enhancedType) {
            enhancedTypeMap.put(attribute, Arrays.asList(enhancedType));
            return this;
        }
    }
}
