/* Tetogram, 2026. GNU GPL v2 or later. One person's bubble color or outline, in every chat or one group. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.tetogram.AppearanceColors;
import org.telegram.messenger.tetogram.AppearanceConfig;
import org.telegram.messenger.tetogram.AppearancePeople;
import org.telegram.messenger.tetogram.AppearanceStore;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

public final class AppearancePersonActivity extends LabsPage {
    private static final int MODE_FILL=0,MODE_OUTLINE=1,MODE_BOTH=2;
    private static final int[] COLORS={0xff7B5CE6,0xffE8577E,0xffD93A3A,0xffF08C2E,0xffE2B93B,0xff4E9A57,0xff1FA88F,0xff39C5BB,0xff3C8DD9,0xff2E3440,0xff8C8C8C,0xffFFFFFF};
    private final long owner,person,dialog;
    private AppearanceConfig config;
    private long revision;
    private boolean groupOnly,busy,leaving,failed;
    private int mode=MODE_FILL,color=COLORS[0];
    private float opacity=1f,width=AppearancePeople.DEFAULT_OUTLINE;
    private String baseline;
    private AppearancePreview preview;
    private FrameLayout previewHost;
    private TextView saveButton;

    /** @param dialog the chat it was opened from (a group enables "only in this group"), or 0 */
    public AppearancePersonActivity(int account,long person,long dialog){super(account);owner=UserConfig.getInstance(account).getClientUserId();this.person=person;this.dialog=dialog;}
    private boolean valid(){return owner>0&&owner==getUserConfig().getClientUserId()&&person!=0;}
    private boolean group(){return dialog<0;}
    private String scope(){return groupOnly&&group()?"chat:"+dialog:AppearanceConfig.GLOBAL;}
    private String name(long id){if(id>0){TLRPC.User u=getMessagesController().getUser(id);if(u!=null)return UserObject.getUserName(u);}else{TLRPC.Chat c=getMessagesController().getChat(-id);if(c!=null)return c.title;}return "ID "+id;}
    @Override protected CharSequence title(){return getString(R.string.TetogramPersonTitle);}

    @Override public View createView(Context context){
        View root=super.createView(context);
        simpleActionBar.setSubtitle(name(person));
        preview=null;previewHost=new FrameLayout(context);
        LinearLayout dock=new LinearLayout(context);dock.setOrientation(LinearLayout.HORIZONTAL);dock.setPadding(dp(8),dp(6),dp(8),dp(6));ThemeBind.backgroundColor(dock,Theme.key_windowBackgroundWhite);
        TextView cancel=dockButton(getString(R.string.Cancel));cancel.setOnClickListener(v->finishFragment());dock.addView(cancel,new LinearLayout.LayoutParams(0,-2,1));
        saveButton=dockButton(getString(R.string.TetogramAppearanceApply));saveButton.setOnClickListener(v->save());dock.addView(saveButton,new LinearLayout.LayoutParams(0,-2,1));
        setBottomActions(dock);
        return root;
    }
    private TextView dockButton(CharSequence text){TextView view=new TextView(getContext());view.setText(text);view.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);view.setGravity(Gravity.CENTER);view.setPadding(dp(12),dp(12),dp(12),dp(12));ThemeBind.textColor(view,Theme.key_windowBackgroundWhiteBlueText);view.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector),2));return view;}

    @Override public void onResume(){
        super.onResume();if(!valid()){finishFragment();return;}
        if(preview!=null)preview.setResumed(true);
        AppearanceStore.load(currentAccount,(ok,c)->{if(!valid())return;failed=!ok;if(config==null){config=c;revision=c.revision;readStored();baseline=state();}refresh();});
    }
    @Override public void onPause(){if(preview!=null)preview.setResumed(false);super.onPause();}
    @Override public void onFragmentDestroy(){if(preview!=null)preview.setResumed(false);super.onFragmentDestroy();}

    private void readStored(){
        AppearanceConfig.Style chat=group()?config.scopes.get("chat:"+dialog):null;
        groupOnly=chat!=null&&AppearancePeople.read(chat.values::get,person)!=null;
        AppearanceConfig.Style source=groupOnly?chat:config.scopes.get(AppearanceConfig.GLOBAL);
        AppearancePeople.Style stored=source==null?null:AppearancePeople.read(source.values::get,person);
        if(stored==null)return;
        if(stored.fill!=null&&stored.hasOutline())mode=MODE_BOTH;else if(stored.hasOutline())mode=MODE_OUTLINE;else mode=MODE_FILL;
        int base=stored.hasOutline()?stored.outline:stored.fill;color=base|0xff000000;
        if(stored.fill!=null)opacity=(stored.fill>>>24)/255f;
        if(stored.hasOutline())width=stored.width;
    }
    private String state(){return scope()+"|"+mode+"|"+color+"|"+Math.round(opacity*100)+"|"+width;}
    private boolean dirty(){return baseline!=null&&!baseline.equals(state());}
    private boolean hasStored(){AppearanceConfig.Style s=config==null?null:config.scopes.get(scope());return s!=null&&AppearancePeople.read(s.values::get,person)!=null;}

    private Integer fill(){return mode==MODE_OUTLINE?null:(color&0xffffff)|(Math.round(opacity*255)<<24);}
    private Integer outline(){return mode==MODE_FILL?null:color|0xff000000;}
    private void write(AppearanceConfig target){AppearanceConfig.Style style=target.edit(scope());AppearancePeople.write(style,person,fill(),outline(),width);}

    private void refresh(){updatePreview();if(saveButton!=null){boolean on=!busy&&config!=null&&(dirty()||!hasStored());saveButton.setEnabled(on);saveButton.setAlpha(on?1:.45f);}if(list!=null)list.adapter.update(false);}
    private void updatePreview(){
        if(config==null||getContext()==null)return;
        AppearanceConfig sample=config.copy();write(sample);
        AppearanceConfig.Resolved style=sample.resolve(AppearanceConfig.Context.direct(dialog,0));
        boolean dark=Theme.isCurrentThemeDark();
        if(preview==null){preview=new AppearancePreview(getContext(),currentAccount,style,dark,3);preview.setSampleSender(person);previewHost.addView(preview,LayoutHelper.createFrame(-1,-1));preview.setResumed(true);}
        else preview.update(style,dark);
        setTopContent(previewHost,220);
    }

    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        if(!valid())return;
        if(failed){items.add(UItem.asShadow(getString(R.string.TetogramAppearanceStorageError)));return;}
        if(config==null){items.add(UItem.asShadow(getString(R.string.Loading)));return;}
        items.add(UItem.asShadow(formatString(R.string.TetogramPersonInfo,name(person))));
        if(group()){
            items.add(UItem.asHeader(getString(R.string.TetogramStudioScope)));
            items.add(UItem.asCustom(AppearanceStudioViews.segments(getContext(),new CharSequence[]{getString(R.string.TetogramPersonEverywhere),getString(R.string.TetogramPersonThisGroup)},groupOnly?1:0,new Runnable[]{()->{groupOnly=false;refresh();},()->{groupOnly=true;refresh();}}),-2));
        }
        items.add(UItem.asHeader(getString(R.string.TetogramPersonStyle)));
        items.add(UItem.asCustom(padded(AppearanceStudioViews.segments(getContext(),new CharSequence[]{getString(R.string.TetogramPersonFill),getString(R.string.TetogramPersonOutline),getString(R.string.TetogramPersonBoth)},mode,new Runnable[]{()->{mode=MODE_FILL;refresh();},()->{mode=MODE_OUTLINE;refresh();},()->{mode=MODE_BOTH;refresh();}})),-2));
        if(mode==MODE_OUTLINE)items.add(UItem.asShadow(getString(R.string.TetogramPersonOutlineInfo)));
        items.add(UItem.asHeader(getString(R.string.TetogramPersonColor)));
        items.add(UItem.asCustom(swatches(),-2));
        if(mode!=MODE_OUTLINE)items.add(UItem.asCustom(slider(getString(R.string.TetogramPersonOpacity),10,100,Math.round(opacity*100),v->v+" %",v->{opacity=v/100f;live();}),-2));
        if(mode!=MODE_FILL)items.add(UItem.asCustom(slider(getString(R.string.TetogramPersonOutlineWidth),1,8,Math.round(width*2),v->(v%2==0?Integer.toString(v/2):(v/2)+".5")+" dp",v->{width=v/2f;live();}),-2));
        if(hasStored())items.add(UItem.asButton(1,getString(R.string.TetogramPersonRemove)).red());
        items.add(UItem.asShadow(getString(R.string.TetogramPersonPrivacy)));
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){if(item.id==1&&!busy)save(true);}

    private View padded(View child){FrameLayout frame=new FrameLayout(getContext());frame.setPadding(dp(16),dp(6),dp(16),dp(6));frame.addView(child,LayoutHelper.createFrame(-1,-2));return frame;}
    private View swatches(){
        LinearLayout column=new LinearLayout(getContext());column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(16),dp(6),dp(16),dp(10));
        LinearLayout row=null;
        for(int i=0;i<=COLORS.length;i++){
            if(i%7==0){row=new LinearLayout(getContext());row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(40));if(i>0)lp.topMargin=dp(8);column.addView(row,lp);}
            View dot;
            if(i==COLORS.length){TextView plus=new TextView(getContext());plus.setText("+");plus.setTextSize(20);plus.setGravity(Gravity.CENTER);ThemeBind.textColor(plus,Theme.key_windowBackgroundWhiteGrayText);plus.setBackground(Theme.createRoundRectDrawable(dp(20),Theme.getColor(Theme.key_windowBackgroundGray)));plus.setContentDescription(getString(R.string.TetogramStudioCustomColor));plus.setOnClickListener(v->customColor());dot=plus;}
            else{int c=COLORS[i];GradientDrawable shape=new GradientDrawable();shape.setShape(GradientDrawable.OVAL);shape.setColor(c);boolean selected=(color|0xff000000)==c;if(selected)shape.setStroke(dp(3),Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));else if(c==0xffFFFFFF)shape.setStroke(dp(1),Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));dot=new View(getContext());dot.setBackground(shape);dot.setContentDescription(AppearanceColors.hex(c));dot.setSelected(selected);dot.setOnClickListener(v->{color=c;refresh();});}
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(40),1);lp.rightMargin=dp(8);row.addView(dot,lp);
        }
        while(row!=null&&row.getChildCount()<7){row.addView(new View(getContext()),new LinearLayout.LayoutParams(0,dp(40),1));}
        return column;
    }
    private void customColor(){
        EditTextBoldCursor field=new EditTextBoldCursor(getContext());field.setTextSize(18);field.setSingleLine(true);field.setText(AppearanceColors.hex(color|0xff000000));field.setPadding(dp(24),dp(8),dp(24),dp(8));ThemeBind.textColor(field,Theme.key_dialogTextBlack);
        showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramStudioCustomColor)).setView(field).setPositiveButton(getString(R.string.OK),(d,n)->{try{color=AppearanceColors.parse(field.getText().toString())|0xff000000;refresh();}catch(IllegalArgumentException e){showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramAppearanceColorFormat)).setPositiveButton(getString(R.string.OK),null).create());}}).setNegativeButton(getString(R.string.Cancel),null).create());
    }
    private interface Format{String text(int value);}
    /** Live while dragging: updates the value and the preview only, so the list (and this slider) is not rebuilt. */
    private interface Change{void set(int value);}
    private void live(){updatePreview();if(saveButton!=null){boolean on=!busy&&config!=null&&(dirty()||!hasStored());saveButton.setEnabled(on);saveButton.setAlpha(on?1:.45f);}}
    private View slider(CharSequence title,int min,int max,int value,Format format,Change change){
        LinearLayout row=new LinearLayout(getContext());row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(20),dp(4),dp(16),dp(4));
        TextView label=new TextView(getContext());label.setText(title);label.setTextSize(TypedValue.COMPLEX_UNIT_SP,15);ThemeBind.textColor(label,Theme.key_windowBackgroundWhiteBlackText);row.addView(label,new LinearLayout.LayoutParams(dp(110),-2));
        SeekBar bar=new SeekBar(getContext());bar.setMax(max-min);bar.setProgress(Math.max(0,Math.min(max-min,value-min)));row.addView(bar,new LinearLayout.LayoutParams(0,dp(44),1));
        TextView shown=new TextView(getContext());shown.setText(format.text(value));shown.setTextSize(TypedValue.COMPLEX_UNIT_SP,14);shown.setGravity(Gravity.RIGHT);ThemeBind.textColor(shown,Theme.key_windowBackgroundWhiteBlackText);row.addView(shown,new LinearLayout.LayoutParams(dp(60),-2));
        bar.setContentDescription(title);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int p,boolean user){if(!user)return;shown.setText(format.text(min+p));change.set(min+p);}
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
        return row;
    }

    private void save(){save(false);}
    /** Writes only this person's keys in the chosen scope, over the newest document (one retry on a revision conflict). */
    private void save(boolean remove){
        if(busy||config==null||!valid())return;busy=true;refresh();
        write(config,revision,remove,true);
    }
    private AppearanceConfig mutate(AppearanceConfig base,boolean remove){
        AppearanceConfig next=base.copy();
        String target=scope();
        if(remove){AppearanceConfig.Style s=next.scopes.get(target);if(s!=null){AppearancePeople.clear(s,person);if(s.values.isEmpty())next.scopes.remove(target);}return next;}
        // "Everywhere" from a group also drops that group's own override, otherwise it would keep winning there.
        if(group()&&AppearanceConfig.GLOBAL.equals(target)){AppearanceConfig.Style chat=next.scopes.get("chat:"+dialog);if(chat!=null){AppearancePeople.clear(chat,person);if(chat.values.isEmpty())next.scopes.remove("chat:"+dialog);}}
        write(next);
        return next;
    }
    private void write(AppearanceConfig base,long expected,boolean remove,boolean retry){
        AppearanceStore.save(currentAccount,owner,mutate(base,remove),expected,(ok,c)->{
            if(!valid())return;
            if(ok){busy=false;config=c;revision=c.revision;baseline=state();leaving=true;finishFragment();return;}
            if(retry){AppearanceStore.load(currentAccount,(loaded,fresh)->{if(!valid())return;if(loaded)write(fresh,fresh.revision,remove,false);else failSave();});return;}
            failSave();
        });
    }
    private void failSave(){busy=false;refresh();showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramAppearanceConflict)).setPositiveButton(getString(R.string.OK),null).create());}

    @Override public boolean canBeginSlide(){return !dirty()&&!busy&&super.canBeginSlide();}
    @Override public boolean onBackPressed(boolean invoked){if(!leaving&&(dirty()||busy)){if(invoked)askExit();return false;}return super.onBackPressed(invoked);}
    @Override public void finishFragment(){if(!leaving&&(dirty()||busy)){askExit();return;}super.finishFragment();}
    private void askExit(){if(busy)return;showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditorUnsaved)).setMessage(getString(R.string.TetogramEditorUnsavedInfo)).setPositiveButton(getString(R.string.TetogramAppearanceApply),(d,n)->save()).setNeutralButton(getString(R.string.TetogramEditorDiscard),(d,n)->{leaving=true;finishFragment();}).setNegativeButton(getString(R.string.Cancel),null).create());}
}
