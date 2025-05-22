package org.upm.btb.accelerometeriot;

import android.util.Log;

import org.json.JSONObject;

import okhttp3.*;

import java.io.IOException;

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";
    private static final OkHttpClient client = new OkHttpClient();

    // Método para enviar telemetría de aceleración por HTTP POST
    public static String sendTelemetryData(JSONObject telemetryData, String targetUrl) {
        // Convertir el JSON a cadena y crear el cuerpo de la solicitud
        String json = telemetryData.toString();
        RequestBody body = RequestBody.create(
                MediaType.get("application/json; charset=utf-8"), json);

        // Imprimir la URL y los parámetros
        Log.d(TAG, "URL: " + targetUrl);
        Log.d(TAG, "Parametros: " + json);  // Aquí estamos imprimiendo el JSON como string

        // Crear la solicitud HTTP POST
        Request request = new Request.Builder()
                .url(targetUrl)
                .post(body)
                .build();

        // Imprimir que la solicitud está siendo enviada
        Log.d(TAG, "Enviando solicitud POST...");

        try (Response response = client.newCall(request).execute()) {
            // Verificar si la solicitud fue exitosa
            if (response.isSuccessful()) {
                // Imprimir que la solicitud se envió correctamente
                Log.d(TAG, "Solicitud POST enviada correctamente. Código de respuesta: " + response.code());
                return response.body().string();  // Devolver la respuesta del servidor
            } else {
                // Imprimir que la solicitud falló
                Log.e(TAG, "Solicitud POST fallida con código: " + response.code());
                return "Error: " + response.code();
            }
        } catch (IOException e) {
            // Imprimir el error en caso de que ocurra una excepción
            Log.e(TAG, "Error durante la solicitud HTTP POST", e);
            return "Request failed: " + e.getMessage();
        }
    }
}
