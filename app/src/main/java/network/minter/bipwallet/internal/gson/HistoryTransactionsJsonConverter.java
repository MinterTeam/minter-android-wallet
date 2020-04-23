/*
 * Copyright (C) by MinterTeam. 2020
 * @link <a href="https://github.com/MinterTeam">Org Github</a>
 * @link <a href="https://github.com/edwardstock">Maintainer Github</a>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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

import network.minter.explorer.MinterExplorerApi;
import network.minter.explorer.models.HistoryTransaction;

public class HistoryTransactionsJsonConverter implements JsonDeserializer<HistoryTransaction>, JsonSerializer<HistoryTransaction> {
    private Gson mGson;

    public HistoryTransactionsJsonConverter() {
        GsonBuilder out = MinterExplorerApi.getInstance().getGsonBuilder();

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
