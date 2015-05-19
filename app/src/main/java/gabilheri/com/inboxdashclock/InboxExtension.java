/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gabilheri.com.inboxdashclock;


import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Gmail unread count extension.
 */
public class InboxExtension extends DashClockExtension {
    private static final String TAG = InboxExtension.class.getSimpleName();

    public static final String PREF_ACCOUNTS = "pref_inbox_accounts";
    public static final String PREF_LABEL = "pref_inbox_label";
    public static final String PREF_ALL = "pref_show_all";
    public static final String PREF_PROMOS = "pref_promos";
    public static final String PREF_SHOW_ACCOUNT = "pref_show_accounts";

    public static final String LABEL_UPDATES = "Updates";
    public static final String LABEL_SOCIAL = "Social";
    public static final String LABEL_FORUMS = "Forums";
    public static final String LABEL_PROMOS = "Promos";

    public static final String HIDE_LABELS = "hide_labels";

    private static final String ACCOUNT_TYPE_GOOGLE = "com.google";

    private static final String SECTIONED_INBOX_CANONICAL_NAME_PREFIX = "^sq_ig_i_";
    private static final String SECTIONED_INBOX_CANONICAL_NAME_PERSONAL = "^sq_ig_i_personal";

    //private static final String[] FEATURES_MAIL = {"service_mail"};

    static String[] getAllAccountNames(Context context) {
        final Account[] accounts = AccountManager.get(context).getAccountsByType(
                InboxExtension.ACCOUNT_TYPE_GOOGLE);
        final String[] accountNames = new String[accounts.length];
        for (int i = 0; i < accounts.length; i++) {
            accountNames[i] = accounts[i].name;
        }
        return accountNames;
    }

