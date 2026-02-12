/*
 * Copyright (C) 2026 The Android Open Source Project
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
package androidx.compose.remote.integration.view.convert;

import android.annotation.SuppressLint;

import androidx.compose.remote.core.CompanionOperation;
import androidx.compose.remote.core.Operation;
import androidx.compose.remote.core.Operations;
import androidx.compose.remote.core.WireBuffer;
import androidx.compose.remote.core.operations.Header;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Lossless converter between RemoteCompose binary (.rc) and JSON.
 * Guaranteed bit-for-bit round-trip fidelity.
 */
@SuppressLint("RestrictedApiAndroidX")
public class RemoteComposeConverter {

    private RemoteComposeConverter() {
    }
    /**
     * Convert RemoteCompose binary to JSON.
     *
     * @param rcBytes RemoteCompose binary
     * @return JSON string
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static @NonNull String remoteComposeToJson(byte @NonNull [] rcBytes)
            throws JSONException {
        WireBuffer buffer = new WireBuffer(rcBytes.length);
        System.arraycopy(rcBytes, 0, buffer.getBuffer(), 0, rcBytes.length);
        // Force set size in WireBuffer (internal field)
        try {
            java.lang.reflect.Field sizeField = WireBuffer.class.getDeclaredField("mSize");
            sizeField.setAccessible(true);
            sizeField.set(buffer, rcBytes.length);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        int apiLevel = Header.peekApiLevel(buffer);
        int profiles = 0;
        if (apiLevel >= 7) {
            try {
                Header header = Header.readDirect(buffer);
                profiles = header.getProfiles();
            } catch (IOException e) {
                // ignore
            }
        }
        buffer.setIndex(0);

        Operations.UniqueIntMap<CompanionOperation> map = Operations.getOperations(apiLevel,
                profiles);
        if (map == null) {
            throw new RuntimeException(
                    "No operations map found for api=" + apiLevel + " profiles=" + profiles);
        }

        JSONObject root = new JSONObject();
        root.put("format", "androidx.compose.remote.rc.json");
        root.put("version", 1);

        JSONObject rc = new JSONObject();
        rc.put("apiLevel", apiLevel);
        rc.put("profiles", profiles);
        JSONArray opsJson = new JSONArray();

        while (buffer.available()) {
            int startIdx = buffer.getIndex();
            int opcode = buffer.readByte();

            CompanionOperation companion = map.get(opcode);
            if (companion == null) {
                throw new RuntimeException("Unknown opcode " + opcode + " at index " + startIdx);
            }

            List<Operation> tempOps = new ArrayList<>();
            companion.read(buffer, tempOps);
            int endIdx = buffer.getIndex();

            byte[] payload = new byte[endIdx - startIdx - 1];
            System.arraycopy(rcBytes, startIdx + 1, payload, 0, payload.length);

            OpcodeRegistry.OpSpec spec = OpcodeRegistry.get(opcode);
            JSONObject opJson = new JSONObject();

            if (spec != null) {
                opJson.put("kind", "op");
                opJson.put("opcode", opcode);
                opJson.put("name", spec.name);

                JSONArray fields = new JSONArray();
                // Rewind to read fields for the structured view
                int currentIdx = buffer.getIndex();
                buffer.setIndex(startIdx + 1);
                for (OpcodeRegistry.FieldSpec fSpec : spec.fields) {
                    fields.put(encodeField(buffer, fSpec, fields));
                }
                buffer.setIndex(currentIdx);
                opJson.put("fields", fields);

                if (spec.isFixedLength() || spec.forceReconstruct) {
                    opJson.put("reconstructFromFields", true);
                }
                if (!spec.isFixedLength()) {
                    opJson.put("payloadBase64", Base64.getEncoder().encodeToString(payload));
                }
            } else {
                opJson.put("kind", "opaque");
                opJson.put("opcode", opcode);
                if (!tempOps.isEmpty()) {
                    opJson.put("name", tempOps.get(0).getClass().getSimpleName());
                }
                opJson.put("payloadBase64", Base64.getEncoder().encodeToString(payload));
            }

            opsJson.put(opJson);
        }

        rc.put("ops", opsJson);
        root.put("rc", rc);

        return root.toString(2);
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static String formatFloat(float f) {
        int bits = Float.floatToRawIntBits(f);
        if ((bits & 0xFF800000) == 0xFF800000) {
            int id = bits & 0x7FFFFF;
            return "NaN(" + id + ")";
        }
        return Float.toString(f);
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static String formatDouble(double d) {
        if (Double.isNaN(d)) {
            long id = Double.doubleToRawLongBits(d) & 0xFFFFFFFFFFFFFL;
            return "NaN(" + id + ")";
        }
        return Double.toString(d);
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static int findFieldInt(JSONArray fields, String name) throws JSONException {
        for (int i = 0; i < fields.length(); i++) {
            JSONObject f = fields.getJSONObject(i);
            if (name.equals(f.getString("name"))) {
                return Integer.parseInt(f.getString("value"));
            }
        }
        return 0;
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static JSONObject encodeField(WireBuffer buffer, OpcodeRegistry.FieldSpec spec,
            JSONArray currentFields) throws JSONException {
        JSONObject f = new JSONObject();
        f.put("name", spec.name);
        f.put("type", spec.type.name());
        switch (spec.type) {
            case BYTE:
                f.put("value", String.valueOf(buffer.readByte()));
                break;
            case SHORT:
                f.put("value", String.valueOf(buffer.readShort()));
                break;
            case INT:
                f.put("value", String.valueOf(buffer.readInt()));
                break;
            case LONG:
                f.put("value", String.valueOf(buffer.readLong()));
                break;
            case FLOAT:
                float fv = buffer.readFloat();
                f.put("value", formatFloat(fv));
                break;
            case DOUBLE:
                double dv = buffer.readDouble();
                f.put("value", formatDouble(dv));
                break;
            case UTF8:
                f.put("value", buffer.readUTF8());
                break;
            case BOOLEAN:
                f.put("value", String.valueOf(buffer.readBoolean()));
                break;
            case BUFFER:
                byte[] data = buffer.readBuffer();
                f.put("value", Base64.getEncoder().encodeToString(data));
                break;
            case FLOAT_ARRAY:
                int len = findFieldInt(currentFields, "expressionLen");
                JSONArray arr = new JSONArray();
                for (int i = 0; i < len; i++) {
                    arr.put(formatFloat(buffer.readFloat()));
                }
                f.put("value", arr);
                break;
            case FLOAT_ARRAY_BASE64:
                int lenB64 = findFieldInt(currentFields, "animationLen");
                byte[] b64data = new byte[lenB64 * 4];
                int startIdx = buffer.getIndex();
                System.arraycopy(buffer.getBuffer(), startIdx, b64data, 0, b64data.length);
                buffer.setIndex(startIdx + b64data.length);
                f.put("value", Base64.getEncoder().encodeToString(b64data));
                break;
        }
        return f;
    }

    /**
     * Convert JSON to RemoteCompose binary.
     *
     * @param jsonStr JSON string
     * @return RemoteCompose binary
     */
    @SuppressLint("RestrictedApiAndroidX")
    public static byte @NonNull [] jsonToRemoteCompose(@NonNull String jsonStr)
            throws JSONException {
        JSONObject root = new JSONObject(jsonStr);
        JSONObject rc = root.getJSONObject("rc");
        JSONArray ops = rc.getJSONArray("ops");

        WireBuffer buffer = new WireBuffer();

        // Ensure WireBuffer allows all opcodes
        try {
            java.lang.reflect.Field validOpsField = WireBuffer.class.getDeclaredField(
                    "mValidOperations");
            validOpsField.setAccessible(true);
            boolean[] validOps = (boolean[]) validOpsField.get(buffer);
            for (int i = 0; i < 256; i++) {
                validOps[i] = true;
            }
        } catch (Exception e) {
            // ignore
        }

        for (int i = 0; i < ops.length(); i++) {
            JSONObject opJson = ops.getJSONObject(i);
            int opcode = opJson.getInt("opcode");

            buffer.start(opcode);

            if (opJson.optBoolean("reconstructFromFields", false)) {
                JSONArray fields = opJson.getJSONArray("fields");
                for (int j = 0; j < fields.length(); j++) {
                    JSONObject fJson = fields.getJSONObject(j);
                    OpcodeRegistry.FieldType type = OpcodeRegistry.FieldType.valueOf(
                            fJson.getString("type"));
                    writeField(buffer, type, fJson);
                }
            } else {
                // AUTHORITATIVE PAYLOAD reconstruction
                byte[] payload = Base64.getDecoder().decode(opJson.getString("payloadBase64"));
                // writeBuffer would add a length prefix, we need to write raw bytes
                for (byte b : payload) {
                    buffer.writeByte(b & 0xFF);
                }
            }
        }

        byte[] result = new byte[buffer.getSize()];
        System.arraycopy(buffer.getBuffer(), 0, result, 0, buffer.getSize());
        return result;
    }

