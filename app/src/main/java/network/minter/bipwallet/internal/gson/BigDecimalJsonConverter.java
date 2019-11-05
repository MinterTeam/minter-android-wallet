package network.minter.bipwallet.internal.gson;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.math.BigDecimal;

import network.minter.blockchain.models.operational.Transaction;

public class BigDecimalJsonConverter implements JsonDeserializer<BigDecimal>, JsonSerializer<BigDecimal> {


    @Override
    public BigDecimal deserialize(JsonElement json, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        if (json.isJsonNull() || !json.isJsonPrimitive()) {
            return new BigDecimal("0");
        }

        if (json.getAsString().isEmpty()) {
            return new BigDecimal("0").setScale(Transaction.VALUE_MUL_DEC.scale());
        }

        return new BigDecimal(json.getAsString());
    }

    @Override
    public JsonElement serialize(BigDecimal src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(src.toPlainString());
    }
}
