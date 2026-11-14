package org.telegram.ui;

public interface MainTabsActivityController {
    void setTabsVisible(boolean visible);
    default void setAppearance(long owner,org.telegram.messenger.tetogram.AppearanceConfig.Resolved style,boolean dark) {}
}
