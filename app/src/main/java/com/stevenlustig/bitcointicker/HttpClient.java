package com.stevenlustig.bitcointicker;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

/**
 * Gets the current and last seen rate
 */
public class HttpClient {
    public static Result getCurrentRate(Context context) throws Exception {
        URL url = new URL("https://api.coindesk.com/v1/bpi/currentprice.json");

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);

        if (connection.getResponseCode() == 200) {
            try (InputStream is = connection.getInputStream()) {
                StringBuilder stringBuilder = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    int c;
                    while ((c = reader.read()) != -1) {
                        stringBuilder.append((char) c);
                    }

                    JSONObject jsonObject = new JSONObject(stringBuilder.toString());
                    JSONObject USD = jsonObject.getJSONObject("bpi").getJSONObject("USD");
                    Spanned symbol = Html.fromHtml(USD.getString("symbol"));

                    float rate = (float) USD.getDouble("rate_float");
                    float lastSeenRate = SharedPreferenceManager.getLastSeenRate(context);

                    return new Result(rate, lastSeenRate, symbol.toString());
                }
            }
        }

        throw new Exception("HTTP Response: " + connection.getResponseCode());
    }
}
