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


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class InboxSettingsActivity extends ActionBarActivity implements View.OnClickListener{

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(R.id.container, new SettingsFragment())
                .commit();

        // use action bar here
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        ImageView devGPlus = (ImageView) findViewById(R.id.dev_gplus);
        ImageView devGit = (ImageView) findViewById(R.id.dev_git);
        ImageView devIn = (ImageView) findViewById(R.id.dev_in);
        ImageView devWeb = (ImageView) findViewById(R.id.dev_web);
        devGPlus.setOnClickListener(this);
        devGit.setOnClickListener(this);
        devIn.setOnClickListener(this);
        devWeb.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {

        String url = "";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        switch (v.getId()) {

            case R.id.dev_gplus:
                url = "https://plus.google.com/u/0/+MarcusViniciusAndreoGabilheri";
                break;
            case R.id.dev_git:
                url = "https://github.com/fnk0";
                break;
            case R.id.dev_in:
                url = "http://www.linkedin.com/in/marcusgabilheri/";
                break;

            case R.id.dev_web:
                url = "http://www.gabilheri.com/";
                break;
        }

        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    public static class SettingsFragment extends PreferenceFragment {

        @Override
        public void onCreate(Bundle paramBundle) {
            super.onCreate(paramBundle);

            addPreferencesFromResource(R.xml.pref_gmail);
            addAccountsPreference();
        }

        private void addAccountsPreference() {
            final String[] accounts = InboxExtension.getAllAccountNames(getActivity());
            Set<String> allAccountsSet = new HashSet<String>();
            allAccountsSet.addAll(Arrays.asList(accounts));

            MultiSelectListPreference accountsPreference = new MultiSelectListPreference(getActivity());
            accountsPreference.setKey(InboxExtension.PREF_ACCOUNTS);
            accountsPreference.setTitle(R.string.pref_inbox_accounts_title);
            accountsPreference.setEntries(accounts);
            accountsPreference.setEntryValues(accounts);
            accountsPreference.setDefaultValue(allAccountsSet);
            getPreferenceScreen().addPreference(accountsPreference);

            Preference.OnPreferenceChangeListener accountsChangeListener
                    = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    int numSelected = 0;
                    int numTotal = accounts.length;

                    try {
                        //noinspection unchecked
                        Set<String> selectedAccounts = (Set<String>) value;
                        if (selectedAccounts != null) {
                            numSelected = selectedAccounts.size();
                        }
                    } catch (ClassCastException ignored) {
                    }

                    preference.setSummary(getResources().getQuantityString(
                            R.plurals.pref_inbox_accounts_summary_template,
                            numTotal, numSelected, numTotal));
                    return true;
                }
            };

            accountsPreference.setOnPreferenceChangeListener(accountsChangeListener);
            accountsChangeListener.onPreferenceChange(accountsPreference,
                    PreferenceManager
                            .getDefaultSharedPreferences(getActivity())
                            .getStringSet(accountsPreference.getKey(), allAccountsSet));
        }
    }
}