    private Set<String> getSelectedAccounts() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        final String[] accounts = InboxExtension.getAllAccountNames(this);
        Set<String> allAccountsSet = new HashSet<String>();
        allAccountsSet.addAll(Arrays.asList(accounts));
        return sp.getStringSet(PREF_ACCOUNTS, allAccountsSet);
    }

    @Override
    protected void onInitialize(boolean isReconnect) {
        super.onInitialize(isReconnect);
        if (!isReconnect) {
            Set<String> selectedAccounts = getSelectedAccounts();
            String[] uris = new String[selectedAccounts.size()];

            int i = 0;
            for (String account : selectedAccounts) {
                uris[i++] = InboxContract.Labels.getLabelsUri(account).toString();
                // TODO: only watch the individual label's URI (GmailContract.Labels.URI)
            }

            addWatchContentUris(uris);
        }
    }

    @Override
    protected void onUpdateData(int reason) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String labelCanonical = sp.getString(PREF_LABEL, "i");
        Set<String> selectedAccounts = getSelectedAccounts();

        if ("i".equals(labelCanonical)) {
            labelCanonical = InboxContract.Labels.LabelCanonicalNames.CANONICAL_NAME_INBOX;
        } else if ("p".equals(labelCanonical)) {
            labelCanonical = InboxContract.Labels.LabelCanonicalNames.CANONICAL_NAME_PRIORITY_INBOX;
        }

        Set<String> selectedLabels = sp.getStringSet(HIDE_LABELS, new HashSet<String>());

        for(String s : selectedLabels) {
            Log.d(TAG, "Label: " + s);
        }

        int unread = 0;
        int unreadUpdate = 0;
        String labelUpdate = "";
        int unreadSocial = 0;
        String labelSocial = "";
        int unreadForums = 0;
        String labelForums = "";
        int unreadPersonal = 0;
        String labelPersonal = "";

        int unreadPromos = 0;
        String labelPromo = "";

        List<Pair<String, Integer>> unreadPerAccount = new ArrayList<Pair<String, Integer>>();
        String lastUnreadLabelUri = null;

        for (String account : selectedAccounts) {
            Cursor cursor = tryOpenLabelsCursor(account);
            if (cursor == null || cursor.isAfterLast()) {
                Log.i(TAG, "No Inbox information found for account.");
                if (cursor != null) {
                    cursor.close();
                }
                continue;
            }

            int accountUnread = 0;

            while (cursor.moveToNext()) {
                String thisCanonicalName = cursor.getString(LabelsQuery.CANONICAL_NAME);
                String name = cursor.getString(3);
                int thisUnread = cursor.getInt(LabelsQuery.NUM_UNREAD_CONVERSATIONS);
//                Log.d(TAG, "Canonical: " + thisCanonicalName + " -- Name: " + name);

                if(thisCanonicalName.equals(InboxContract.Labels.LabelCanonicalNames.CANONICAL_NAME_GROUP)) {
                    unreadForums = thisUnread;
                    labelForums = name;
                } else if(thisCanonicalName.equals(InboxContract.Labels.LabelCanonicalNames.CANONICAL_NAME_UPDATES)) {
                    unreadUpdate = thisUnread;
                    labelUpdate = name;
                } else if(thisCanonicalName.equals(InboxContract.Labels.LabelCanonicalNames.CANONICAL_NAME_PERSONAL)) {
                    unreadPersonal = thisUnread;
                    labelPersonal = name;
                } else if(thisCanonicalName.equals(InboxContract.Labels.LabelCanonicalNames.CANONICAL_NAME_SOCIAL)) {
                    unreadSocial = thisUnread;
                    labelSocial = name;
                } else if(thisCanonicalName.equals(InboxContract.Labels.LabelCanonicalNames.CANONICAL_NAME_PROMO)) {
                    unreadPromos = thisUnread;
                    labelPromo = name;
//                    Log.d(TAG, "Found promos!! " + unreadPromos);
                }

                if (labelCanonical.equals(thisCanonicalName)) {
                    if (thisUnread > 0) {
                        lastUnreadLabelUri = cursor.getString(LabelsQuery.URI);
                    }
                    break;
                } else if (!TextUtils.isEmpty(thisCanonicalName)
                        && thisCanonicalName.startsWith(SECTIONED_INBOX_CANONICAL_NAME_PREFIX)) {
                    accountUnread += thisUnread;
                    if (thisUnread > 0 && SECTIONED_INBOX_CANONICAL_NAME_PERSONAL.equals(thisCanonicalName)) {
                        lastUnreadLabelUri = cursor.getString(LabelsQuery.URI);
                    }
                }
            }

            if (accountUnread > 0) {
                unreadPerAccount.add(new Pair<String, Integer>(account, accountUnread));
                unread += accountUnread;
            }

            cursor.close();
        }

        StringBuilder body = new StringBuilder();
        if(sp.getBoolean(PREF_SHOW_ACCOUNT, true)) {
            for (Pair<String, Integer> pair : unreadPerAccount) {
                if (pair.second == 0) {
                    continue;
                }

                if (body.length() > 0) {
                    body.append("\n");
                }
                body.append(pair.first).append(" (").append(pair.second).append(")");
                if(sp.getBoolean(PREF_ALL, true)) {
                    body.append("\n");
                }
            }
        }

        boolean hasUpdates = false;
        boolean hasSocial = false;
        boolean hasForums = false;

        if(sp.getBoolean(PREF_ALL, true)) {
            if(unreadPersonal > 0) {
                body.append(labelPersonal).append(" (").append(unreadPersonal).append(")");
            }

            if(!selectedLabels.contains(LABEL_UPDATES)) {
                if(unreadUpdate > 0) {
                    if(unreadPersonal > 0) {
                        body.append("\n");
                    }
                    hasUpdates = true;
                    body.append(labelUpdate).append(" (").append(unreadUpdate).append(")");
                }
            } else {
                Log.d(TAG, "Updates should be hidden");
                unread -= unreadUpdate;
            }

            if(!selectedLabels.contains(LABEL_SOCIAL)) {
                if(unreadSocial > 0) {
                    if(unreadPersonal > 0|| hasUpdates) {
                        body.append("\n");
                    }
                    hasSocial = true;
                    body.append(labelSocial).append(" (").append(unreadSocial).append(")");
                }
            } else {
                Log.d(TAG, "Social should be hidden");
                unread -= unreadSocial;
            }

            if(!selectedLabels.contains(LABEL_FORUMS)) {
                if(unreadForums > 0) {
                    if(unreadPersonal > 0|| hasUpdates || hasSocial) {
                        body.append("\n");
                    }
                    hasForums = true;
                    body.append(labelForums).append(" (").append(unreadForums).append(")");
                }
            } else {
                Log.d(TAG, "Forums should be hidden");
                unread -= unreadForums;
            }

            if(!selectedLabels.contains(LABEL_PROMOS)) {
                if(unreadPromos > 0) {
                    if(unreadPersonal > 0|| hasUpdates || hasSocial || hasForums) {
                        body.append("\n");
                    }
                    body.append(labelPromo).append(" (").append(unreadPromos).append(")");
                }
            } else {
                Log.d(TAG, "Promos should be hidden");
                unread -= unreadPromos;
            }
        }

        Intent clickIntent = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.inbox");

        publishUpdate(new ExtensionData()
                .visible(unread > 0)
                .status(Integer.toString(unread))
                .expandedTitle(getResources().getQuantityString(R.plurals.inbox_title_template, unread, unread))
                .icon(R.drawable.ic_inbox_logo)
                .expandedBody(body.toString())
                .clickIntent(clickIntent));
    }

    private Cursor tryOpenLabelsCursor(String account) {
        try {
            return getContentResolver().query(
                    InboxContract.Labels.getLabelsUri(account),
                    LabelsQuery.PROJECTION,
                    null, // NOTE: the Labels API doesn't allow selections here
                    null,
                    null);

        } catch (Exception e) {
            // From developer console: "Permission Denial: opening provider com.google.android.gsf..
            // From developer console: "SQLiteException: no such table: labels"
            // From developer console: "NullPointerException"
            Log.e(TAG, "Error opening Inbox labels", e);
            return null;
        }
    }

    private interface LabelsQuery {
        String[] PROJECTION = {
                InboxContract.Labels.NUM_UNREAD_CONVERSATIONS,
                InboxContract.Labels.URI,
                InboxContract.Labels.CANONICAL_NAME,
                InboxContract.Labels.NAME,
        };

        int NUM_UNREAD_CONVERSATIONS = 0;
        int URI = 1;
        int CANONICAL_NAME = 2;
    }
}
