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

package software.amazon.awssdk.mapper.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

class DynamoDBTypeConvertedJsonTest {

    private final DynamoDBMapper mapper = DynamoDBMapper.builder()
                                                        .dynamoDbClient(DynamoDbClient.builder()
                                                            .region(Region.US_EAST_1)
                                                            .credentialsProvider(AnonymousCredentialsProvider.create())
                                                            .build())
                                                        .build();

    @Test
    void isAvailable_withJacksonDatabind_returnsTrue() {
        assertThat(JacksonDatabindSupport.isAvailable()).isTrue();
    }

    @Test
    void getTableModel_jsonAttribute_storesJsonStringAndReadsItBack() {
        JsonItem item = new JsonItem();
        item.setId("1");
        item.setCurrency(new Currency(79.99, "USD"));

        DynamoDBMapperTableModel<JsonItem> model = mapper.getTableModel(JsonItem.class);
        Map<String, AttributeValue> stored = model.convert(item);

        assertThat(stored.get("currency").s()).isEqualTo("{\"amount\":79.99,\"unit\":\"USD\"}");
        Currency loaded = model.unconvert(stored).getCurrency();
        assertThat(loaded.getAmount()).isEqualTo(79.99);
        assertThat(loaded.getUnit()).isEqualTo("USD");
    }

    @Test
    void getTableModel_targetType_readsIntoTargetType() {
        ListItem item = new ListItem();
        item.setId("1");
        CurrencyList currencies = new CurrencyList();
        currencies.addAll(Arrays.asList(new Currency(1.0, "EUR"), new Currency(2.0, "GBP")));
        item.setCurrencies(currencies);

        DynamoDBMapperTableModel<ListItem> model = mapper.getTableModel(ListItem.class);
        ListItem loaded = model.unconvert(model.convert(item));

        assertThat(loaded.getCurrencies()).isInstanceOf(CurrencyList.class);
        assertThat(loaded.getCurrencies()).extracting(Currency::getUnit).containsExactly("EUR", "GBP");
    }

    @Test
    void unconvert_unknownProperty_isIgnored() {
        Currency loaded = converter(Currency.class).unconvert("{\"amount\":1.0,\"unit\":\"EUR\",\"unknown\":true}");

        assertThat(loaded.getUnit()).isEqualTo("EUR");
    }

    @Test
    void unconvert_invalidJson_throwsMappingExceptionWithDatabindCause() {
        assertThatThrownBy(() -> converter(Currency.class).unconvert("{not json"))
            .isInstanceOf(DynamoDBMappingException.class)
            .hasMessage("Unable to read JSON string")
            .hasCauseInstanceOf(JsonProcessingException.class);
    }

    @Test
    void convert_unserializableObject_throwsMappingExceptionWithDatabindCause() {
        assertThatThrownBy(() -> converter(Object.class).convert(new Object()))
            .isInstanceOf(DynamoDBMappingException.class)
            .hasMessage("Unable to write object to JSON")
            .hasCauseInstanceOf(JsonMappingException.class);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> DynamoDBTypeConvertedJson.Converter<T> converter(Class<T> type) {
        try {
            DynamoDBTypeConvertedJson annotation =
                JsonItem.class.getMethod("getCurrency").getAnnotation(DynamoDBTypeConvertedJson.class);
            return new DynamoDBTypeConvertedJson.Converter(type, annotation);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    public static class JsonItem {
        private String id;
        private Currency currency;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBTypeConvertedJson
        public Currency getCurrency() {
            return currency;
        }

        public void setCurrency(Currency currency) {
            this.currency = currency;
        }
    }

    public static class ListItem {
        private String id;
        private CurrencyList currencies;

        @DynamoDBHashKey
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        @DynamoDBTypeConvertedJson(targetType = CurrencyList.class)
        public CurrencyList getCurrencies() {
            return currencies;
        }

        public void setCurrencies(CurrencyList currencies) {
            this.currencies = currencies;
        }
    }

    public static class CurrencyList extends ArrayList<Currency> {
    }

    public static class Currency {
        private Double amount;
        private String unit;

        public Currency() {
        }

        Currency(Double amount, String unit) {
            this.amount = amount;
            this.unit = unit;
        }

        public Double getAmount() {
            return amount;
        }

        public void setAmount(Double amount) {
            this.amount = amount;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
        }
    }
}
