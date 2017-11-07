package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.os.Bundle;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.model.Player;

/**
 * Dialog that simply shows privacy and terms of the app. Required by Google Play policies.
 */
public class GenericMessageDialog extends BaseMessageDialog
{
    public GenericMessageDialog(Context context) {
        super(context);
    }
    public void init(String sTitle, String sMessage) {
        m_sMessage = sMessage;
        m_sTitle   = sTitle;
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putString("T", m_sTitle);
        outState.putString("M", m_sMessage);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init(outState.getString("T"), outState.getString("M"));
        return true;
    }
}
