package gabilheri.com.inboxdashclock;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.net.Uri;
import android.text.TextUtils;


public final class InboxContract {
    private InboxContract() {}

    /**
     * Permission required to access this {@link android.content.ContentProvider}
     */
    public static final String PERMISSION = "com.google.android.gm.permission.READ_CONTENT_PROVIDER";

    /**
     * Authority for the Gmail content provider.
     */
    public static final String AUTHORITY = "com.google.android.gm";

    static final String LABELS_PARAM = "/labels";
    static final String LABEL_PARAM = "/label/";
    static final String BASE_URI_STRING = "content://" + AUTHORITY;
    static final String PACKAGE = "com.google.android.gm";


    /**
     * Check if the installed Gmail app supports querying for label information.
     *
     * @param c an application Context
     * @return true if it's safe to make label API queries
     */
    public static boolean canReadLabels(Context c) {
        boolean supported = false;

        try {
            final PackageInfo info = c.getPackageManager().getPackageInfo(PACKAGE,
                    PackageManager.GET_PROVIDERS | PackageManager.GET_PERMISSIONS);
            boolean allowRead = false;
            if (info.permissions != null) {
                for (int i = 0, len = info.permissions.length; i < len; i++) {
                    final PermissionInfo perm = info.permissions[i];
                    if (PERMISSION.equals(perm.name)
                            && perm.protectionLevel < PermissionInfo.PROTECTION_SIGNATURE) {
                        allowRead = true;
                        break;
                    }
                }
            }
            if (allowRead && info.providers != null) {
                for (int i = 0, len = info.providers.length; i < len; i++) {
                    final ProviderInfo provider = info.providers[i];
                    if (AUTHORITY.equals(provider.authority) &&
                            TextUtils.equals(PERMISSION, provider.readPermission)) {
                        supported = true;
                    }
                }
            }
        } catch (NameNotFoundException e) {
            // Gmail app not found
        }
        return supported;
    }



    /**
     * Table containing label information.
     */
    public static final class Labels {
        /**
         * Label canonical names for default Gmail system labels.
         */
        public static final class LabelCanonicalNames {
            /**
             * Canonical name for the Inbox label
             */
            public static final String CANONICAL_NAME_INBOX = "^i";

            /**
             * Canonical name for the Priority Inbox label
             */
            public static final String CANONICAL_NAME_PRIORITY_INBOX = "^iim";

            /**
             * Canonical name for the Starred label
             */
            public static final String CANONICAL_NAME_STARRED = "^t";

            /**
             * Canonical name for the Sent label
             */
            public static final String CANONICAL_NAME_SENT = "^f";

            /**
             * Canonical name for the Drafts label
             */
            public static final String CANONICAL_NAME_DRAFTS = "^r";

            /**
             * Canonical name for the All Mail label
             */
            public static final String CANONICAL_NAME_ALL_MAIL = "^all";

            /**
             * Canonical name for the Spam label
             */
            public static final String CANONICAL_NAME_SPAM = "^s";

            /**
             * Canonical name for the Trash label
             */
            public static final String CANONICAL_NAME_TRASH = "^k";

            public static final String CANONICAL_NAME_PROMO = "^sq_ig_i_promo";

            public static final String CANONICAL_NAME_SOCIAL = "^sq_ig_i_social";

            public static final String CANONICAL_NAME_PERSONAL = "^sq_ig_i_personal";

            public static final String CANONICAL_NAME_GROUP = "^sq_ig_i_group";

            public static final String CANONICAL_NAME_UPDATES = "^sq_ig_i_notification";

            private LabelCanonicalNames() {}
        }

        /**
         * The MIME-type of uri providing a directory of
         * label items.
         */
        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.com.google.android.gm.label";

        /**
         * The MIME-type of a label item.
         */
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.com.google.android.gm.label";

        public static final String CANONICAL_NAME = "canonicalName";

        public static final String NAME = "name";

        public static final String NUM_CONVERSATIONS = "numConversations";

        public static final String NUM_UNREAD_CONVERSATIONS = "numUnreadConversations";

        public static final String TEXT_COLOR = "text_color";

        public static final String BACKGROUND_COLOR = "background_color";

        public static final String URI = "labelUri";


        public static Uri getLabelsUri(String account) {
            return Uri.parse(BASE_URI_STRING + "/" + account + LABELS_PARAM);
        }

        private Labels() {}
    }
}
