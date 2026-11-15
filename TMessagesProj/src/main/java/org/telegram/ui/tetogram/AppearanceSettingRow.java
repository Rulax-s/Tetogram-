/* Tetogram, 2026. GPL v2 or later. Growing settings rows; values never compete for title width. */
package org.telegram.ui.tetogram;
import android.content.Context;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
public final class AppearanceSettingRow extends LinearLayout {
    public AppearanceSettingRow(Context context,CharSequence title,CharSequence value,boolean enabled,Runnable action){this(context,title,value,enabled,null,action);}
    public AppearanceSettingRow(Context context,CharSequence title,CharSequence value,boolean enabled,Boolean checked,Runnable action){
        super(context);setOrientation(HORIZONTAL);setGravity(Gravity.CENTER_VERTICAL);setPadding(AndroidUtilities.dp(20),AndroidUtilities.dp(12),AndroidUtilities.dp(20),AndroidUtilities.dp(12));setMinimumHeight(AndroidUtilities.dp(56));
        LinearLayout column=new LinearLayout(context);column.setOrientation(VERTICAL);addView(column,new LayoutParams(0,-2,1));
        TextView name=new TextView(context);name.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);name.setText(title);name.setSingleLine(false);name.setMaxLines(Integer.MAX_VALUE);ThemeBind.textColor(name,Theme.key_windowBackgroundWhiteBlackText);column.addView(name,new LayoutParams(-1,-2));
        if(value!=null&&value.length()>0&&!value.toString().contentEquals(title)){TextView detail=new TextView(context);detail.setTextSize(TypedValue.COMPLEX_UNIT_SP,13);detail.setText(value);detail.setSingleLine(false);ThemeBind.textColor(detail,Theme.key_windowBackgroundWhiteGrayText);LayoutParams lp=new LayoutParams(-1,-2);lp.topMargin=AndroidUtilities.dp(4);column.addView(detail,lp);}
        if(checked!=null){org.telegram.ui.Components.Switch toggle=new org.telegram.ui.Components.Switch(context);toggle.setChecked(checked,false);toggle.setClickable(false);toggle.setFocusable(false);toggle.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);LayoutParams lp=new LayoutParams(AndroidUtilities.dp(40),AndroidUtilities.dp(32));lp.leftMargin=AndroidUtilities.dp(16);addView(toggle,lp);setAccessibilityDelegate(new android.view.View.AccessibilityDelegate(){@Override public void onInitializeAccessibilityNodeInfo(android.view.View view,android.view.accessibility.AccessibilityNodeInfo info){super.onInitializeAccessibilityNodeInfo(view,info);info.setClassName("android.widget.Switch");info.setCheckable(true);info.setChecked(checked);}});}
        setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector),2));setEnabled(enabled);setAlpha(enabled?1:.5f);setFocusable(true);setOnClickListener(v->{if(isEnabled())action.run();});
    }
}
