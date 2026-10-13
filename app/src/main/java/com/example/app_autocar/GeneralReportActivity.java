package com.example.app_autocar;

import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import org.json.JSONObject;

/** Full-screen, frozen report snapshot. Going back preserves the ongoing query in Home. */
public class GeneralReportActivity extends AppCompatActivity {
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setLineSpacing(dp(4),1);return t;}
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        try{
            java.io.File folder=new java.io.File(getCacheDir(),"reports");String name=getIntent().getStringExtra("snapshot");if(name==null||!name.matches("general_[a-f0-9-]+\\.json"))throw new IllegalArgumentException();
            java.io.File file=new java.io.File(folder,name);JSONObject data;
            try(java.io.FileInputStream input=new java.io.FileInputStream(file)){data=new JSONObject(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));}
            LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(244,247,251));
            ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{androidx.core.graphics.Insets b=insets.getInsets(WindowInsetsCompat.Type.systemBars());v.setPadding(0,b.top,0,b.bottom);return insets;});
            ScrollView scroll=new ScrollView(this);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
            LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(22),dp(8),dp(22),dp(22));GradientDrawable gradient=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(0,43,92),Color.rgb(20,83,130)});hero.setBackground(gradient);body.addView(hero);
            TextView back=text("‹ Volver a Inicio",14,Color.WHITE);back.setMinHeight(dp(48));back.setGravity(android.view.Gravity.CENTER_VERTICAL);back.setOnClickListener(v->finish());hero.addView(back);
            TextView title=text("Informe del vehículo",27,Color.WHITE);title.setTypeface(null,Typeface.BOLD);hero.addView(title);
            TextView plate=text(PlateUtils.display(data.optString("plate")),25,Color.rgb(255,214,0));plate.setTypeface(null,Typeface.BOLD);plate.setPadding(0,dp(10),0,dp(8));hero.addView(plate);
            hero.addView(text(data.optString("progress"),13,Color.WHITE));
            java.text.SimpleDateFormat date=new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm",java.util.Locale.ROOT);date.setTimeZone(java.util.TimeZone.getTimeZone("America/Lima"));hero.addView(text("Captura del informe: "+date.format(new java.util.Date(data.optLong("created")))+" · Perú",11,Color.rgb(214,228,242)));
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(18),dp(18),dp(18),dp(18));GradientDrawable bg=new GradientDrawable();bg.setColor(Color.WHITE);bg.setCornerRadius(dp(16));card.setBackground(bg);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(16),dp(16),dp(16),dp(12));body.addView(card,p);
            String summary=data.optString("summary");int first=summary.indexOf('\n');String content=first>=0?summary.substring(first+1):summary;
            for(String part:content.trim().split("\\n\\n")){String[] lines=part.split("\\n",2);TextView heading=text(lines[0],16,Color.rgb(0,43,92));heading.setTypeface(null,Typeface.BOLD);heading.setPadding(0,dp(8),0,dp(8));card.addView(heading);if(lines.length>1){TextView details=text(lines[1],15,Color.rgb(31,41,55));details.setTextIsSelectable(true);card.addView(details);}}
            TextView complete=text(data.optString("detail"),14,Color.rgb(31,41,55));complete.setTextIsSelectable(true);complete.setPadding(dp(18),0,dp(18),dp(20));complete.setVisibility(View.GONE);
            TextView expand=text("Ver información completa ▾",14,Color.rgb(0,43,92));expand.setMinHeight(dp(48));expand.setGravity(android.view.Gravity.CENTER_VERTICAL);expand.setPadding(dp(20),0,dp(20),0);body.addView(expand);body.addView(complete);expand.setOnClickListener(v->{boolean open=complete.getVisibility()!=View.VISIBLE;complete.setVisibility(open?View.VISIBLE:View.GONE);expand.setText(open?"Ocultar información completa ▴":"Ver información completa ▾");});
            Button pdf=new Button(this);pdf.setText("Guardar / compartir PDF");pdf.setAllCaps(false);pdf.setTextColor(Color.WHITE);pdf.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(0,43,92)));root.addView(pdf,new LinearLayout.LayoutParams(-1,dp(56)));pdf.setOnClickListener(v->{String pdfName=data.optString("pdf");if(pdfName.isEmpty())return;java.io.File report=new java.io.File(folder,pdfName);try{if(!report.getCanonicalFile().getParentFile().equals(folder.getCanonicalFile())||!report.isFile())throw new IllegalArgumentException();android.net.Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".reports",report);android.content.Intent share=new android.content.Intent(android.content.Intent.ACTION_SEND).setType("application/pdf").putExtra(android.content.Intent.EXTRA_STREAM,uri).addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);share.setClipData(android.content.ClipData.newRawUri("Informe AUTOCAR",uri));startActivity(android.content.Intent.createChooser(share,"Guardar informe"));}catch(Exception e){Toast.makeText(this,"El PDF ya no está disponible. Abre de nuevo el informe desde Inicio.",Toast.LENGTH_SHORT).show();}});
            setContentView(root);
        }catch(Exception e){Toast.makeText(this,"No se pudo cargar el informe",Toast.LENGTH_SHORT).show();finish();}
    }
}
