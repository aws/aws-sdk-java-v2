package software.amazon.awssdk.protocols.jsoncore;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;
import software.amazon.awssdk.utils.StringInputStream;

public class JsonNodeTest {
    private static final JsonNodeParser PARSER = JsonNode.parser();

    @Test
    public void parseString_works() {
        assertThat(PARSER.parse("{}").isObject()).isTrue();
    }

    @Test
    public void parseInputStream_works() {
        assertThat(PARSER.parse(new StringInputStream("{}")).isObject()).isTrue();
    }

    @Test
    public void parseByteArray_works() {
        assertThat(PARSER.parse("{}".getBytes(UTF_8)).isObject()).isTrue();
    }

    @Test
    public void parseNull_givesCorrectType() {
        JsonNode node = PARSER.parse("null");

        assertThat(node.isNull()).isTrue();
        assertThat(node.isBoolean()).isFalse();
        assertThat(node.isNumber()).isFalse();
        assertThat(node.isString()).isFalse();
        assertThat(node.isArray()).isFalse();
        assertThat(node.isObject()).isFalse();
        assertThat(node.isEmbeddedObject()).isFalse();

        assertThatThrownBy(node::asBoolean).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(node::asNumber).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(node::asString).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(node::asArray).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(node::asObject).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(node::asEmbeddedObject).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void parseBoolean_givesCorrectType() {
        String[] options = { "true", "false" };
        for (String option : options) {
            JsonNode node = PARSER.parse(option);


            assertThat(node.isNull()).isFalse();
            assertThat(node.isBoolean()).isTrue();
            assertThat(node.isNumber()).isFalse();
            assertThat(node.isString()).isFalse();
            assertThat(node.isArray()).isFalse();
            assertThat(node.isObject()).isFalse();
            assertThat(node.isEmbeddedObject()).isFalse();

            assertThatThrownBy(node::asNumber).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asString).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asArray).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asObject).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asEmbeddedObject).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    public void parseNumber_givesCorrectType() {
        String[] options = { "-1e100", "-1", "0", "1", "1e100" };
        for (String option : options) {
            JsonNode node = PARSER.parse(option);

            assertThat(node.isNull()).isFalse();
            assertThat(node.isBoolean()).isFalse();
            assertThat(node.isNumber()).isTrue();
            assertThat(node.isString()).isFalse();
            assertThat(node.isArray()).isFalse();
            assertThat(node.isObject()).isFalse();
            assertThat(node.isEmbeddedObject()).isFalse();

            assertThatThrownBy(node::asBoolean).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asString).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asArray).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asObject).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asEmbeddedObject).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    public void parseString_givesCorrectType() {
        String[] options = { "\"\"", "\"foo\"" };
        for (String option : options) {
            JsonNode node = PARSER.parse(option);

            assertThat(node.isNull()).isFalse();
            assertThat(node.isBoolean()).isFalse();
            assertThat(node.isNumber()).isFalse();
            assertThat(node.isString()).isTrue();
            assertThat(node.isArray()).isFalse();
            assertThat(node.isObject()).isFalse();
            assertThat(node.isEmbeddedObject()).isFalse();

            assertThatThrownBy(node::asBoolean).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asNumber).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asArray).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asObject).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asEmbeddedObject).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    public void parseArray_givesCorrectType() {
        String[] options = { "[]", "[null]" };
        for (String option : options) {
            JsonNode node = PARSER.parse(option);

            assertThat(node.isNull()).isFalse();
            assertThat(node.isBoolean()).isFalse();
            assertThat(node.isNumber()).isFalse();
            assertThat(node.isString()).isFalse();
            assertThat(node.isArray()).isTrue();
            assertThat(node.isObject()).isFalse();
            assertThat(node.isEmbeddedObject()).isFalse();

            assertThatThrownBy(node::asBoolean).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asNumber).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asString).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asObject).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asEmbeddedObject).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    public void parseObject_givesCorrectType() {
        String[] options = { "{}", "{ \"foo\": null }" };
        for (String option : options) {
            JsonNode node = PARSER.parse(option);

            assertThat(node.isNull()).isFalse();
            assertThat(node.isBoolean()).isFalse();
            assertThat(node.isNumber()).isFalse();
            assertThat(node.isString()).isFalse();
            assertThat(node.isArray()).isFalse();
            assertThat(node.isObject()).isTrue();
            assertThat(node.isEmbeddedObject()).isFalse();

            assertThatThrownBy(node::asBoolean).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asNumber).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asString).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asArray).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(node::asEmbeddedObject).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Test
    public void parseBoolean_givesCorrectValue() {
        assertThat(PARSER.parse("true").asBoolean()).isTrue();
        assertThat(PARSER.parse("false").asBoolean()).isFalse();
    }

    @Test
    public void parseNumber_givesCorrectValue() {
        assertThat(PARSER.parse("0").asNumber()).isEqualTo("0");
        assertThat(PARSER.parse("-1").asNumber()).isEqualTo("-1");
        assertThat(PARSER.parse("1").asNumber()).isEqualTo("1");
        assertThat(PARSER.parse("1e10000").asNumber()).isEqualTo("1e10000");
        assertThat(PARSER.parse("-1e10000").asNumber()).isEqualTo("-1e10000");
        assertThat(PARSER.parse("1.23").asNumber()).isEqualTo("1.23");
        assertThat(PARSER.parse("-1.23").asNumber()).isEqualTo("-1.23");
    }

    @Test
    public void parseString_givesCorrectValue() {
        assertThat(PARSER.parse("\"foo\"").asString()).isEqualTo("foo");
        assertThat(PARSER.parse("\"\"").asString()).isEqualTo("");
        assertThat(PARSER.parse("\"&nbsp;\"").asString()).isEqualTo("&nbsp;");
        assertThat(PARSER.parse("\"%20\"").asString()).isEqualTo("%20");
        assertThat(PARSER.parse("\"\\\"\"").asString()).isEqualTo("\"");
        assertThat(PARSER.parse("\" \"").asString()).isEqualTo(" ");
    }

    @Test
    public void parseArray_givesCorrectValue() {
        assertThat(PARSER.parse("[]").asArray()).isEmpty();
        assertThat(PARSER.parse("[null, 1]").asArray()).satisfies(list -> {
            assertThat(list).hasSize(2);
            assertThat(list.get(0).isNull()).isTrue();
            assertThat(list.get(1).asNumber()).isEqualTo("1");
        });
    }

    @Test
    public void parseObject_givesCorrectValue() {
        assertThat(PARSER.parse("{}").asObject()).isEmpty();
        assertThat(PARSER.parse("{\"foo\": \"bar\", \"baz\": 0}").asObject()).satisfies(map -> {
            assertThat(map).hasSize(2);
            assertThat(map.get("foo").asString()).isEqualTo("bar");
            assertThat(map.get("baz").asNumber()).isEqualTo("0");
        });
    }

    @Test
    public void text_returnsContent() {
        assertThat(PARSER.parse("null").text()).isEqualTo(null);
        assertThat(PARSER.parse("0").text()).isEqualTo("0");
        assertThat(PARSER.parse("\"foo\"").text()).isEqualTo("foo");
        assertThat(PARSER.parse("true").text()).isEqualTo("true");
        assertThat(PARSER.parse("[]").text()).isEqualTo(null);
        assertThat(PARSER.parse("{}").text()).isEqualTo(null);
    }

    @Test
    public void getString_returnsContent() {
        assertThat(PARSER.parse("null").get("")).isEmpty();
        assertThat(PARSER.parse("0").get("")).isEmpty();
        assertThat(PARSER.parse("\"foo\"").get("")).isEmpty();
        assertThat(PARSER.parse("true").get("")).isEmpty();
        assertThat(PARSER.parse("[]").get("")).isEmpty();
        assertThat(PARSER.parse("{\"\":0}").get("")).map(JsonNode::asNumber).hasValue("0");
    }

    @Test
    public void getArray_returnsContent() {
        assertThat(PARSER.parse("null").get(0)).isEmpty();
        assertThat(PARSER.parse("0").get(0)).isEmpty();
        assertThat(PARSER.parse("\"foo\"").get(0)).isEmpty();
        assertThat(PARSER.parse("true").get(0)).isEmpty();
        assertThat(PARSER.parse("[]").get(0)).isEmpty();
        assertThat(PARSER.parse("[null]").get(0)).map(JsonNode::isNull).hasValue(true);
        assertThat(PARSER.parse("{}").get("")).isEmpty();
    }

    @Test
    public void toStringIsCorrect() {
        String input = "{"
                       + "\"1\": \"2\","
                       + "\"3\": 4,"
                       + "\"5\": null,"
                       + "\"6\": false,"
                       + "\"7\": [[{}]],"
                       + "\"8\": \"\\\\n\\\"\""
                       + "}";
        assertThat(PARSER.parse(input).toString()).isEqualTo(input);
    }

    @Test
    public void exceptionsIncludeErrorLocation() {
        assertThatThrownBy(() -> PARSER.parse("{{foo}")).hasMessageContaining("foo");
    }

    @Test
    public void removeErrorLocations_removesErrorLocations() {
        assertThatThrownBy(() -> JsonNode.parserBuilder()
                                         .removeErrorLocations(true)
                                         .build()
                                         .parse("{{foo}"))
            .satisfies(exception -> {
                Throwable cause = exception;
                while (cause != null) {
                    assertThat(cause.getMessage()).doesNotContain("foo");
                    cause = cause.getCause();
                }
            });
    }
}