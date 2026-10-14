package com.example.agent;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import android.text.InputType;

public class MainActivity extends Activity {

    private EditText projectName;
    private EditText sourceCode;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(16, 16, 16, 16);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("منشئ تطبيقات أندرويد");
        title.setTextSize(23);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        projectName = new EditText(this);
        projectName.setSingleLine(true);
        projectName.setHint("اسم التطبيق");
        projectName.setText("MyAndroidApp");
        root.addView(projectName);

        TextView label = new TextView(this);
        label.setText("اكتب كود Java أو Kotlin:");
        label.setTextColor(Color.BLACK);
        label.setTextSize(16);
        root.addView(label);

        sourceCode = new EditText(this);
        sourceCode.setGravity( android.view.Gravity.TOP );
        sourceCode.setMinLines(12);
        sourceCode.setTextSize(14);
        sourceCode.setTypeface(
            android.graphics.Typeface.MONOSPACE
        );
        sourceCode.setHint("اكتب الكود هنا...");
        sourceCode.setInputType(
            InputType.TYPE_CLASS_TEXT
            | InputType.TYPE_TEXT_FLAG_MULTI_LINE
            | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        );

        LinearLayout.LayoutParams codeParams =
            new LinearLayout.LayoutParams(
                -1, 0, 1
            );
        root.addView(sourceCode, codeParams);

        Button buildButton = new Button(this);
        buildButton.setText("تجهيز البناء");
        root.addView(buildButton);

        status = new TextView(this);
        status.setTextColor(Color.DKGRAY);
        status.setTextSize(15);
        root.addView(status);

        buildButton.setOnClickListener(v -> {
            String name = projectName.getText().toString().trim();
            String code = sourceCode.getText().toString().trim();

            if (name.isEmpty() || code.isEmpty()) {
                status.setText("اكتب اسم التطبيق والكود أولًا.");
                return;
            }

            status.setText(
                "تم إدخال الكود. الخطوة التالية هي ربط خادم البناء."
            );
        });

        setContentView(root);
    }
}
