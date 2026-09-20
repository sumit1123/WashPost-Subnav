package com.wapo.flagship.di.core.modules;

/*
 * Copyright (C) 2017 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.annotation.Nullable;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.wapo.android.commons.logs.EventLog;
import com.wapo.android.remotelog.logger.RemoteLog;
import com.wapo.flagship.FlagshipApplication;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;


public final class DefaultOnDataMismatchAdapter<T> extends JsonAdapter<T> {
    private final JsonAdapter<T> delegate;
    private final T defaultValue;

    private DefaultOnDataMismatchAdapter(JsonAdapter<T> delegate, T defaultValue) {
        this.delegate = delegate;
        this.defaultValue = defaultValue;
    }

    @Override public T fromJson(JsonReader reader) throws IOException {
        // Use a peeked reader to leave the reader in a known state even if there's an exception.
        JsonReader peeked = reader.peekJson();
        T result;
        try {
            // Attempt to decode to the target type with the peeked reader.
            result = delegate.fromJson(peeked);
        } catch (Exception e) {
            result = defaultValue;
            try {
                String value = reader.peekJson().readJsonValue().toString();
                RemoteLog.e(FlagshipApplication.getInstance(), new EventLog.Builder()
                        .setMessage("Article Error. Item type mismatch in " + delegate.toString().split("\\.")[0])
                        .setErrorMessage(e.getMessage())
                        .set("for", (value.length() > 1000 ? value.substring(0, 1000) + "..." : value)).build());
            } catch (Exception err) {
                RemoteLog.e(FlagshipApplication.getInstance(), new EventLog.Builder()
                        .setMessage("Article Error. Item type mismatch")
                        .setErrorMessage(e.getMessage()).build());
            }
        } finally {
            peeked.close();
        }
        // Skip the value back on the reader, no matter the state of the peeked reader.
        reader.skipValue();
        return result;
    }

    @Override public void toJson(JsonWriter writer, T value) throws IOException {
        try {
            delegate.toJson(writer, value);
        } catch (Exception e) {
            writer.endObject();
            String msg = e.getMessage();
            RemoteLog.e(FlagshipApplication.getInstance(), new EventLog.Builder()
                    .setMessage("Article Error. Exception in toJson")
                    .setErrorMessage(msg.length() > 1000 ? msg.substring(msg.length() - 1000) + "..." : msg).build());
        }
    }

    public static <T> Factory newFactory(final Class<T> type, final T defaultValue) {
        return new Factory() {
            @Override public @Nullable
            JsonAdapter<?> create(
                    Type requestedType, Set<? extends Annotation> annotations, Moshi moshi) {
                if (type != requestedType) return null;
                JsonAdapter<T> delegate = moshi.nextAdapter(this, type, annotations);
                return new DefaultOnDataMismatchAdapter<>(delegate, defaultValue);
            }
        };
    }
}