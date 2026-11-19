/* Tetogram, 2026. GNU GPL v2 or later. Labs overview: features, permissions, local data. */
package org.telegram.ui.tetogram;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.tetogram.AndroidPermissions;
import org.telegram.messenger.tetogram.AppearanceConfig;
import org.telegram.messenger.tetogram.AppearancePeople;
import org.telegram.messenger.tetogram.AppearancePresets;
import org.telegram.messenger.tetogram.AppearanceStore;
import org.telegram.messenger.tetogram.DeletedHistory;
import org.telegram.messenger.tetogram.EditHistory;
import org.telegram.messenger.tetogram.LabsSettings;
import org.telegram.messenger.tetogram.SectionsStore;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.BasePermissionsActivity;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.DialogsActivity;

public final class LabsActivity extends LabsPage {
    private long[] deletedStats,editStats;
    private long mediaBytes=-1;
    public LabsActivity(int account){super(account);}
    @Override protected CharSequence title(){return getString(R.string.TetogramLabs);}

    @Override public void onResume(){
        super.onResume();
        DeletedHistory.stats(currentAccount,v->{deletedStats=v;update();});
        EditHistory.stats(currentAccount,v->{editStats=v;update();});
        long uid=UserConfig.getInstance(currentAccount).getClientUserId();
        if(uid>0)Utilities.globalQueue.postRunnable(()->{long size=folderSize(new File(AppearanceStore.directory(uid),"media"),0);AndroidUtilities.runOnUIThread(()->{mediaBytes=size;update();});});
        AppearanceStore.load(currentAccount,(ok,c)->update());
    }
    private void update(){if(list!=null)list.adapter.update(false);}
    private static long folderSize(File f,int depth){if(f==null||!f.exists()||depth>3)return 0;if(f.isFile())return f.length();long sum=0;File[] files=f.listFiles();if(files!=null)for(File c:files)sum+=folderSize(c,depth+1);return sum;}

    // ---- state shown on the tiles ----
    private String appearanceState(){
        AppearanceConfig c=fillConfig();AppearanceConfig.Style global=AppearanceConfig.withoutPersonal(c.scopes.get(AppearanceConfig.GLOBAL));
        if(global.values.isEmpty())return getString(R.string.TetogramLabsTelegramTheme);
        for(AppearanceConfig.Preset p:AppearancePresets.builtins())if(AppearanceConfig.sameAppearance(global,p.style))return formatString(R.string.TetogramLabsThemeName,p.name);
        for(AppearanceConfig.Preset p:c.presets.values())if(AppearanceConfig.sameAppearance(global,p.style))return formatString(R.string.TetogramLabsThemeName,p.name);
        return getString(R.string.TetogramLabsOwnTheme);
    }
    private String sectionsState(){try{int n=SectionsStore.load(currentAccount).sections.size();return n==0?getString(R.string.TetogramLabsNone):formatString(R.string.TetogramLabsCount,n);}catch(Exception e){return getString(R.string.TetogramStorageError);}}
    private String peopleState(){AppearanceConfig c=fillConfig();HashSet<Long> people=new HashSet<>();for(AppearanceConfig.Style s:c.scopes.values())people.addAll(AppearancePeople.people(s));return people.isEmpty()?getString(R.string.TetogramLabsNone):formatString(R.string.TetogramLabsCount,people.size());}

