/*
 * Copyright (C) by MinterTeam. 2019
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

package network.minter.bipwallet.tests.internal;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.ResponseBody;

import static network.minter.core.internal.common.Preconditions.firstNonNull;

/**
 * minter-android-explorer. 2019
 * @author Eduard Maximovich [edward.vstock@gmail.com]
 */
public class ApiMockInterceptor implements Interceptor {
    private final static String BASE_PATH = "src/androidTest/assets/";
    private final String mPrefix;
    private Map<String, String> mOverrides = new HashMap<>();

    public ApiMockInterceptor() {
        this("");
    }

    public ApiMockInterceptor(String prefix) {
        mPrefix = prefix;
    }

    public ApiMockInterceptor override(String url, String filename) {
        mOverrides.put(url, filename);
        return this;
    }

    @Override
    public final okhttp3.Response intercept(Chain chain) throws IOException {
        String content;
        try {
            content = readMockResponse(chain.request());
        } catch (FileNotFoundException e) {
            content = e.getMessage();
        }
        ResponseBody rb = ResponseBody.create(MediaType.parse("application/json"), content);

        return new okhttp3.Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(rb)
                .build();
    }

    private String readFile(File file) throws IOException {
        byte[] data = new byte[(int) file.length()];
        DataInputStream is = new DataInputStream(new FileInputStream(file));
        is.readFully(data);

        return new String(data);
    }

    private String readMockResponse(Request request) throws IOException {
        String fileName = mPrefix;
        fileName += request.url().encodedPath();
        if (request.url().encodedQuery() != null) {
            fileName += "/";
            fileName += request.url().encodedQuery();
        }

        if (mOverrides.containsKey(fileName)) {
            fileName = mPrefix + mOverrides.get(fileName);
        }

        File file = new File(BASE_PATH + fileName + ".json");
        if (!file.exists()) {
            file = new File(BASE_PATH + fileName + "/default.json");
        }

        if (!file.exists()) {
            file = new File(BASE_PATH + fileName + "requests.json");
            if (file.exists()) {
                final String doc = readFile(file);
                List<RequestDoc> reqs = new GsonBuilder().create().fromJson(doc, new TypeToken<List<RequestDoc>>() {
                }.getType());
                for (RequestDoc item : reqs) {
                    if (item.equals(request.url().encodedQuery())) {
                        return item.response.toString();
                    }
                }

            }
        }


        if (!file.exists()) {
            File errorFile = new File(BASE_PATH + "not_found.json");
            throw new FileNotFoundException(String.format(readFile(errorFile), String.format("Mock response for request %s/%s not found: file name: %s", request.url().encodedPath(), firstNonNull(request.url().encodedQuery(), ""), fileName)));
        }

        return readFile(file);
    }

    private final static class RequestDoc {
        String request;
        JsonObject response;
    }
}
