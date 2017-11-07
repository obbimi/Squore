package com.doubleyellow.scoreboard.model;

public enum LockState {
    /** Special value for people that do not want locked models */
    UnlockedUnchangeable  (false, true , false , false),

    Unlocked               (false, true , false, false),
    UnlockedManual         (false, true , false, false),
    UnlockedEndOfFinalGame (false, true , true , false),
    LockedManual           (true , true , false, false),
    LockedManualGUI        (true , true , false, false),
    LockedEndOfMatch       (true , true , true , false),
    LockedEndOfMatchForced (true , true , true , false), // deprecated
    LockedIdleTime         (true , true , false, false),
    SharedEndedMatch       (true , false, false, false),
    LockedEndOfMatchRetired(true , true , true , true ), // for 'End Match' menu option
    LockedEndOfMatchConduct(true , true , true , true ), // for 'End Match' menu option
    ;
    // Hidden is a special locked state for shared matches that can only be set using the web interface. It is for hiding 'accidentally published' or 'faulty published' matches. Not used in the app

    private boolean bLocked                = false;
    private boolean bIsUnlockable          = true;
    private boolean bAllowRecordingConduct = false;
    private boolean bEndMatchManually      = false;
    LockState(boolean bLocked, boolean bIsUnlockable, boolean bAllowRecordingConduct, boolean bEndMatchManually) {
        this.bLocked = bLocked;
        this.bIsUnlockable = bIsUnlockable;
        this.bAllowRecordingConduct = bAllowRecordingConduct;
        this.bEndMatchManually = bEndMatchManually;
    }

    public boolean isLocked() {
        return bLocked;
    }
    public boolean isUnlockable() {
        return bIsUnlockable;
    }
    public boolean isEndMatchManually() {
        return bEndMatchManually;
    }
    public boolean AllowRecordingConduct() {
        return bAllowRecordingConduct;
    }
}
