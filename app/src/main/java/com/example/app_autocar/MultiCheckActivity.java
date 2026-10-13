package com.example.app_autocar;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/** Standalone compatibility screen using the same engine as the unified consultation. */
public class MultiCheckActivity extends AppCompatActivity {
    public static final String EXTRA_PLATE="plate";
    private PriorityQueryEngine engine;
    @Override protected void onCreate(@Nullable Bundle saved){
        super.onCreate(saved);
        engine=new PriorityQueryEngine(this,getIntent());
        engine.start();
        setContentView(engine.view());
    }
    @Override protected void onDestroy(){if(engine!=null)engine.dispose();super.onDestroy();}
}