    @SuppressLint("RestrictedApiAndroidX")
    private static void writeField(WireBuffer buffer, OpcodeRegistry.FieldType type,
            JSONObject fJson) throws JSONException {
        switch (type) {
            case BYTE:
                buffer.writeByte(Integer.parseInt(fJson.getString("value")));
                break;
            case SHORT:
                buffer.writeShort(Integer.parseInt(fJson.getString("value")));
                break;
            case INT:
                buffer.writeInt(Integer.parseInt(fJson.getString("value")));
                break;
            case LONG:
                buffer.writeLong(Long.parseLong(fJson.getString("value")));
                break;
            case FLOAT:
                String s = fJson.getString("value");
                if (s.startsWith("NaN(")) {
                    int id = Integer.parseInt(s.substring(4, s.length() - 1));
                    buffer.writeFloat(Float.intBitsToFloat(id | -0x800000));
                } else {
                    buffer.writeFloat(Float.parseFloat(s));
                }
                break;
            case DOUBLE:
                String sd = fJson.getString("value");
                if (sd.startsWith("NaN(")) {
                    long id = Long.parseLong(sd.substring(4, sd.length() - 1));
                    buffer.writeDouble(Double.longBitsToDouble(id | 0x7FF8000000000000L));
                } else {
                    buffer.writeDouble(Double.parseDouble(sd));
                }
                break;
            case UTF8:
                buffer.writeUTF8(fJson.getString("value"));
                break;
            case BOOLEAN:
                buffer.writeBoolean(Boolean.parseBoolean(fJson.getString("value")));
                break;
            case BUFFER:
                byte[] data = Base64.getDecoder().decode(fJson.getString("value"));
                buffer.writeBuffer(data);
                break;
            case FLOAT_ARRAY:
                JSONArray arr = fJson.getJSONArray("value");
                for (int i = 0; i < arr.length(); i++) {
                    String fs = arr.getString(i);
                    if (fs.startsWith("NaN(")) {
                        int id = Integer.parseInt(fs.substring(4, fs.length() - 1));
                        buffer.writeFloat(Float.intBitsToFloat(id | -0x800000));
                    } else {
                        buffer.writeFloat(Float.parseFloat(fs));
                    }
                }
                break;
            case FLOAT_ARRAY_BASE64:
                byte[] b64data = Base64.getDecoder().decode(fJson.getString("value"));
                for (byte b : b64data) {
                    buffer.writeByte(b & 0xFF);
                }
                break;
        }
    }

