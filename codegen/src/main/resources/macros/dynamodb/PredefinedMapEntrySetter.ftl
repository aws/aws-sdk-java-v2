<#macro content shapeModel>

    <#if shapeModel.shapeName == "GetItemRequest" ||
         shapeModel.shapeName == "UpdateItemRequest" ||
         shapeModel.shapeName == "DeleteItemRequest" ||
         shapeModel.shapeName == "DeleteRequest">

        /**
         * Set the hash and range key attributes of the item.
         * <p>For a hash-only table, you only need to provide the hash attribute.
         * For a hash-and-range table, you must provide both.
         *
         * @param hashKey a map entry including the name and value of the primary hash key.
         * @param rangeKey a map entry including the name and value of the primary range key, or null if it is a hash-only table.
         */
        public void setKey(java.util.Map.Entry<String, AttributeValue> hashKey, java.util.Map.Entry<String, AttributeValue> rangeKey) throws IllegalArgumentException {
            java.util.HashMap<String,AttributeValue> key = new java.util.HashMap<String,AttributeValue>();
            if (hashKey != null) {
                key.put(hashKey.getKey(), hashKey.getValue());
            } else {
                throw new IllegalArgumentException("hashKey must be non-null object.");
            }
            if (rangeKey != null) {
                key.put(rangeKey.getKey(), rangeKey.getValue());
            }
            setKey(key);
        }

        /**
         * Set the hash and range key attributes of the item.
         * <p>For a hash-only table, you only need to provide the hash attribute.
         * For a hash-and-range table, you must provide both.
         * <p>
         * Returns a reference to this object so that method calls can be chained together.
         *
         * @param hashKey a map entry including the name and value of the primary hash key.
         * @param rangeKey a map entry including the name and value of the primary range key, or null if it is a hash-only table.
         */
        public ${shapeModel.shapeName} withKey(java.util.Map.Entry<String, AttributeValue> hashKey, java.util.Map.Entry<String, AttributeValue> rangeKey) throws IllegalArgumentException {
            setKey(hashKey, rangeKey);
            return this;
        }

    </#if>

    <#if shapeModel.shapeName == "QueryRequest" ||
         shapeModel.shapeName == "ScanRequest">

        /**
         * The primary hash and range keys of the first item that this operation will evaluate.
         * Use the value that was returned for <i>LastEvaluatedKey</i> in the
         * previous operation. <p>The data type for <i>ExclusiveStartKey</i> must
         * be String, Number or Binary. No set data types are allowed.
         *
         * @param hashKey a map entry including the name and value of the primary hash key.
         * @param rangeKey a map entry including the name and value of the primary range key, or null if it is a hash-only table.
         */
        public void setExclusiveStartKey(java.util.Map.Entry<String, AttributeValue> hashKey, java.util.Map.Entry<String, AttributeValue> rangeKey) throws IllegalArgumentException {
            java.util.HashMap<String,AttributeValue> exclusiveStartKey = new java.util.HashMap<String,AttributeValue>();
            if (hashKey != null) {
                exclusiveStartKey.put(hashKey.getKey(), hashKey.getValue());
            } else {
                throw new IllegalArgumentException("hashKey must be non-null object.");
            }
            if (rangeKey != null) {
                exclusiveStartKey.put(rangeKey.getKey(), rangeKey.getValue());
            }
            setExclusiveStartKey(exclusiveStartKey);
        }

        /**
         * The primary hash and range keys of the first item that this operation will evaluate.
         * Use the value that was returned for <i>LastEvaluatedKey</i> in the
         * previous operation. <p>The data type for <i>ExclusiveStartKey</i> must
         * be String, Number or Binary. No set data types are allowed.
         * <p>
         * Returns a reference to this object so that method calls can be chained together.
         *
         * @param hashKey a map entry including the name and value of the primary hash key.
         * @param rangeKey a map entry including the name and value of the primary range key, or null if it is a hash-only table.
         */
        public ${shapeModel.shapeName} withExclusiveStartKey(java.util.Map.Entry<String, AttributeValue> hashKey, java.util.Map.Entry<String, AttributeValue> rangeKey) throws IllegalArgumentException {
            setExclusiveStartKey(hashKey, rangeKey);
            return this;
        }

    </#if>

</#macro>
