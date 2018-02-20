package com.example.administrator.aktiehq;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.ShareActionProvider;



/**
 * A simple {@link Fragment} subclass.
 */
public class AktiendetailFragment extends Fragment {

    private String aktienInfo;
    public AktiendetailFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_aktiendetail, container, false);

                Intent empfangenerIntent = getActivity().getIntent();
        if (empfangenerIntent != null && empfangenerIntent.hasExtra(Intent.EXTRA_TEXT)) {
            aktienInfo = empfangenerIntent.getStringExtra(Intent.EXTRA_TEXT);
            ((TextView) rootView.findViewById(R.id.aktiendetail_text)).setText(aktienInfo);
        }
        return rootView;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_aktiendetailfragment, menu);

        MenuItem item = menu.findItem(R.id.action_teile_aktiendaten);
        ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
        Intent shareIntent = new Intent(Intent.ACTION_SEND);

        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT,aktienInfo);

        if (shareActionProvider != null) {
            shareActionProvider.setShareIntent(shareIntent);
        } else {
            Log.d("Aktiendetail: ", "onOptionsItemSelected: sap nicht vorhanden");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_starte_browser:
                zeigeWebseiteImBrowser();
                return true;
//            case R.id.action_teile_aktiendaten:
//                ShareActionProvider shareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(item);
//                Intent shareIntent = new Intent(Intent.ACTION_SEND);
//                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
//                shareIntent.setType("text/plain");
//                shareIntent.putExtra(Intent.EXTRA_TEXT,aktienInfo);
//                if (shareActionProvider != null) {
//                    shareActionProvider.setShareIntent(shareIntent);
//                } else {
//                    Log.d("Aktiendetail: ", "onOptionsItemSelected: sap nicht vorhanden");
//                }
//                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void zeigeWebseiteImBrowser() {
        int pos = aktienInfo.indexOf(":");
        String symbol = aktienInfo.substring(0,pos);
        String website = "http://finance.yahoo.com/q?s=" + symbol;
        Uri uri = Uri.parse(website);
        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
        if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d("Aktiendetail", "zeigeWebseiteImBrowser: Keine App da!");
        }

    }

}
