package com.example.app_autocar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Only the currency pair is requested; entered prices stay on the device. */
public final class CurrencyConverter {
    private CurrencyConverter() {}
    public static BigDecimal convert(String amount,String rate,boolean solesToDollars) {
        BigDecimal a=new BigDecimal(amount.replace(',','.')),r=new BigDecimal(rate);
        if(a.signum()<0||r.signum()<=0||a.precision()>16)throw new IllegalArgumentException();
        return (solesToDollars?a.divide(r,8,RoundingMode.HALF_UP):a.multiply(r)).setScale(2,RoundingMode.HALF_UP);
    }
    public static void show(AppCompatActivity activity) {
        new AlertDialog.Builder(activity).setTitle("Soles ↔ Dólares").setView(buildView(activity)).setPositiveButton("Cerrar",null).show();
    }
    public static LinearLayout buildView(AppCompatActivity activity) {
        int padding=Math.round(12*activity.getResources().getDisplayMetrics().density);
        LinearLayout panel=new LinearLayout(activity);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(padding,padding,padding,padding);
        LinearLayout row=new LinearLayout(activity);row.setGravity(android.view.Gravity.CENTER_VERTICAL);panel.addView(row);
        EditText amount=new EditText(activity);amount.setHint("US$");amount.setSingleLine(true);amount.setTextSize(18);amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);LinearLayout inputGroup=new LinearLayout(activity);inputGroup.setOrientation(LinearLayout.VERTICAL);TextView inputCurrency=new TextView(activity);inputCurrency.setTextSize(11);inputGroup.addView(inputCurrency);inputGroup.addView(amount,new LinearLayout.LayoutParams(-1,padding*4));row.addView(inputGroup,new LinearLayout.LayoutParams(0,-2,1));
        Button direction=new Button(activity);direction.setAllCaps(false);direction.setText("⇄");direction.setMinimumWidth(0);direction.setPadding(0,0,0,0);row.addView(direction,new LinearLayout.LayoutParams(padding*4,padding*4));
        EditText result=new EditText(activity);result.setHint("S/");result.setFocusable(false);result.setSingleLine(true);result.setTextSize(18);result.setTextColor(Color.rgb(0,43,92));row.addView(result,new LinearLayout.LayoutParams(0,padding*4,1));
        LinearLayout meta=new LinearLayout(activity);meta.setGravity(android.view.Gravity.CENTER_VERTICAL);meta.setVisibility(android.view.View.GONE);
        TextView info=new TextView(activity);info.setTextSize(11);meta.addView(info,new LinearLayout.LayoutParams(0,-2,1));
        Button retry=new Button(activity);retry.setText("↻");retry.setContentDescription("Actualizar tipo de cambio");retry.setMinimumWidth(0);retry.setPadding(0,0,0,0);meta.addView(retry,new LinearLayout.LayoutParams(padding*4,padding*4));
        TextView credit=new TextView(activity);credit.setText("Tasa diaria referencial · ExchangeRate-API ↗");credit.setTextSize(11);credit.setTextColor(Color.rgb(0,43,92));credit.setTooltipText("No incluye comisiones ni el cambio de compra/venta del banco");credit.setOnClickListener(v->activity.startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.exchangerate-api.com"))));
        SharedPreferences cache=activity.getSharedPreferences("exchange_rate",0);
        String[] rate={cache.getString("pen","")};long[] updated={cache.getLong("updated",0)};boolean[] penToUsd={false};
        Runnable recalculate=()->{inputCurrency.setText(penToUsd[0]?"S/":"US$");direction.setContentDescription(penToUsd[0]?"Cambiar de soles a dólares":"Cambiar de dólares a soles");amount.setHint(penToUsd[0]?"S/":"US$");result.setHint(penToUsd[0]?"US$":"S/");if(rate[0].isEmpty()||amount.getText().toString().isEmpty()){result.setText("");return;}try{result.setText((penToUsd[0]?"US$ ":"S/ ")+convert(amount.getText().toString(),rate[0],penToUsd[0]).toPlainString());}catch(Exception e){result.setText("");}};
        Runnable showRate=()->{String date=DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Lima")).format(Instant.ofEpochSecond(updated[0]));info.setText("1 US$ = S/ "+rate[0]+"\nActualizado: "+date+" · Perú"+(System.currentTimeMillis()/1000-updated[0]>172800?"\nDato antiguo; actualiza antes de comparar.":""));recalculate.run();};
        direction.setOnClickListener(v->{penToUsd[0]=!penToUsd[0];recalculate.run();});
        amount.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){recalculate.run();}public void afterTextChanged(Editable e){}});
        Runnable fetch=()->{
            retry.setEnabled(false);info.setText(rate[0].isEmpty()?"Obteniendo tipo de cambio…":"Actualizando tasa guardada…");
            new Thread(()->{HttpURLConnection connection=null;try{
                connection=(HttpURLConnection)new URL("https://open.er-api.com/v6/latest/USD").openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(10000);
                if(connection.getResponseCode()!=200)throw new java.io.IOException();
                byte[] bytes;try(java.io.InputStream stream=connection.getInputStream()){bytes=stream.readNBytes(100000);}
                JSONObject data=new JSONObject(new String(bytes,StandardCharsets.UTF_8));String value=data.getJSONObject("rates").getString("PEN");long time=data.getLong("time_last_update_unix");
                if(!"success".equals(data.getString("result"))||!"USD".equals(data.getString("base_code"))||new BigDecimal(value).signum()<=0||time<=0)throw new java.io.IOException();
                long next=data.getLong("time_next_update_unix");cache.edit().putString("pen",value).putLong("updated",time).putLong("next",next).putLong("fetched",System.currentTimeMillis()/1000).apply();
                activity.runOnUiThread(()->{if(activity.isDestroyed())return;rate[0]=value;updated[0]=time;showRate.run();retry.setEnabled(true);});
            }catch(Exception e){activity.runOnUiThread(()->{if(activity.isDestroyed())return;if(!rate[0].isEmpty()){showRate.run();info.append("\nSin conexión: tasa guardada, no actualizada.");}else info.setText("No se pudo obtener la tasa. Reintenta.");retry.setEnabled(true);});}finally{if(connection!=null)connection.disconnect();}},"exchange-rate").start();
        };
        retry.setOnClickListener(v->{if(System.currentTimeMillis()/1000-cache.getLong("fetched",0)<3600&&!rate[0].isEmpty())showRate.run();else fetch.run();});
        Button information=new Button(activity);information.setText("ⓘ");information.setContentDescription("Tipo de cambio, fecha y fuente ExchangeRate-API");information.setMinimumWidth(0);information.setPadding(0,0,0,0);row.addView(information,new LinearLayout.LayoutParams(padding*4,padding*4));information.setOnClickListener(v->{LinearLayout details=new LinearLayout(activity);details.setOrientation(LinearLayout.VERTICAL);details.setPadding(padding,padding,padding,padding);TextView status=new TextView(activity);status.setText(info.getText()+"\nTasa diaria referencial. No incluye comisiones ni cotización de compra/venta.");details.addView(status);if(credit.getParent()!=null)((android.view.ViewGroup)credit.getParent()).removeView(credit);details.addView(credit);new AlertDialog.Builder(activity).setTitle("Tipo de cambio").setView(details).setPositiveButton("Actualizar",(d,w)->retry.performClick()).setNegativeButton("Cerrar",null).show();});
        amount.setText("1");recalculate.run();
        if(!rate[0].isEmpty()&&System.currentTimeMillis()/1000<cache.getLong("next",0))showRate.run();else fetch.run();
        return panel;
    }
}
