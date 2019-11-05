package network.minter.bipwallet.internal.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;

import network.minter.core.crypto.BytesData;
import network.minter.core.crypto.MinterAddress;
import network.minter.core.crypto.MinterCheck;
import network.minter.core.crypto.MinterHash;
import network.minter.core.crypto.MinterPublicKey;
import network.minter.core.internal.api.converters.BigIntegerJsonConverter;
import network.minter.core.internal.api.converters.BytesDataJsonConverter;
import network.minter.core.internal.api.converters.MinterAddressJsonConverter;
import network.minter.core.internal.api.converters.MinterCheckJsonConverter;
import network.minter.core.internal.api.converters.MinterHashJsonConverter;
import network.minter.core.internal.api.converters.MinterPublicKeyJsonConverter;
import network.minter.explorer.MinterExplorerApi;
import network.minter.explorer.models.HistoryTransaction;

public class HistoryTransactionsJsonConverter implements JsonDeserializer<HistoryTransaction>, JsonSerializer<HistoryTransaction> {
    private Gson mGson;

    public HistoryTransactionsJsonConverter() {
        GsonBuilder out = MinterExplorerApi.getInstance().getGsonBuilder();
        out.registerTypeAdapter(MinterAddress.class, new MinterAddressJsonConverter());
        out.registerTypeAdapter(MinterPublicKey.class, new MinterPublicKeyJsonConverter());
        out.registerTypeAdapter(MinterHash.class, new MinterHashJsonConverter());
        out.registerTypeAdapter(MinterCheck.class, new MinterCheckJsonConverter());
        out.registerTypeAdapter(BigInteger.class, new BigIntegerJsonConverter());
        out.registerTypeAdapter(BigDecimal.class, new BigDecimalJsonConverter());
        out.registerTypeAdapter(BytesData.class, new BytesDataJsonConverter());

        mGson = out.create();
    }

    @Override
    public HistoryTransaction deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        HistoryTransaction tx = mGson.fromJson(json, HistoryTransaction.class);

        JsonObject base = json.getAsJsonObject();
        if (base.isJsonNull()) {
            return null;
        }
        if (!base.has("data") || base.get("data").isJsonNull()) {
            tx.data = null;
            return tx;
        }

        JsonObject data = base.get("data").getAsJsonObject();
        tx.data = mGson.fromJson(data, tx.getType().getCls());

        return tx;
    }

    @Override
    public JsonElement serialize(HistoryTransaction src, Type typeOfSrc, JsonSerializationContext context) {
        return mGson.toJsonTree(src);
    }
}
