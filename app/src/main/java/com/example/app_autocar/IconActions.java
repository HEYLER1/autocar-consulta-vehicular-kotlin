package com.example.app_autocar;

import android.content.Context;
import android.graphics.Color;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;

/** Familiar icon actions with a 48dp touch area, tooltip and screen-reader label. */
public final class IconActions {
  private IconActions() {}

  public static MaterialButton button(Context context, int icon, String label) {
    MaterialButton b = new MaterialButton(context);
    b.setText("");
    b.setElevation(0);
    b.setStateListAnimator(null);
    b.setRippleColor(android.content.res.ColorStateList.valueOf(Color.argb(25, 90, 130, 180)));
    b.setIconResource(icon);
    b.setIconSize(Math.round(18 * context.getResources().getDisplayMetrics().density));
    b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
    b.setIconPadding(0);
    b.setMinWidth(0);
    b.setMinimumWidth(0);
    b.setPadding(0, 0, 0, 0);
    b.setInsetTop(0);
    b.setInsetBottom(0);
    b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.TRANSPARENT));
    b.setIconTint(android.content.res.ColorStateList.valueOf(Color.rgb(0, 43, 92)));
    b.setContentDescription(label);
    b.setTooltipText(label);
    return b;
  }

  public static void apply(TextView view, int icon, String label) {
    android.graphics.drawable.Drawable drawable =
        ContextCompat.getDrawable(view.getContext(), icon);
    int size = Math.round(18 * view.getResources().getDisplayMetrics().density);
    drawable.setBounds(0, 0, size, size);
    drawable.setTint(Color.rgb(0, 43, 92));
    view.setCompoundDrawables(drawable, null, null, null);
    view.setText("");
    view.setContentDescription(label);
    view.setTooltipText(label);
  }
}
