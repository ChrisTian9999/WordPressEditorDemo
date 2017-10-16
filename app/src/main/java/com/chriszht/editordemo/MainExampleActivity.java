package com.chriszht.editordemo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import org.wordpress.android.editor.Utils;

public class MainExampleActivity extends AppCompatActivity {
    private Activity mActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this;
        setContentView(R.layout.activity_example);

        Button newEditorPost1 = (Button) findViewById(R.id.new_editor_post_1);
        newEditorPost1.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(MainExampleActivity.this, EditorExampleActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(EditorExampleActivity.TITLE_PARAM, getString(R.string.example_post_visual_title));
                bundle.putString(EditorExampleActivity.CONTENT_PARAM, Utils.getHtmlFromFile(mActivity,
                        "example/example-content.html"));
                bundle.putString(EditorExampleActivity.TITLE_PLACEHOLDER_PARAM,
                        getString(R.string.example_post_title_placeholder));
                bundle.putString(EditorExampleActivity.CONTENT_PLACEHOLDER_PARAM,
                        getString(R.string.example_post_content_placeholder));
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        Button newEditorPostEmpty = (Button) findViewById(R.id.new_editor_post_empty);
        newEditorPostEmpty.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                Intent intent = new Intent(MainExampleActivity.this, EditorExampleActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString(EditorExampleActivity.TITLE_PARAM, "");
                bundle.putString(EditorExampleActivity.CONTENT_PARAM, "");
                bundle.putString(EditorExampleActivity.TITLE_PLACEHOLDER_PARAM,
                        getString(R.string.example_post_title_placeholder));
                bundle.putString(EditorExampleActivity.CONTENT_PLACEHOLDER_PARAM,
                        getString(R.string.example_post_content_placeholder));
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });
    }
}
