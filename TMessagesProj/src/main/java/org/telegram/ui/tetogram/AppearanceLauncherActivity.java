/* Tetogram, 2026. GPL v2 or later. Device-wide native aliases, separate from account shortcuts/themes. */
package org.telegram.ui.tetogram;
import static org.telegram.messenger.LocaleController.getString;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.*;
import org.telegram.ui.LauncherIconController;
import org.telegram.ui.LauncherIconController.LauncherIcon;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Components.*;
public final class AppearanceLauncherActivity extends LabsPage implements NotificationCenter.NotificationCenterDelegate {
    private LauncherIconSettings draft,baseline;
    private LauncherIcon initialIcon,classic;
    private boolean touched,saving,leaving,destroyed;
    private TextView apply;
    private final HashMap<Integer,Bitmap> thumbnails=new HashMap<>();
    private final HashSet<Integer> pending=new HashSet<>();
    public AppearanceLauncherActivity(int account){super(account);reload();}
    private void reload(){baseline=LauncherIconController.settings();draft=baseline.copy();initialIcon=LauncherIconController.currentIcon();classic=draft.seasonal?null:initialIcon;touched=false;}
    @Override protected CharSequence title(){return getString(R.string.TetogramAppearanceLauncher);}
    @Override public boolean onFragmentCreate(){if(!super.onFragmentCreate())return false;NotificationCenter.getGlobalInstance().addObserver(this,NotificationCenter.tetogramLauncherIconChanged);return true;}
    @Override public void onFragmentDestroy(){destroyed=true;NotificationCenter.getGlobalInstance().removeObserver(this,NotificationCenter.tetogramLauncherIconChanged);thumbnails.clear();pending.clear();super.onFragmentDestroy();}
    @Override public void onResume(){super.onResume();if(!dirty())reload();refresh();}
    @Override public void didReceivedNotification(int id,int account,Object...args){if(id==NotificationCenter.tetogramLauncherIconChanged&&!destroyed&&!saving){if(!dirty())reload();refresh();}}
    private LauncherIcon target(){if(!touched)return initialIcon;return draft.seasonal?LauncherIconController.find(AppearanceIconPolicy.alias(draft.brand,draft.choice(),Calendar.getInstance().get(Calendar.MONTH)+1)):classic==null?LauncherIcon.DEFAULT:classic;}
    private boolean dirty(){return touched&&(!draft.sameChoice(baseline)||baseline.corrupt&&!draft.corrupt||target()!=initialIcon);}
    private boolean canApply(){return !destroyed&&!leaving&&!saving&&dirty()&&LauncherIconController.hasArtwork(target());}
    private void refresh(){if(apply!=null){apply.setEnabled(canApply());apply.setAlpha(canApply()?1:.45f);}if(list!=null)list.adapter.update(false);}
    private TextView text(CharSequence value,int size){TextView v=new TextView(getContext());v.setText(value);v.setTextSize(size);v.setGravity(Gravity.CENTER);v.setPadding(AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8),AndroidUtilities.dp(8));ThemeBind.textColor(v,Theme.key_windowBackgroundWhiteBlackText);return v;}
    private TextView button(CharSequence value,boolean selected,Runnable action){TextView v=text(value,14);v.setMinHeight(AndroidUtilities.dp(48));v.setSelected(selected);if(selected)v.setTypeface(AndroidUtilities.bold());ThemeBind.textColor(v,selected?Theme.key_windowBackgroundWhiteBlueText:Theme.key_windowBackgroundWhiteBlackText);if(selected){android.graphics.drawable.Drawable bg=Theme.createRoundRectDrawable(AndroidUtilities.dp(12),Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));bg.setAlpha(36);v.setBackground(bg);ThemeBind.drawableColorFilter(v,bg,Theme.key_windowBackgroundWhiteBlueText,android.graphics.PorterDuff.Mode.SRC_IN);}else v.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector),2));v.setEnabled(!saving);v.setOnClickListener(w->{if(!saving&&!destroyed)action.run();});return v;}
    @Override public View createView(Context context){FrameLayout root=(FrameLayout)super.createView(context);LinearLayout dock=new LinearLayout(context);ThemeBind.backgroundColor(dock,Theme.key_windowBackgroundWhite);TextView cancel=button(getString(R.string.Cancel),false,this::finishFragment);apply=button(getString(R.string.TetogramAppearanceApply),true,this::save);dock.addView(cancel,new LinearLayout.LayoutParams(0,-2,1));dock.addView(apply,new LinearLayout.LayoutParams(0,-2,1));setBottomActions(dock);refresh();return root;}
    @Override public boolean canBeginSlide(){return !dirty()&&!saving&&super.canBeginSlide();}
    @Override public boolean onBackPressed(boolean invoked){if(!leaving&&(dirty()||saving)){if(invoked)requestExit();return false;}return super.onBackPressed(invoked);}
    @Override public void finishFragment(){if(!leaving&&(dirty()||saving)){requestExit();return;}super.finishFragment();}
    private void requestExit(){if(saving)return;showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditorUnsaved)).setMessage(getString(R.string.TetogramEditorUnsavedInfo)).setPositiveButton(getString(R.string.TetogramAppearanceApply),(d,n)->save()).setNeutralButton(getString(R.string.TetogramEditorDiscard),(d,n)->{leaving=true;finishFragment();}).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void message(CharSequence value){if(!destroyed&&getContext()!=null)showDialog(new AlertDialog.Builder(getContext()).setMessage(value).setPositiveButton(getString(R.string.OK),null).create());}
    private void save(){if(!canApply()){if(dirty())message(getString(R.string.TetogramLauncherMissingArtwork)+"\n"+missingPath());return;}saving=true;refresh();try{LauncherIconController.applySelection(draft,classic,baseline.revision);leaving=true;finishFragment();}catch(RuntimeException e){message(getString(LauncherIconController.settings().revision!=baseline.revision?R.string.TetogramLauncherConflict:R.string.TetogramLauncherApplyError));}finally{saving=false;refresh();}}
    private void chooseBrand(String brand){draft.brand=brand;draft.corrupt=false;draft.seasonal=true;classic=null;touched=true;refresh();}
    private void chooseSeason(AppearanceIconPolicy.Season season){draft.select(season);draft.corrupt=false;classic=null;touched=true;refresh();}
    private int seasonLabel(AppearanceIconPolicy.Season s){switch(s){case SPRING:return R.string.TetogramEditorSpring;case SUMMER:return R.string.TetogramEditorSummer;case AUTUMN:return R.string.TetogramEditorAutumn;case WINTER:return R.string.TetogramEditorWinter;default:return R.string.TetogramLauncherAutoShort;}}
    private String missingPath(){AppearanceIconPolicy.Season s=draft.automatic?AppearanceIconPolicy.season(Calendar.getInstance().get(Calendar.MONTH)+1):draft.manual;return "assets/launcher/seasonal/"+draft.brand.toLowerCase(java.util.Locale.ROOT)+"_"+s.name().toLowerCase(java.util.Locale.ROOT)+".png";}
    private void load(int resource){if(resource==0||resource==R.drawable.tetogram_mark||thumbnails.containsKey(resource)||!pending.add(resource))return;Utilities.globalQueue.postRunnable(()->{Bitmap bitmap=null;try{bitmap=ImageLoader.loadBitmap(null,android.net.Uri.parse("android.resource://"+ApplicationLoader.applicationContext.getPackageName()+"/"+resource),512,512,true);}catch(Exception|OutOfMemoryError ignored){}final Bitmap ready=bitmap;AndroidUtilities.runOnUIThread(()->{if(destroyed){if(ready!=null)ready.recycle();return;}pending.remove(resource);thumbnails.put(resource,ready);refresh();});});}
    private float nativeScale(LauncherIcon icon){return LauncherIconController.isSeasonal(icon)&&AppearanceIconPolicy.canonical(icon.key).equals(icon.key)?AppearanceIconPolicy.SEASONAL_CONTENT_SCALE:AppearanceIconPolicy.SAFE_CONTENT_SCALE;}
    private View icon(LauncherIcon icon,int size,int shape){
        if(!LauncherIconController.hasArtwork(icon))return text(getString(R.string.TetogramLauncherNoArtwork),12);
        load(icon.foreground);Bitmap bitmap=thumbnails.get(icon.foreground);
        if(icon.foreground!=R.drawable.tetogram_mark&&bitmap==null)return text(getString(thumbnails.containsKey(icon.foreground)?R.string.TetogramLauncherPreviewError:R.string.Loading),12);
        return new AppearanceIconPreview(getContext(),bitmap,null,nativeScale(icon),shape,icon.background);
    }
    private void row(ArrayList<UItem> items,CharSequence name,CharSequence value,Runnable action){items.add(UItem.asCustom(new AppearanceSettingRow(getContext(),name,value,!saving,action),-2));}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){if(destroyed)return;
        items.add(UItem.asShadow(getString(R.string.TetogramEditorIconDeviceScope)));
        items.add(UItem.asHeader(getString(R.string.TetogramLauncherBrand)));
        LinearLayout brands=new LinearLayout(getContext());for(String brand:new String[]{"Tetogram","Mikugram"})brands.addView(button(getString(brand.equals("Tetogram")?R.string.TetogramLauncherTeto:R.string.TetogramLauncherMiku),draft.brand.equals(brand),()->chooseBrand(brand)),new LinearLayout.LayoutParams(0,-2,1));items.add(UItem.asCustom(brands,-2));
        items.add(UItem.asHeader(getString(R.string.TetogramEditorSeason)));LinearLayout seasons=new LinearLayout(getContext());for(AppearanceIconPolicy.Season season:AppearanceIconPolicy.Season.values())seasons.addView(button(getString(seasonLabel(season)),draft.seasonal&&draft.choice()==season,()->chooseSeason(season)),new LinearLayout.LayoutParams(0,-2,1));items.add(UItem.asCustom(seasons,-2));
        LauncherIcon selected=target();LinearLayout preview=new LinearLayout(getContext());preview.setOrientation(LinearLayout.VERTICAL);preview.setGravity(Gravity.CENTER);preview.addView(icon(selected,160,2),new LinearLayout.LayoutParams(AndroidUtilities.dp(160),AndroidUtilities.dp(160)));String label=selected!=null&&AppearanceIconPolicy.canonical(selected.key).startsWith("Mikugram")?"Mikugram":"Tetogram";preview.addView(text(label,22));if(draft.seasonal)preview.addView(text(getString(seasonLabel(draft.choice()))+(draft.automatic?" · "+getString(seasonLabel(AppearanceIconPolicy.season(Calendar.getInstance().get(Calendar.MONTH)+1))):""),14));else preview.addView(text(getString(R.string.TetogramEditorClassicIcons),14));items.add(UItem.asCustom(preview,-2));
        if(!LauncherIconController.hasArtwork(selected)||draft.seasonal&&!LauncherIconController.hasArtwork(LauncherIconController.find(AppearanceIconPolicy.alias(draft.brand,draft.choice(),Calendar.getInstance().get(Calendar.MONTH)+1))))items.add(UItem.asShadow(getString(R.string.TetogramLauncherMissingArtwork)+"\n"+missingPath()));
        items.add(UItem.asHeader(getString(R.string.TetogramLauncherGallery)));HorizontalScrollView gallery=new HorizontalScrollView(getContext());LinearLayout cards=new LinearLayout(getContext());for(AppearanceIconPolicy.Season season:new AppearanceIconPolicy.Season[]{AppearanceIconPolicy.Season.SPRING,AppearanceIconPolicy.Season.SUMMER,AppearanceIconPolicy.Season.AUTUMN,AppearanceIconPolicy.Season.WINTER}){LauncherIcon entry=LauncherIconController.find(AppearanceIconPolicy.alias(draft.brand,season,1));LinearLayout card=new LinearLayout(getContext());card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER);card.addView(icon(entry,84,2),new LinearLayout.LayoutParams(AndroidUtilities.dp(84),AndroidUtilities.dp(84)));card.addView(button(getString(seasonLabel(season)),draft.seasonal&&(draft.automatic?AppearanceIconPolicy.season(Calendar.getInstance().get(Calendar.MONTH)+1):draft.manual)==season,()->chooseSeason(season)));card.setOnClickListener(v->{if(!saving)chooseSeason(season);});cards.addView(card,new LinearLayout.LayoutParams(AndroidUtilities.dp(108),-2));}gallery.addView(cards);items.add(UItem.asCustom(gallery,-2));
        if(LauncherIconController.hasArtwork(selected)){LinearLayout masks=new LinearLayout(getContext());for(int shape=0;shape<3;shape++){LinearLayout cell=new LinearLayout(getContext());cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);cell.addView(icon(selected,64,shape),new LinearLayout.LayoutParams(AndroidUtilities.dp(64),AndroidUtilities.dp(64)));cell.addView(text(getString(shape==0?R.string.TetogramEditorRound:shape==1?R.string.TetogramEditorSquare:R.string.TetogramEditorSquircle),12));masks.addView(cell,new LinearLayout.LayoutParams(0,-2,1));}items.add(UItem.asCustom(masks,-2));}
        row(items,getString(R.string.TetogramLauncherRestore),null,()->{draft=new LauncherIconSettings();draft.revision=baseline.revision;classic=LauncherIcon.DEFAULT;touched=true;refresh();});
        row(items,getString(R.string.TetogramEditorClassicIcons),getString(R.string.TetogramLauncherLegacyInfo),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramEditorClassicIcons)).setItems(new CharSequence[]{"Tetogram Classic","Tetogram T",getString(R.string.TetogramEditorAutumn)},(d,n)->{draft.seasonal=false;draft.corrupt=false;draft.brand="Tetogram";classic=n==0?LauncherIcon.DEFAULT:n==1?LauncherIcon.AQUA:LauncherIcon.VINTAGE;touched=true;refresh();}).create()));
        row(items,getString(R.string.TetogramLauncherCustomShortcut),getString(R.string.TetogramAppearanceLauncherLimits),()->presentFragment(new AppearanceShortcutActivity(currentAccount)));
        items.add(UItem.asShadow(getString(R.string.TetogramLauncherCalendarInfo)));
    }
    @Override protected void click(UItem item,View view,int position,float x,float y){}
}
