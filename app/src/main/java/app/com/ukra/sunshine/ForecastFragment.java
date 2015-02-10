package app.com.ukra.sunshine;

/**
 * Created by kirilldavidenko on 05.02.15.
 */

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        List<String> forecast = new ArrayList<>();
        forecast.add("Today-Sunny-23/18");
        forecast.add("Tomorrow-Foggy-20/17");
        forecast.add("Thursday-Rainy-18/16");
        forecast.add("Friday-Sunny-17/15");
        forecast.add("Saturday-Cloudy-18/15");
        forecast.add("Sunday-Frosty-6/-1");
        forecast.add("Monday-Foggy-7/5");
        forecast.add("Saturday-Cloudy-18/15");
        forecast.add("Sunday-Frosty-6/-1");
        forecast.add("Monday-Foggy-7/5");
        forecast.add("Saturday-Cloudy-18/15");
        forecast.add("Sunday-Frosty-6/-1");
        forecast.add("Monday-Foggy-7/5");
        forecast.add("Saturday-Cloudy-18/15");
        forecast.add("Sunday-Frosty-6/-1");
        forecast.add("Monday-Foggy-7/5");

        mForecastAdapter = new ArrayAdapter<>(
                getActivity(), R.layout.list_item_forecast,
                R.id.list_item_forecast_text_view, forecast);

        ListView listView = (ListView) rootView.findViewById(R.id.listview_forecast);
        listView.setAdapter(mForecastAdapter);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_refresh) {
            new FetchWeatherTask().execute("Kiev", "json", "metric", "7");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        public static final String QUERY_PARAM = "q";
        public static final String MODE_PARAM = "mode";
        public static final String UNITS_PARAM = "units";
        public static final String DAYS_PARAM = "cnt";
        public static final String TIME_MODE_PATH = "daily";
        public static final String API_METHOD_PATH = "forecast";
        public static final String API_VERSION_PATH = "2.5";
        public static final String API_ROOT_PATH = "data";
        public static final String API_ACCESS_METHOD = "http";
        public static final String API_BASE_URL = "api.openweathermap.org";

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected String[] doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String forecastJsonStr = null;

            try {
                Uri.Builder builder = new Uri.Builder();
                builder.scheme(API_ACCESS_METHOD)
                        .authority(API_BASE_URL)
                        .appendPath(API_ROOT_PATH)
                        .appendPath(API_VERSION_PATH)
                        .appendPath(API_METHOD_PATH)
                        .appendPath(TIME_MODE_PATH)
                .appendQueryParameter(QUERY_PARAM, params[0])
                .appendQueryParameter(MODE_PARAM, params[1])
                .appendQueryParameter(UNITS_PARAM, params[2])
                .appendQueryParameter(DAYS_PARAM, params[3]);

                URL url = new URL(builder.build().toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                String buffer = "";
                if (inputStream == null) {
                    forecastJsonStr = null;
                } else {
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        buffer += line + "\n";
                    }

                    if (buffer.length() == 0) {
                        forecastJsonStr = null;
                    }
                    forecastJsonStr = buffer;
                }
            } catch (IOException e) {
                Log.e("ForecastFragment", "Error ", e);
                forecastJsonStr = null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            int days = Integer.parseInt(params[3]);
            String[] result = null;
            try {
                result = getWeatherDataFromJson(forecastJsonStr, days);
            }  catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
            }
            return result;
        }

        private String getReadableDateString(long time) {
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         * <p/>
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[numDays];
            for (int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime = dayForecast.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            return resultStrs;
        }

        @Override
        protected void onPostExecute(String[] strings) {
            if(strings != null) {
                mForecastAdapter.clear();
                for (String str : strings) {
                    mForecastAdapter.add(str);
                }
            }
        }
    }
}