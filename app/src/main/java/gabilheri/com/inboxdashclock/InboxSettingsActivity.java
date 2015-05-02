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
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InboxSettingsActivity extends AppCompatActivity implements View.OnClickListener{

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

    public static class SettingsFragment extends PreferenceFragment implements BillingProcessor.IBillingHandler{

        BillingProcessor billingProcessor;
        String billingID = null;
        MaterialDialog donateDialog;
        List<String> products;

        @Override
        public void onCreate(Bundle paramBundle) {
            super.onCreate(paramBundle);
            billingProcessor = new BillingProcessor(getActivity(), getString(R.string.billing_key), this);
            addPreferencesFromResource(R.xml.pref_inbox);
            addAccountsPreference();

            findPreference("donate").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showDonateDialog();
                    return true;
                }
            });

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

        @Override
        public void onProductPurchased(String purchaseId, TransactionDetails transactionDetails) {
            if(donateDialog != null) {
                if(donateDialog.isShowing()) {
                    donateDialog.dismiss();
                }
            }
            boolean purchased = false;

            for(String s : products) {
                if(s.equals(purchaseId)) {
                    purchased = true;
                    Toast.makeText(getActivity(), "You can only buy this item once!", Toast.LENGTH_LONG).show();
                }
            }
            if(!purchased) {
                Toast.makeText(getActivity(), "Thank you!! You rock!", Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onPurchaseHistoryRestored() {

        }

        @Override
        public void onBillingError(int i, Throwable throwable) {
            if(donateDialog != null) {
                if(donateDialog.isShowing()) {
                    donateDialog.dismiss();
                }
            }
            new MaterialDialog.Builder(getActivity())
                    .title("Error")
                    .content("An error occurred while processing your purchase.")
                    .positiveText("Dismiss")
                    .positiveColor(getResources().getColor(R.color.accent_color))
                    .build()
                    .show();
        }

        @Override
        public void onBillingInitialized() {

        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (!billingProcessor.handleActivityResult(requestCode, resultCode, data)) {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        public void showDonateDialog() {
            donateDialog = new MaterialDialog.Builder(getActivity())
                    .title("Donate")
                    .icon(getResources().getDrawable(R.drawable.ic_donate_cart))
                    .customView(R.layout.donate_dialog, true)
                    .negativeText("Cancel")
                    .negativeColor(getResources().getColor(R.color.accent_color))
                    .positiveText("Donate")
                    .positiveColor(getResources().getColor(R.color.primary_dark))
                    .build();

            View v = donateDialog.getCustomView();

            if(v != null) {
                RadioButton support = (RadioButton) v.findViewById(R.id.development);
                RadioButton coffee = (RadioButton) v.findViewById(R.id.coffee);
                RadioButton beer = (RadioButton) v.findViewById(R.id.beer);
                RadioButton pizza = (RadioButton) v.findViewById(R.id.pizza);
                RadioButton dinner = (RadioButton) v.findViewById(R.id.dinner);
                RadioButton college = (RadioButton) v.findViewById(R.id.college);

                final ArrayList<RadioButton> buttons = new ArrayList<>();
                buttons.add(support);
                buttons.add(coffee);
                buttons.add(beer);
                buttons.add(pizza);
                buttons.add(dinner);
                buttons.add(college);
                products =  billingProcessor.listOwnedProducts();
                for(RadioButton rb : buttons) {
                    rb.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            for(RadioButton r : buttons) {
                                r.setChecked(false);
                            }
                            ((RadioButton)v).setChecked(true);
                            billingID = getBillingId(v.getId());
                        }
                    });
                }

                View positive = donateDialog.getActionButton(DialogAction.POSITIVE);
                positive.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (billingID != null) {
//                            Log.d(LOG_TAG, "Purchasing... " + billingID);
                                billingProcessor.purchase(getActivity(), billingID);
                        }
                    }
                });
            }

            donateDialog.show();
        }

        public String getBillingId(int id) {
            switch (id) {
                case R.id.development:
                    return getResources().getString(R.string.encourage_development);
                case R.id.coffee:
                    return getResources().getString(R.string.coffee);
                case R.id.beer:
                    return getResources().getString(R.string.beer);
                case R.id.pizza:
                    return getResources().getString(R.string.pizza);
                case R.id.dinner:
                    return getResources().getString(R.string.dinner);
                case R.id.college:
                    return getResources().getString(R.string.college);
                default:
                    return null;
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if(billingProcessor != null) {
                billingProcessor.release();
            }
        }



    }

}
