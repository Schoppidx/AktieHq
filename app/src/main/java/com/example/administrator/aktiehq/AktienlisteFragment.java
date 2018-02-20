package com.example.administrator.aktiehq;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


/**
 * A simple {@link Fragment} subclass.
 */
public class AktienlisteFragment extends Fragment {
    ArrayAdapter<String> aktienlisteAdapter;
    SwipeRefreshLayout swipeRefreshLayout;

    private final String TAG = AktienlisteFragment.class.getSimpleName();

    public AktienlisteFragment() {
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
        View rootView = inflater.inflate(R.layout.fragment_aktienliste, container, false);
        String[] aktienlisteArray = {};
//                "Adidas - Kurs: 73,45 €",
//                "Allianz - Kurs: 145,12 €",
//                "BASF - Kurs: 84,27 €",
//                "Bayer - Kurs: 128,60 €",
//                "Beiersdorf - Kurs: 80,55 €",
//                "BMW St. - Kurs: 104,11 €",
//                "Commerzbank - Kurs: 12,47 €",
//                "Continental - Kurs: 209,94 €",
//                "Daimler - Kurs: 84,33 €"
//        };

        if (savedInstanceState != null) {
            aktienlisteArray = savedInstanceState.getStringArray("Saved_Array");
            Log.d(TAG, "onCreateView: Zustand wieder hergestellt");
        }
        List<String> aktienliste = new ArrayList<>(Arrays.asList(aktienlisteArray));
        aktienlisteAdapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_aktienliste,
                R.id.list_item_aktieliste_textview, aktienliste);
        aktualisiereDaten();


        ListView aktienlisteListView = rootView.findViewById(R.id.listview_aktienliste);
        aktienlisteListView.setAdapter(aktienlisteAdapter);

        aktienlisteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String aktienInfo = parent.getItemAtPosition(position).toString();
                Intent aktiendetailIntent = new Intent(getActivity(), AktiendetailActivity.class);
                aktiendetailIntent.putExtra(Intent.EXTRA_TEXT, aktienInfo);
//                Toast.makeText(getActivity(), aktienInfo, Toast.LENGTH_SHORT).show();
                startActivity(aktiendetailIntent);
            }
        });
        Log.d(TAG, "onCreateView: Liste fertig");

        swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh_layout_aktienliste);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                aktualisiereDaten();
            }
        });

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_aktienlistefragment, menu);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        int anzElemente = aktienlisteAdapter.getCount();
        String aktienlisteArray[] = new String[anzElemente];
        for (int i = 0; i < anzElemente; i++) {
            aktienlisteArray[i] = aktienlisteAdapter.getItem(i);
        }
        outState.putStringArray("Saved_Array", aktienlisteArray);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_daten_aktualiseren:
                Toast.makeText(getActivity(), "Aktuaslisieren", Toast.LENGTH_SHORT).show();
                aktualisiereDaten();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private boolean checkForWebAccess() {

        ConnectivityManager cm = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void aktualisiereDaten() {
        if (!checkForWebAccess()) {
            Toast.makeText(getActivity(), "kein inet",Toast.LENGTH_SHORT).show();
            return;
        }
        HoleDatenTask datenTask = new HoleDatenTask();

        SharedPreferences sPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String prefAktienKey = getString(R.string.preference_aktienliste_key);
        String prefAktienDefault = getString(R.string.preference_aktienliste_default);
        String aktienliste = sPrefs.getString(prefAktienKey, prefAktienDefault);

        String prefIndizemodusKey = getString(R.string.preference_indizemodus_key);
        boolean indiziemodus = sPrefs.getBoolean(prefIndizemodusKey, false);
        if (indiziemodus) {
            String indizieliste = "^GDAXI,^TECDAX,^MDAXI,^SDAXI,^GSPC,^N225,^HSI,XAGUSD=X,XAUUSD=X";
            datenTask.execute(indizieliste);
        } else {
            datenTask.execute(aktienliste);
        }
    }

    public class HoleDatenTask extends AsyncTask<String, Integer, String[]> {

        ProgressDialog dialog;

        private String[] ergerbnisArray;
        private final String URL_PARAMETER = "http://www.programmierenlernenhq.de/tools/query.php";

        /*
        Diese Methode wirds HintergrundTask ausgeführt.
         */
        @Override
        protected String[] doInBackground(String... strings) {
            if (strings.length == 0) {
                Log.i(TAG, "doInBackground: strings leer");
                return null;
            }

            String aktiendatenXMLString = "";
            String symbols = strings[0];

            String anfrageString = URL_PARAMETER + "?s=" + symbols;
            Log.i(TAG, "doInBackground: " + anfrageString);

            HttpURLConnection httpURLConnection = null;
            BufferedReader bufferedReader = null;


//            anfrageString = "https://query.yahooapis.com/v1/public/yql?q=select%20*%20from%20csv%20where%20url%3D'https%3A%2F%2Fwww.alphavantage.co%2Fquery%3Ffunction%3DTIME_SERIES_DAILY_ADJUSTED%26symbol%3DAMZN%26outputsize%3Dfull%26apikey%3DDLVRDEO6Z583RMPA%26datatype%3Dcsv'&format=xml&env=store%3A%2F%2Fdatatables.org%2Falltableswithkeys";
            try {

                URL url = new URL(anfrageString);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                if (inputStream == null) {
                    return null;
                }
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String line;
                int i = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    aktiendatenXMLString += line + "\n";
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    publishProgress(50);
                }

                if (aktiendatenXMLString.length() == 0) {
                    return null;
                }
                Log.i(TAG, "doInBackground: " + aktiendatenXMLString);
            } catch (IOException e) {

                Log.e(TAG, "doInBackground: ", e);
            } finally {

                if (httpURLConnection == null) {
                    httpURLConnection.disconnect();
                }
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "doInBackground: fehler beim Schließen", e);
                    }
                }
            }
            //Hier parsen wir die XML Daten
            return leseXmlAktiendatenAus(aktiendatenXMLString);
        }

        /*
        Diese Methode kommt vor der Ausfühung von doInBackground() zur ausführung.
         */
        @Override
        protected void onPreExecute() {
            ergerbnisArray = new String[20];
            dialog = new ProgressDialog(getContext());
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(100);
            dialog.show();

            super.onPreExecute();
        }

        /*
        Diese Methode kommt nach der Ausfühung von doInBackground() zur ausführung.
         */
        @Override
        protected void onPostExecute(String[] strings) {
            dialog.dismiss();
            if (strings != null) {
                aktienlisteAdapter.clear();
                for (String aktienString : strings) {
                    aktienlisteAdapter.add(aktienString);
                    Log.i(TAG, "onPostExecute: " + aktienString);
                }
            }
            swipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), "Daten vollständig geladen", Toast.LENGTH_SHORT).show();
        }

        /*
        Hier kann der Fortschritt der Ausführung angezeigt werden
         */
        @Override
        protected void onProgressUpdate(Integer... values) {
//            ProgressBar progressBar = getActivity().findViewById(R.id.progressBar2);
//            progressBar.setVisibility(View.VISIBLE);
//            progressBar.setMax(50);
//            progressBar.setProgress(values[0]);
            dialog.incrementProgressBy(values[0]);
//            Log.d(TAG, "onProgressUpdate: " + values[0] + " geladen.");
        }

        private String[] leseXmlAktiendatenAus(String xmlString) {
            Document doc;
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            try {
                DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                InputSource inputSource = new InputSource();
                inputSource.setCharacterStream(new StringReader(xmlString));

                doc = documentBuilder.parse(inputSource);
            } catch (ParserConfigurationException e) {
                Log.e(TAG, "leseXmlAktiendatenAus: ", e);

                return null;
            } catch (SAXException e) {
                Log.e(TAG, "leseXmlAktiendatenAus: ", e);

                return null;
            } catch (IOException e) {
                Log.e(TAG, "leseXmlAktiendatenAus: ", e);

                return null;
            }
            Element xmlAktiendaten = doc.getDocumentElement();
            NodeList aktienListe = xmlAktiendaten.getElementsByTagName("row");

            int anzahlAktien = aktienListe.getLength();
            int anzahlAktienParameter = aktienListe.item(0).getChildNodes().getLength();
            String ausgabeArray[] = new String[anzahlAktien];
            String alleAktienDatenArray[][] = new String[anzahlAktien][anzahlAktienParameter];

            Node aktienParameter;
            String aktienParameterWert;

            for (int i = 0; i < anzahlAktien; i++) {
                NodeList aktienParameterListe = aktienListe.item(i).getChildNodes();
                for (int j = 0; j < anzahlAktienParameter; j++) {
                    aktienParameter = aktienParameterListe.item(j);
                    aktienParameterWert = aktienParameter.getFirstChild().getNodeValue();
                    alleAktienDatenArray[i][j] = aktienParameterWert;
                }
                ausgabeArray[i] = alleAktienDatenArray[i][0]; //symbol
                ausgabeArray[i] += ": " + alleAktienDatenArray[i][4]; //price
                ausgabeArray[i] += " " + alleAktienDatenArray[i][2]; //currency
                ausgabeArray[i] += " (" + alleAktienDatenArray[i][8] + ") "; //percent
                ausgabeArray[i] += " - [" + alleAktienDatenArray[i][1] + "]"; //name

                Log.i(TAG, "XML Ausgabe: " + ausgabeArray[i]);
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                publishProgress(50);
            }


            return ausgabeArray;
        }
    }
}
