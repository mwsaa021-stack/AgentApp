package com.example.agent;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final String API_URL =
        "https://script.google.com/macros/s/AKfycbyNdbtVflpWwWsqMdCs9dMKMqzW09TviaOZ66OpVDVY5pj6QRqlHnoC7Gn23bGkJ6ldbQ/exec";

    private LinearLayout messages;
    private EditText input;
    private Button sendButton;
    private ScrollView scroll;

    private final ExecutorService executor =
        Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 20, 20, 20);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("المساعد الذكي");
        title.setTextSize(24);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 10, 0, 20);
        root.addView(title);

        scroll = new ScrollView(this);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(messages);

        root.addView(scroll,
            new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);

        input = new EditText(this);
        input.setHint("اكتب رسالتك هنا");
        input.setTextSize(16);

        bottom.addView(input,
            new LinearLayout.LayoutParams(0, -2, 1));

        sendButton = new Button(this);
        sendButton.setText("إرسال");
        bottom.addView(sendButton);

        root.addView(bottom);
        setContentView(root);

        addMessage("المساعد الذكي جاهز. اكتب رسالتك.", false);

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String message = input.getText().toString().trim();
        if (message.isEmpty()) return;

        input.setText("");
        addMessage("أنت: " + message, false);
        sendButton.setEnabled(false);
        addMessage("جاري الاتصال...", true);

        executor.execute(() -> {
            String reply;

            try {
                JSONObject body = new JSONObject();
                body.put("message", message);

                HttpURLConnection connection =
                    (HttpURLConnection)
                    new URL(API_URL).openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(20000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
                );

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.toString().getBytes(
                        StandardCharsets.UTF_8));
                }

                int status = connection.getResponseCode();

                InputStream stream =
                    status >= 200 && status < 400
                    ? connection.getInputStream()
                    : connection.getErrorStream();

                if (stream == null) {
                    throw new IOException("HTTP " + status);
                }

                StringBuilder result = new StringBuilder();

                try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(
                        stream, StandardCharsets.UTF_8))) {

                    String line;
                    while ((line = reader.readLine()) != null) {
                        result.append(line);
                    }
                }

                connection.disconnect();

                JSONObject response =
                    new JSONObject(result.toString());

                reply = response.has("reply")
                    ? response.getString("reply")
                    : "خطأ من الخادم: "
                      + response.optString("error",
                          result.toString());

            } catch (Exception e) {
                reply = "تعذر الاتصال: " + e.getMessage();
            }

            String finalReply = reply;

            runOnUiThread(() -> {
                removeLastMessage();
                addMessage("المساعد: " + finalReply, false);
                sendButton.setEnabled(true);
            });
        });
    }

    private void addMessage(String text, boolean temporary) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(17);
        view.setTextColor(Color.BLACK);
        view.setPadding(12, 12, 12, 12);
        view.setTag(temporary ? "temporary" : "normal");

        messages.addView(view);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void removeLastMessage() {
        int count = messages.getChildCount();

        if (count > 0 &&
            "temporary".equals(
                messages.getChildAt(count - 1).getTag())) {
            messages.removeViewAt(count - 1);
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