    // ---- permissions ----
    private boolean lockScreenCalls(){if(Build.VERSION.SDK_INT<34)return true;NotificationManager nm=(NotificationManager)ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);return nm==null||nm.canUseFullScreenIntent();}
    private boolean background(){PowerManager pm=(PowerManager)ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);return pm==null||pm.isIgnoringBatteryOptimizations(ApplicationLoader.applicationContext.getPackageName());}
    private void open(Intent intent){Activity a=getParentActivity();if(a==null)return;try{a.startActivity(intent);}catch(Exception e){try{a.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+a.getPackageName())));}catch(Exception ignored){}}}
    private Uri pkg(){return Uri.parse("package:"+ApplicationLoader.applicationContext.getPackageName());}

    /** One copy of the appearance document per list rebuild instead of one per tile. */
    private AppearanceConfig filled;
    private AppearanceConfig fillConfig(){if(filled==null)filled=AppearanceStore.snapshot(currentAccount);return filled;}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){
        filled=null;Context ctx=getContext();
        items.add(UItem.asCustomShadow(header(ctx),-2));
        rowActions.clear();
        // Four main tools as tiles, the rest as ordinary settings rows grouped like Telegram's own settings.
        ArrayList<View> tiles=new ArrayList<>();
        tiles.add(tile(ctx,R.drawable.settings_chat,0xff6A4FC4,getString(R.string.TetogramAppearance),appearanceState(),false,()->presentFragment(new AppearanceActivity(currentAccount))));
        tiles.add(tile(ctx,R.drawable.msg_folders,0xff2F7DD3,getString(R.string.TetogramSections),sectionsState(),false,()->presentFragment(new SectionsActivity(currentAccount))));
        int playlists=org.telegram.messenger.tetogram.PlaylistsStore.load(currentAccount).playlists.size();
        tiles.add(tile(ctx,R.drawable.msg_filled_data_music,0xffE5533D,getString(R.string.TetogramMusic),playlists==0?getString(R.string.TetogramLabsNone):formatString(R.string.TetogramLabsCount,playlists),false,()->presentFragment(new MusicActivity(currentAccount))));
        int translated=org.telegram.messenger.tetogram.ChatTranslation.chats(currentAccount).size();
        tiles.add(tile(ctx,R.drawable.msg_translate,0xff3C8DBC,getString(R.string.TetogramTranslate),translated==0?getString(R.string.TetogramLabsNone):formatString(R.string.TetogramLabsCount,translated),translated>0,()->presentFragment(new TranslateSettingsActivity(currentAccount))));
        items.add(UItem.asCustomShadow(grid(ctx,tiles),-2));

        items.add(UItem.asHeader(getString(R.string.TetogramLabsGroupChats)));
        int tagCount=org.telegram.messenger.tetogram.ChatTagsStore.load(currentAccount).tags.size();
        row(items,R.drawable.menu_tag_filter,getString(R.string.TetogramTags),tagCount==0?getString(R.string.TetogramLabsNone):formatString(R.string.TetogramLabsCount,tagCount),()->presentFragment(new TagsActivity(currentAccount)));
        int bookmarks=org.telegram.messenger.tetogram.BookmarksStore.load(currentAccount).items.size();
        row(items,R.drawable.msg_fave,getString(R.string.TetogramBookmarks),bookmarks==0?getString(R.string.TetogramLabsNone):formatString(R.string.TetogramLabsCount,bookmarks),()->presentFragment(new BookmarksActivity(currentAccount,0)));
        boolean ignore=org.telegram.messenger.tetogram.Ignore.enabled(currentAccount);
        row(items,R.drawable.msg_block,getString(R.string.TetogramIgnoreTile),ignore?formatString(R.string.TetogramIgnoreTileOn,org.telegram.messenger.tetogram.Ignore.blocked(currentAccount).size()):getString(R.string.TetogramLabsOff),()->presentFragment(new IgnoreActivity(currentAccount)));
        String voiceLang=org.telegram.messenger.tetogram.VoiceTranscription.language();boolean voiceReady=org.telegram.messenger.tetogram.VoiceTranscription.installed(voiceLang);
        if(VoiceUi.ENABLED)row(items,R.drawable.msg_photo_text_regular,getString(R.string.TetogramVoice),voiceReady?org.telegram.messenger.tetogram.ChatTranslation.languageName(voiceLang):getString(R.string.TetogramLabsOff),()->presentFragment(new VoiceSettingsActivity(currentAccount)));

        items.add(UItem.asHeader(getString(R.string.TetogramLabsGroupPeople)));
        row(items,R.drawable.menu_profile_colors,getString(R.string.TetogramPersonSection),peopleState(),()->presentFragment(AppearanceActivity.people(currentAccount)));
        boolean presence=org.telegram.messenger.tetogram.TetoPresence.connected(currentAccount)&&org.telegram.messenger.tetogram.TetoPresence.visible(currentAccount);
        row(items,R.drawable.msg_contacts,getString(R.string.TetogramPresence),getString(presence?R.string.TetogramPresenceOn:R.string.TetogramLabsOff),()->presentFragment(new PresenceActivity(currentAccount)));

        items.add(UItem.asHeader(getString(R.string.TetogramLabsGroupHistory)));
        boolean deleted=LabsSettings.enabled(currentAccount),edits=LabsSettings.edits(currentAccount);
        row(items,R.drawable.msg_delete,getString(R.string.TetogramLabsDeletedTitle),getString(deleted?R.string.TetogramLabsSaving:R.string.TetogramLabsOff),()->presentFragment(new LabsHistoryActivity(currentAccount,false)));
        row(items,R.drawable.msg_edit,getString(R.string.TetogramLabsEditsTitle),getString(edits?R.string.TetogramLabsSaving:R.string.TetogramLabsOff),()->presentFragment(new LabsHistoryActivity(currentAccount,true)));
        row(items,R.drawable.msg_stats,getString(R.string.TetogramStats),getString(R.string.TetogramLabsPickChat),this::pickStatsChat);

        items.add(UItem.asHeader(getString(R.string.TetogramLabsSystem)));
        LinearLayout perms=card(ctx);
        Activity activity=getParentActivity();
        status(perms,getString(R.string.TetogramLabsLockCalls),lockScreenCalls(),getString(R.string.TetogramLabsLockCallsInfo),getString(R.string.TetogramLabsEnable),()->{Intent i=Build.VERSION.SDK_INT>=34?new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,pkg()):new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,pkg());open(i);});
        boolean notif=activity!=null&&AndroidPermissions.notifications(activity);
        status(perms,getString(R.string.TetogramLabsNotifications),notif,getString(R.string.TetogramLabsNotificationsInfo),getString(R.string.TetogramLabsEnable),()->{Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,ApplicationLoader.applicationContext.getPackageName());open(i);});
        boolean gallery=activity!=null&&AndroidPermissions.gallery(activity);
        status(perms,getString(R.string.TetogramLabsGallery),gallery,getString(R.string.TetogramLabsGalleryInfo),getString(R.string.TetogramLabsAllow),()->{if(getParentActivity()!=null)AndroidPermissions.requestGallery(getParentActivity(),BasePermissionsActivity.REQUEST_CODE_EXTERNAL_STORAGE,true);});
        status(perms,getString(R.string.TetogramLabsBackground),background(),getString(R.string.TetogramLabsBackgroundInfo),getString(R.string.TetogramLabsOpen),()->open(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)));
        items.add(UItem.asCustom(perms,-2));

        items.add(UItem.asHeader(getString(R.string.TetogramLabsData)));
        LinearLayout data=card(ctx);
        dataRow(data,getString(R.string.TetogramLabsDeletedTitle),deletedStats==null?"…":formatString(R.string.TetogramLabsSavedValue,AndroidUtilities.formatWholeNumber((int)Math.min(Integer.MAX_VALUE,deletedStats[0]),0),AndroidUtilities.formatFileSize(deletedStats[1])));
        dataRow(data,getString(R.string.TetogramLabsEditsTitle),editStats==null?"…":formatString(R.string.TetogramLabsSavedValue,AndroidUtilities.formatWholeNumber((int)Math.min(Integer.MAX_VALUE,editStats[0]),0),AndroidUtilities.formatFileSize(editStats[1])));
        dataRow(data,getString(R.string.TetogramLabsAppearanceMedia),mediaBytes<0?"…":AndroidUtilities.formatFileSize(mediaBytes));
        items.add(UItem.asCustom(data,-2));
        items.add(UItem.asShadow(getString(R.string.TetogramLabsDataInfo)));

        items.add(UItem.asHeader(getString(R.string.TetogramInformation)));
        items.add(UItem.asButton(8,getString(R.string.TetogramLabsWhatsNew),BuildVars.BUILD_VERSION_STRING));
        items.add(UItem.asButton(10,getString(R.string.TetogramLicenses),"GPL v2+"));
        items.add(UItem.asShadow(getString(R.string.TetogramIntro)));
    }
    private final ArrayList<Runnable> rowActions=new ArrayList<>();
    private static final int ROW=1000;
    private void row(ArrayList<UItem> items,int icon,CharSequence title,CharSequence value,Runnable action){rowActions.add(action);items.add(UItem.asButton(ROW+rowActions.size()-1,icon,title,value));}
    @Override protected void click(UItem item,View view,int position,float x,float y){
        if(item.id>=ROW&&item.id-ROW<rowActions.size()){rowActions.get(item.id-ROW).run();return;}
        if(item.id==8)showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramLabsWhatsNew)).setMessage(getString(R.string.TetogramLabsWhatsNewText)).setPositiveButton(getString(R.string.OK),null).create());
        else if(item.id==10)showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramInformation)).setMessage(getString(R.string.TetogramLicenseInfo)).setPositiveButton(getString(R.string.OK),null).create());
    }
    private void pickStatsChat(){
        Bundle args=new Bundle();args.putBoolean("onlySelect",true);args.putInt("dialogsType",0);
        DialogsActivity picker=new DialogsActivity(args);picker.setCurrentAccount(currentAccount);
        picker.setDelegate((fragment,dids,message,param,notify,schedule,repeat,topics)->{if(dids.isEmpty()||DialogObject.isEncryptedDialog(dids.get(0).dialogId))return false;fragment.finishFragment();presentFragment(new ChatStatsActivity(currentAccount,dids.get(0).dialogId));return true;});
        presentFragment(picker);
    }

    // ---- views ----
    @SuppressWarnings("deprecation")
    private static int versionCode(){try{return ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(),0).versionCode;}catch(Exception e){return 0;}}
    private int text(){return Theme.getColor(Theme.key_windowBackgroundWhiteBlackText);}
    private int gray(){return Theme.getColor(Theme.key_windowBackgroundWhiteGrayText);}
    private View header(Context ctx){
        LinearLayout card=new LinearLayout(ctx);card.setOrientation(LinearLayout.HORIZONTAL);card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(18),dp(18),dp(18),dp(18));
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff42307A,0xff6A4FC4,0xff8E6CE8});bg.setCornerRadius(dp(22));card.setBackground(bg);
        TextView mark=new TextView(ctx);mark.setText("T");mark.setTextSize(TypedValue.COMPLEX_UNIT_SP,28);mark.setTypeface(AndroidUtilities.bold());mark.setTextColor(0xff42307A);mark.setGravity(Gravity.CENTER);GradientDrawable m=new GradientDrawable();m.setCornerRadius(dp(16));m.setColor(Color.WHITE);mark.setBackground(m);card.addView(mark,new LinearLayout.LayoutParams(dp(56),dp(56)));
        LinearLayout texts=new LinearLayout(ctx);texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(label(ctx,"Tetogram "+BuildVars.BUILD_VERSION_STRING,17,true,Color.WHITE));
        texts.addView(label(ctx,formatString(R.string.TetogramLabsBuild,versionCode(),"13.2.2"),12,false,0xffE6DEFF));
        texts.addView(label(ctx,getString(R.string.TetogramLabsLocalOnly),12,false,0xffE6DEFF));
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=dp(14);card.addView(texts,tp);
        FrameLayout frame=new FrameLayout(ctx);frame.setPadding(dp(16),dp(8),dp(16),dp(4));frame.addView(card,LayoutHelper.createFrame(-1,-2));
        return frame;
    }
    private TextView label(Context ctx,CharSequence s,int sp,boolean bold,int color){TextView t=new TextView(ctx);t.setText(s);t.setTextSize(TypedValue.COMPLEX_UNIT_SP,sp);if(bold)t.setTypeface(AndroidUtilities.bold());t.setTextColor(color);return t;}
    private View tile(Context ctx,int icon,int color,CharSequence title,CharSequence state,boolean on,Runnable action){
        LinearLayout tile=new LinearLayout(ctx);tile.setOrientation(LinearLayout.VERTICAL);tile.setPadding(dp(14),dp(14),dp(14),dp(14));
        GradientDrawable bg=new GradientDrawable();bg.setCornerRadius(dp(18));bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));tile.setBackground(bg);
        tile.setForeground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector),2));
        ImageView image=new ImageView(ctx);image.setImageResource(icon);image.setColorFilter(Color.WHITE);image.setScaleType(ImageView.ScaleType.CENTER);GradientDrawable ib=new GradientDrawable();ib.setCornerRadius(dp(11));ib.setColor(color);image.setBackground(ib);tile.addView(image,new LinearLayout.LayoutParams(dp(36),dp(36)));
        TextView name=label(ctx,title,15,true,text());name.setSingleLine(true);name.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(-1,-2);np.topMargin=dp(10);tile.addView(name,np);
        TextView value=label(ctx,state,12,false,on?0xff2E9E62:gray());value.setSingleLine(true);value.setEllipsize(android.text.TextUtils.TruncateAt.END);tile.addView(value);
        tile.setContentDescription(title+", "+state);tile.setOnClickListener(v->action.run());
        return tile;
    }
    private View grid(Context ctx,ArrayList<View> tiles){
        LinearLayout column=new LinearLayout(ctx);column.setOrientation(LinearLayout.VERTICAL);column.setPadding(dp(16),dp(4),dp(16),dp(8));
        for(int i=0;i<tiles.size();i+=2){LinearLayout row=new LinearLayout(ctx);row.setOrientation(LinearLayout.HORIZONTAL);LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);if(i>0)rp.topMargin=dp(10);column.addView(row,rp);
            for(int j=i;j<Math.min(i+2,tiles.size());j++){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);if(j>i)lp.leftMargin=dp(10);row.addView(tiles.get(j),lp);}
            if(i+1>=tiles.size()){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,1,1);lp.leftMargin=dp(10);row.addView(new View(ctx),lp);}}
        return column;
    }
    private LinearLayout card(Context ctx){LinearLayout card=new LinearLayout(ctx);card.setOrientation(LinearLayout.VERTICAL);return card;}
    private void status(LinearLayout card,CharSequence title,boolean ok,CharSequence problem,CharSequence actionLabel,Runnable action){
        Context ctx=card.getContext();
        LinearLayout row=new LinearLayout(ctx);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(18),dp(12),dp(16),dp(12));row.setMinimumHeight(dp(56));
        TextView mark=label(ctx,ok?"✓":"!",15,true,ok?0xff2E9E62:0xffD9822B);mark.setGravity(Gravity.CENTER);GradientDrawable mb=new GradientDrawable();mb.setShape(GradientDrawable.OVAL);mb.setColor(ok?0x332E9E62:0x33D9822B);mark.setBackground(mb);row.addView(mark,new LinearLayout.LayoutParams(dp(28),dp(28)));
        LinearLayout texts=new LinearLayout(ctx);texts.setOrientation(LinearLayout.VERTICAL);texts.addView(label(ctx,title,15,false,text()));
        if(!ok){TextView info=label(ctx,problem,12,false,gray());texts.addView(info);}
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=dp(14);row.addView(texts,tp);
        if(ok){row.addView(label(ctx,getString(R.string.TetogramLabsOk),13,false,0xff2E9E62));}
        else{TextView button=label(ctx,actionLabel,13,true,Color.WHITE);button.setGravity(Gravity.CENTER);button.setPadding(dp(14),0,dp(14),0);button.setBackground(Theme.createRoundRectDrawable(dp(17),Theme.getColor(Theme.key_featuredStickers_addButton)));button.setOnClickListener(v->action.run());row.addView(button,new LinearLayout.LayoutParams(-2,dp(34)));}
        row.setContentDescription(title+", "+(ok?getString(R.string.TetogramLabsOk):problem));
        card.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }
    private void dataRow(LinearLayout card,CharSequence title,CharSequence value){
        Context ctx=card.getContext();LinearLayout row=new LinearLayout(ctx);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(20),dp(12),dp(20),dp(12));
        row.addView(label(ctx,title,15,false,text()),new LinearLayout.LayoutParams(0,-2,1));row.addView(label(ctx,value,14,false,gray()));
        card.addView(row,new LinearLayout.LayoutParams(-1,-2));
    }
}
