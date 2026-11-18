/* Tetogram, 2026. GNU GPL v2 or later. Keeps the user's native auto-night schedule across forced light/dark modes. */
package org.telegram.messenger.tetogram;

public final class AppearanceNight {
    /** Telegram's Theme.AUTO_NIGHT_TYPE_NONE. */
    public static final int NONE=0;
    /** No remembered native value. */
    public static final int UNSET=-1;
    private AppearanceNight(){}
    public static boolean forced(String mode){return "dark".equals(mode)||"light".equals(mode);}
    /**
     * @param mode the active account's global Tetogram mode
     * @param current Theme.selectedAutoNightType right now
     * @param remembered value saved before Tetogram disabled auto-night, or {@link #UNSET}
     * @return {auto-night type to use, value to remember}
     */
    public static int[] decide(String mode,int current,int remembered){
        if(forced(mode))return new int[]{NONE,current!=NONE&&remembered==UNSET?current:remembered};
        return new int[]{remembered!=UNSET&&current==NONE?remembered:current,UNSET};
    }
}
