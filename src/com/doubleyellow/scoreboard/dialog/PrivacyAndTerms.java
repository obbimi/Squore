package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import com.doubleyellow.scoreboard.R;

/**
 * Dialog that simply shows privacy and terms of the app. Required by Google Play policies.
 */
public class PrivacyAndTerms extends BaseMessageDialog
{
    public PrivacyAndTerms(Context context) {
        super(context);
        m_mdWebViewResId = R.raw.privacy_and_terms;
        m_sTitle         = context.getString(R.string.privacy_and_terms) ;
    }
}
