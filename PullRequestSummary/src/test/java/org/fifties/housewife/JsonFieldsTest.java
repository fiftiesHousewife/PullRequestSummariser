package org.fifties.housewife;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JsonFieldsTest {

    @Test
    void extractsStringValue() {
        final JsonObject object = new JsonObject();
        object.addProperty("name", "test");
        assertThat(JsonFields.str(object, "name")).isEqualTo("test");
    }

    @Test
    void returnsEmptyStringForMissingKey() {
        assertThat(JsonFields.str(new JsonObject(), "missing")).isEmpty();
    }

    @Test
    void returnsEmptyStringForNullValue() {
        final JsonObject object = new JsonObject();
        object.add("name", JsonNull.INSTANCE);
        assertThat(JsonFields.str(object, "name")).isEmpty();
    }

    @Test
    void extractsIntegerValue() {
        final JsonObject object = new JsonObject();
        object.addProperty("count", 42);
        assertThat(JsonFields.num(object, "count")).isEqualTo(42);
    }

    @Test
    void returnsZeroForMissingNumber() {
        assertThat(JsonFields.num(new JsonObject(), "missing")).isZero();
    }

    @Test
    void extractsBooleanValue() {
        final JsonObject object = new JsonObject();
        object.addProperty("active", true);
        assertThat(JsonFields.bool(object, "active")).isTrue();
    }

    @Test
    void returnsFalseForMissingBoolean() {
        assertThat(JsonFields.bool(new JsonObject(), "missing")).isFalse();
    }

    @Test
    void extractsArrayValue() {
        final JsonObject object = new JsonObject();
        final JsonArray array = new JsonArray();
        array.add("one");
        object.add("items", array);
        assertThat(JsonFields.arr(object, "items")).hasSize(1);
    }

    @Test
    void returnsEmptyArrayForMissingKey() {
        assertThat(JsonFields.arr(new JsonObject(), "missing")).isEmpty();
    }

    @Test
    void handlesAllNullCasesConsistently() {
        final JsonObject object = new JsonObject();
        object.add("str", JsonNull.INSTANCE);
        object.add("num", JsonNull.INSTANCE);
        object.add("bool", JsonNull.INSTANCE);
        object.add("arr", JsonNull.INSTANCE);

        assertAll(
                () -> assertThat(JsonFields.str(object, "str")).isEmpty(),
                () -> assertThat(JsonFields.num(object, "num")).isZero(),
                () -> assertThat(JsonFields.bool(object, "bool")).isFalse(),
                () -> assertThat(JsonFields.arr(object, "arr")).isEmpty()
        );
    }
}
