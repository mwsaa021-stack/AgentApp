package com.example.agent;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private LinearLayout chatLayout;
    private EditText input;
    private Button sendButton;
    private ScrollView scrollView;

    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    // ضع رابط Apps Script هنا
    private static final String API_URL =
            https://script.google.com/macros/s/AKfycbxVxdBLRPQWHR0mjtciLt25D8ibsIfRX3T3YhuR8sa8DFaalgAUJnrQPS9Jy1F46_n8/exec";

    private TextView statusMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("AgentApp - مساعدك الذكي");
        title.setTextSize(22);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        title.setPadding(8, 16, 8, 16);
        root.addView(title);

        scrollView = new ScrollView(this);

        chatLayout = new LinearLayout(this);
        chatLayout.setOrientation(LinearLayout.VERTICAL);
        chatLayout.setPadding(4, 8, 4, 8);

        scrollView.addView(chatLayout);

        root.addView(scrollView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1
        ));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);

        input = new EditText(this);
        input.setHint("اكتب رسالتك هنا...");
        input.setTextSize(16);

        bottom.addView(input, new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));

        sendButton = new Button(this);
        sendButton.setText("إرسال");
        bottom.addView(sendButton);

        root.addView(bottom);

        setContentView(root);

        addMessage("AgentApp",
                "مرحبًا! اكتب سؤالك وسأرسله إلى Gemini.");

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void addMessage(String sender, String message) {
        TextView text = new TextView(this);
        text.setText(sender + ":\n" + message);
        text.setTextSize(16);
        text.setTextColor(Color.BLACK);
        text.setPadding(12, 12, 12, 12);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        params.setMargins(0, 6, 0, 6);
        text.setLayoutParams(params);

        chatLayout.addView(text);

        scrollView.post(() ->
                scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessage() {
        String message = input.getText().toString().trim();

        if (message.isEmpty()) {
            return;
        }

        if (API_URL.startsWith("PASTE_") ||
                !API_URL.startsWith("https://")) {
            Toast.makeText(this,
                    "ضع رابط Apps Script الصحيح أولًا",
                    Toast.LENGTH_LONG).show();
            return;
        }

        addMessage("أنت", message);
        input.setText("");

        sendButton.setEnabled(false);

        statusMessage = new TextView(this);
        statusMessage.setText("Gemini يفكر...");
        statusMessage.setTextSize(16);
        statusMessage.setTextColor(Color.DKGRAY);
        statusMessage.setPadding(12, 12, 12, 12);
        chatLayout.addView(statusMessage);

        scrollView.post(() ->
                scrollView.fullScroll(View.FOCUS_DOWN));

        executor.execute(() -> {
            String answer;

            try {
                answer = askGemini(message);
            } catch (Exception e) {
                answer = "تعذر الاتصال: " +
                        e.getMessage();
            }

            final String result = answer;

            runOnUiThread(() -> {
                if (statusMessage != null) {
                    chatLayout.removeView(statusMessage);
                    statusMessage = null;
                }

                addMessage("Gemini", result);
                sendButton.setEnabled(true);
            });
        });
    }

    private String askGemini(String message)
            throws Exception {

        HttpURLConnection connection = null;

        try {
            URL url = new URL(API_URL);

            connection =
                    (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("POST");
            connection.setConnectTimeout(20000);
            connection.setReadTimeout(60000);
            connection.setDoOutput(true);

            connection.setRequestProperty(
                    "Content-Type",
                    "application/json; charset=UTF-8"
            );

            JSONObject body = new JSONObject();
            body.put("message", message);

            byte[] data = body.toString()
                    .getBytes(StandardCharsets.UTF_8);

            try (OutputStream os =
                         connection.getOutputStream()) {
                os.write(data);
            }

            int status = connection.getResponseCode();

            InputStream stream =
                    status >= 200 && status < 400
                            ? connection.getInputStream()
                            : connection.getErrorStream();

            if (stream == null) {
                throw new Exception(
                        "خطأ اتصال HTTP: " + status);
            }

            StringBuilder response =
                    new StringBuilder();

            try (BufferedReader reader =
                         new BufferedReader(
                                 new InputStreamReader(
                                         stream,
                                         StandardCharsets.UTF_8))) {

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            JSONObject result =
                    new JSONObject(response.toString());

            if (result.has("error")) {
                return "خطأ: " +
                        result.optString("error");
            }

            return result.optString(
                    "reply",
                    "لم يصل رد مفهوم من Gemini."
            );

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
