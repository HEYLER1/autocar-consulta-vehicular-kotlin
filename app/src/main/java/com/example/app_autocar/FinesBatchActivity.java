package com.example.app_autocar;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/** Compatibility entry for tests and older links; Home embeds the same panel. */
public class FinesBatchActivity extends AppCompatActivity {
    public UnifiedQueryPanel panel;
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        panel=new UnifiedQueryPanel(this,getIntent(),this::finish);
        panel.start();
        if(panel.view()!=null)setContentView(panel.view());
    }
    @Override protected void onDestroy(){if(panel!=null)panel.dispose();super.onDestroy();}
}
