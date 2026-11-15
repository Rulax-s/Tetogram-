package org.telegram.ui;

public interface MainTabsActivityController {
    void setTabsVisible(boolean visible);
    default boolean getSectionsAnchor(DialogsActivity source,android.graphics.Rect anchor){return false;}
    default void setSectionsMenuState(DialogsActivity source,boolean available,boolean open,boolean animated){}

    default void setAppearance(long owner,org.telegram.messenger.tetogram.AppearanceConfig.Resolved style,boolean dark) {}
}
