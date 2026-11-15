/* Tetogram, 2026. GPL v2 or later. Full-screen native scene; draft remains owned by the editor. */
package org.telegram.ui.tetogram;
import java.util.ArrayList;
import android.content.Context;
import android.view.View;
import org.telegram.messenger.*;
import org.telegram.messenger.tetogram.AppearanceConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.Components.*;
public final class AppearancePreviewPage extends LabsPage {
    private final AppearanceConfig.Resolved style;private final boolean dark;private final int surface;private final String section;
    private AppearancePreview preview;
    public AppearancePreviewPage(int account,AppearanceConfig.Resolved style,boolean dark,int surface,String section){super(account);this.style=style;this.dark=dark;this.surface=surface;this.section=section;}
    @Override protected CharSequence title(){return LocaleController.getString(R.string.TetogramAppearancePreviewSurface);}
    @Override public View createView(Context context){preview=new AppearancePreview(context,currentAccount,style,dark,surface,section);return super.createView(context);}
    @Override protected void fill(ArrayList<UItem> items,UniversalAdapter adapter){if(preview!=null)items.add(UItem.asCustom(preview,Math.max(240,Math.round((AndroidUtilities.displaySize.y-mSystemInsets.top-mSystemInsets.bottom-ActionBar.getCurrentActionBarHeight())/AndroidUtilities.density))));}
    @Override protected void click(UItem item,View view,int position,float x,float y){}
    @Override public void onResume(){super.onResume();if(preview!=null)preview.setResumed(true);}
    @Override public void onPause(){if(preview!=null)preview.setResumed(false);super.onPause();}
}
