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

package network.minter.bipwallet.tests.internal;

import android.content.Context;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import network.minter.core.internal.log.Mint;
import network.minter.core.internal.log.StdLogger;
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

    static {
        Mint.brew(new StdLogger());
    }

    private final String mPrefix;
    private final Map<String, String> mOverrides = new HashMap<>();
    private boolean mUnitTest = true;
    private WeakReference<Context> mContext;

    public ApiMockInterceptor() {
        this("");
    }

    public ApiMockInterceptor(String prefix) {
        mPrefix = prefix;
    }

    public ApiMockInterceptor(String prefix, Context context) {
        mPrefix = prefix;
        mUnitTest = false;
        mContext = new WeakReference<>(context);
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

    public ApiMockInterceptor override(String url, String filename) {
        mOverrides.put(mPrefix + "/" + url, filename);
        return this;
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
        } else {
            System.out.println(String.format("Not found override for %s", fileName));
        }

        if (mUnitTest) {
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
                throw new FileNotFoundException(String.format(readFile(errorFile), String.format("Mock response for request %s/%s not found: file name: assets:%s", request.url().encodedPath(), firstNonNull(request.url().encodedQuery(), ""), fileName)));
            }

            return readFile(file);
        } else {
            InputStream is = tryReadAsset(fileName);

            if (is == null) {
                is = tryReadAsset(fileName + "requests.json");
            }

            if (is == null) {
                is = tryReadAsset("not_found.json");

                if (is != null) {
                    throw new FileNotFoundException(
                            String.format(
                                    readStream(is),
                                    String.format("Mock response for request %s/%s not found: file name: %s", request.url().encodedPath(), firstNonNull(request.url().encodedQuery(), ""), fileName),
                                    String.format("Mock response for request %s/%s not found: file name: %s", request.url().encodedPath(), firstNonNull(request.url().encodedQuery(), ""), fileName)
                            )
                    );
                } else {
                    throw new FileNotFoundException(String.format("{\"error:{\"message\":\"%s\"}\", \"log\":\"%s\"}",
                            String.format("Mock response for request %s/%s not found: file name: %s", request.url().encodedPath(), firstNonNull(request.url().encodedQuery(), ""), fileName),
                            String.format("Mock response for request %s/%s not found: file name: %s", request.url().encodedPath(), firstNonNull(request.url().encodedQuery(), ""), fileName)
                    ));
                }
            }

            return readStream(is);
        }
    }

    private String readStream(InputStream is) throws IOException {
        try {
            int sz = is.available();
            byte[] buffer = new byte[sz];
            is.read(buffer, 0, sz);

            return new String(buffer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw e;
        }
    }

    private InputStream tryReadAsset(String fileName) {
        InputStream is;
        try {
            is = mContext.get().getResources().getAssets().open(fileName + ".json");
        } catch (IOException e) {
            is = getClass().getClassLoader().getResourceAsStream("assets" + File.separator + fileName + ".json");
        }

        return is;
    }

    private final static class RequestDoc {
        String request;
        JsonObject response;
    }
}
