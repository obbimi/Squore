package com.doubleyellow.scoreboard.firebase;

import com.doubleyellow.scoreboard.main.ScoreBoard;

import java.util.UUID;

/** Firebase Cloud Messaging */
public interface FCMHandler {
    public static final String key_action = "action";

    public void init(ScoreBoard scoreBoard, UUID sPusherUUID, String sMatchPlayerNames);
    public boolean handleAction(String sAction);
    public void cleanup();
}