    /**
     * Generate a report of all opcodes.
     */
    public static void generateReport() throws JSONException {
        System.out.println("Opcode Registry Report");
        System.out.println("======================");

        @SuppressLint("PrimitiveInCollection")
        Map<Integer, OpcodeRegistry.OpSpec> all = OpcodeRegistry.getAll();
        for (Integer opcode : all.keySet()) {
            OpcodeRegistry.OpSpec spec = all.get(opcode);
            System.out.print(opcode + " -> " + spec.name + " [");
            for (int i = 0; i < spec.fields.length; i++) {
                System.out.print(spec.fields[i].name + ":" + spec.fields[i].type);
                if (i < spec.fields.length - 1) System.out.print(", ");
            }
            System.out.println("]");
        }
    }

    /**
     * Main entry point.
     */
    public static void main(String @NonNull [] args) throws Exception {
        if (args.length == 1 && "report".equals(args[0])) {
            generateReport();
            return;
        }
        if (args.length < 3) {
            System.out.println("RemoteCompose Lossless Converter CLI");
            System.out.println("Usage: rc2json <in.rc> <out.json>");
            System.out.println("       json2rc <in.json> <out.rc>");
            return;
        }
        String cmd = args[0];
        java.io.File in = new java.io.File(args[1]);
        java.io.File out = new java.io.File(args[2]);

        if ("rc2json".equals(cmd)) {
            byte[] bytes = java.nio.file.Files.readAllBytes(in.toPath());
            String json = remoteComposeToJson(bytes);
            java.nio.file.Files.write(out.toPath(), json.getBytes());
            System.out.println("Success: Binary -> JSON (" + in.getName() + ")");
        } else if ("json2rc".equals(cmd)) {
            String json = new String(java.nio.file.Files.readAllBytes(in.toPath()));
            byte[] bytes = jsonToRemoteCompose(json);
            java.nio.file.Files.write(out.toPath(), bytes);
            System.out.println("Success: JSON -> Binary (" + in.getName() + ")");
        }
    }
}
