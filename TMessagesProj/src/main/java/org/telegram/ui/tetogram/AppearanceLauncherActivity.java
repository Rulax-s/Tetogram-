/* Tetogram, 2026. GNU GPL v2 or later. Explicit pinned shortcuts, native avatar crop, no server upload. */
package org.telegram.ui.tetogram;
import static org.telegram.messenger.LocaleController.getString;
import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import java.io.File;
import java.util.*;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.*;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.ActionBar.*;
import org.telegram.ui.Cells.AppIconsSelectorCell;
import org.telegram.ui.Components.*;
public final class AppearanceLauncherActivity extends LabsPage {
    private static final int PICK=61044;
    private static final String SHORTCUT="tetogram.personalized";
    private final long owner;
    private AppearanceConfig config;
    private AppearanceConfig.Style draft;
    private long revision;
    private boolean destroyed,busy,failed;
    private int generation;
    private ImageUpdater cropper;
    private AppearanceMedia.Job job;
    private final ArrayList<File> pending=new ArrayList<>();
    private final ArrayList<Runnable> actions=new ArrayList<>();
    public AppearanceLauncherActivity(int account){super(account);owner=getUserConfig().getClientUserId();}
    private boolean valid(){return !destroyed&&owner>0&&owner==getUserConfig().getClientUserId();}
    @Override protected CharSequence title(){return getString(R.string.TetogramAppearanceLauncher);}
    @Override public void onResume(){super.onResume();if(cropper!=null)cropper.onResume();AppearanceStore.load(currentAccount,(ok,c)->{if(!valid())return;failed=!ok;if(draft==null){config=c;revision=c.revision;draft=c.edit("global").copy();}refresh();});}
    @Override public void onPause(){if(cropper!=null)cropper.onPause();super.onPause();}
    @Override public void onFragmentDestroy(){destroyed=true;generation++;if(job!=null)job.cancel();if(cropper!=null){cropper.setDelegate(null);cropper.parentFragment=null;cropper.clear();}for(File f:pending)AppearanceMedia.discard(f);pending.clear();super.onFragmentDestroy();}
    private void refresh(){if(list!=null)list.adapter.update(false);}
    private void row(ArrayList<UItem> items,CharSequence label,Runnable run){actions.add(run);items.add(UItem.asButton(actions.size(),label).setEnabled(!busy));}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){actions.clear();if(!valid())return;if(failed){items.add(UItem.asShadow(getString(R.string.TetogramAppearanceStorageError)));return;}if(draft==null){items.add(UItem.asShadow(getString(R.string.Loading)));return;}
        items.add(UItem.asShadow(getString(R.string.TetogramAppearanceLauncherLimits)));
        items.add(UItem.asHeader(getString(R.string.AppIcon)));items.add(UItem.asCustom(new AppIconsSelectorCell(getContext(),this,currentAccount),120));
        LinearLayout preview=new LinearLayout(getContext());preview.setOrientation(LinearLayout.VERTICAL);preview.setGravity(Gravity.CENTER);preview.setPadding(0,AndroidUtilities.dp(8),0,AndroidUtilities.dp(8));
        FrameLayout icon=new FrameLayout(getContext());ThemeBind.backgroundColor(icon,Theme.key_chats_actionBackground);icon.setOutlineProvider(new ViewOutlineProvider(){public void getOutline(View v,Outline o){o.setRoundRect(0,0,v.getWidth(),v.getHeight(),AndroidUtilities.dp(16));}});icon.setClipToOutline(true);preview.addView(icon,new LinearLayout.LayoutParams(AndroidUtilities.dp(64),AndroidUtilities.dp(64)));
        File file=AppearanceMedia.file(owner,draft.values.get("launcher.icon.file"));String emoji=draft.values.get("launcher.emoji");if(file!=null){BackupImageView image=new BackupImageView(getContext());image.setImage(file.getAbsolutePath(),"64_64",getContext().getResources().getDrawable(R.drawable.tetogram_mark));icon.addView(image,LayoutHelper.createFrame(-1,-1));}else if(emoji!=null&&!emoji.isEmpty()){TextView text=new TextView(getContext());text.setTextSize(36);text.setGravity(Gravity.CENTER);text.setText(Emoji.replaceEmoji(emoji,text.getPaint().getFontMetricsInt(),false));icon.addView(text,LayoutHelper.createFrame(-1,-1));}else{ImageView image=new ImageView(getContext());image.setImageResource(R.drawable.tetogram_mark);icon.addView(image,LayoutHelper.createFrame(-1,-1));}
        TextView label=new TextView(getContext());label.setGravity(Gravity.CENTER);label.setText(draft.values.getOrDefault("launcher.label","Tetogram"));ThemeBind.textColor(label,Theme.key_windowBackgroundWhiteBlackText);preview.addView(label,new LinearLayout.LayoutParams(-1,AndroidUtilities.dp(30)));items.add(UItem.asCustom(preview,112));
        row(items,getString(R.string.TetogramAppearanceLauncherName),()->input(getString(R.string.TetogramAppearanceLauncherName),draft.values.getOrDefault("launcher.label","Tetogram"),text->{draft.set("launcher.label",text);refresh();}));
        row(items,getString(R.string.TetogramAppearanceLauncherNames),()->showDialog(new AlertDialog.Builder(getContext()).setTitle(getString(R.string.TetogramAppearanceLauncherNames)).setItems(new CharSequence[]{"Tetogram","Tetogram Labs"},(d,n)->{draft.set("launcher.label",n==0?"Tetogram":"Tetogram Labs");refresh();}).create()));
        row(items,getString(R.string.TetogramAppearanceChooseImage),this::pick);
        row(items,getString(R.string.TetogramSectionIcon),()->input(getString(R.string.TetogramSectionIcon),draft.values.getOrDefault("launcher.emoji","🎨"),text->{draft.set("launcher.emoji",text);draft.values.remove("launcher.icon.file");refresh();}));
        row(items,getString(R.string.TetogramAppearanceReset),()->{draft.values.remove("launcher.icon.file");draft.values.remove("launcher.emoji");draft.values.remove("launcher.label");refresh();});
        row(items,getString(R.string.Save),this::save);
        row(items,getString(R.string.TetogramAppearancePinShortcut),()->showDialog(new AlertDialog.Builder(getContext()).setMessage(getString(R.string.TetogramAppearanceLauncherLimits)).setPositiveButton(getString(R.string.OK),(d,n)->save(this::pin)).setNegativeButton(getString(R.string.Cancel),null).create()));
        row(items,getString(R.string.TetogramAppearanceRemoveShortcut),()->{try{ShortcutManagerCompat.disableShortcuts(getContext(),Collections.singletonList(SHORTCUT),getString(R.string.TetogramAppearanceShortcutRemoved));}catch(Exception ignored){}message(getString(R.string.TetogramAppearanceShortcutRemoved));});
        if(busy)items.add(UItem.asShadow(getString(R.string.TetogramAppearancePreparingMedia)));
    }
    private void save(){save(()->{});}
    private void save(Runnable done){if(busy||!valid())return;busy=true;config.scopes.put("global",draft.copy());AppearanceStore.save(currentAccount,owner,config,revision,(ok,c)->{if(!valid())return;busy=false;if(ok){config=c;revision=c.revision;done.run();}else message(getString(R.string.TetogramAppearanceConflict));refresh();});refresh();}
    private void input(CharSequence title,String initial,java.util.function.Consumer<String> result){EditTextBoldCursor edit=new EditTextBoldCursor(getContext());edit.setSingleLine(true);edit.setText(initial);edit.setTextSize(18);edit.setPadding(AndroidUtilities.dp(24),AndroidUtilities.dp(8),AndroidUtilities.dp(24),AndroidUtilities.dp(8));ThemeBind.textColor(edit,Theme.key_dialogTextBlack);showDialog(new AlertDialog.Builder(getContext()).setTitle(title).setView(edit).setPositiveButton(getString(R.string.OK),(d,n)->{String value=edit.getText().toString().trim();if(value.codePointCount(0,value.length())>40||value.isEmpty())message(getString(R.string.TetogramAppearanceInvalidValue));else result.accept(value);}).setNegativeButton(getString(R.string.Cancel),null).create());}
    private void message(CharSequence text){if(valid()&&getContext()!=null)showDialog(new AlertDialog.Builder(getContext()).setMessage(text).setPositiveButton(getString(R.string.OK),null).create());}
    private void pick(){try{startActivityForResult(new Intent(Build.VERSION.SDK_INT>=33?MediaStore.ACTION_PICK_IMAGES:Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),PICK);}catch(Exception e){try{startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("image/*").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),PICK);}catch(Exception ignored){message(getString(R.string.TetogramAppearanceMediaError));}}}
    @Override public void onActivityResultFragment(int request,int result,Intent data){super.onActivityResultFragment(request,result,data);if(request!=PICK||result!=Activity.RESULT_OK||data==null||data.getData()==null||!valid())return;int token=++generation;busy=true;refresh();job=AppearanceMedia.importUri(currentAccount,owner,data.getData(),false,asset->{busy=false;if(!valid()||token!=generation){if(asset!=null)AppearanceMedia.discard(asset.file);return;}if(asset==null){message(getString(R.string.TetogramAppearanceMediaError));refresh();return;}pending.add(asset.file);if(cropper!=null)cropper.clear();cropper=new ImageUpdater(currentAccount,false,ImageUpdater.FOR_TYPE_USER,false);cropper.parentFragment=this;cropper.setUploadAfterSelect(false);cropper.setDelegate((photo,video,time,path,big,small,isVideo,markup)->{if(!valid()||token!=generation||big==null)return;busy=true;refresh();File cropped=FileLoader.getInstance(currentAccount).getPathToAttach(big,true);job=AppearanceMedia.importUri(currentAccount,owner,android.net.Uri.fromFile(cropped),false,ready->{busy=false;if(!valid()||token!=generation){if(ready!=null)AppearanceMedia.discard(ready.file);return;}if(ready==null)message(getString(R.string.TetogramAppearanceMediaError));else{pending.add(ready.file);draft.set("launcher.icon.file",ready.file.getName());draft.values.remove("launcher.emoji");}refresh();});});try{cropper.openPhotoForEdit(asset.file.getAbsolutePath(),null,AndroidUtilities.getImageOrientation(asset.file.getAbsolutePath()),false);}catch(Exception e){message(getString(R.string.TetogramAppearanceMediaError));}refresh();});}
    private void pin(){if(!valid()||busy)return;Context context=getContext();if(!ShortcutManagerCompat.isRequestPinShortcutSupported(context)){message(getString(R.string.TetogramAppearanceShortcutUnsupported));return;}busy=true;refresh();int token=++generation;File f=AppearanceMedia.file(owner,draft.values.get("launcher.icon.file"));String emoji=draft.values.get("launcher.emoji"),label=draft.values.getOrDefault("launcher.label","Tetogram");Utilities.globalQueue.postRunnable(()->{Bitmap loaded=null;try{if(f!=null)loaded=ImageLoader.loadBitmap(f.getAbsolutePath(),null,256,256,true);}catch(Exception ignored){}final Bitmap image=loaded;AndroidUtilities.runOnUIThread(()->{if(!valid()||generation!=token){if(image!=null)image.recycle();return;}try{Bitmap icon=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(icon);canvas.drawColor(Theme.getColor(Theme.key_chats_actionBackground));if(image!=null){Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);canvas.drawBitmap(image,null,new Rect(0,0,256,256),paint);}else if(emoji!=null&&!emoji.isEmpty()){TextView text=new TextView(context);text.setGravity(Gravity.CENTER);text.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,136);text.setText(Emoji.replaceEmoji(emoji,text.getPaint().getFontMetricsInt(),false));text.measure(View.MeasureSpec.makeMeasureSpec(256,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(256,View.MeasureSpec.EXACTLY));text.layout(0,0,256,256);text.draw(canvas);}else{Drawable drawable=context.getResources().getDrawable(R.drawable.tetogram_mark).mutate();drawable.setBounds(0,0,256,256);drawable.draw(canvas);}Intent intent=new Intent(context,LaunchActivity.class).setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);ShortcutInfoCompat info=new ShortcutInfoCompat.Builder(context,SHORTCUT).setShortLabel(label).setLongLabel(label).setIntent(intent).setIcon(IconCompat.createWithAdaptiveBitmap(icon)).build();ShortcutManagerCompat.enableShortcuts(context,Collections.singletonList(info));if(!ShortcutManagerCompat.requestPinShortcut(context,info,null))message(getString(R.string.TetogramAppearanceShortcutUnsupported));}catch(Exception e){message(getString(R.string.TetogramAppearanceShortcutUnsupported));}finally{if(image!=null)image.recycle();busy=false;refresh();}});});}
    @Override protected void click(UItem item,View v,int p,float x,float y){if(valid()&&!busy&&item.id>0&&item.id<=actions.size())actions.get(item.id-1).run();}
}
