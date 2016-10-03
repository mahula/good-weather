package org.asdtm.goodweather.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import org.asdtm.goodweather.ConnectionDetector;
import org.asdtm.goodweather.model.WeatherForecast;
import org.asdtm.goodweather.utils.ApiKeys;
import org.asdtm.goodweather.utils.AppPreference;
import org.asdtm.goodweather.utils.Constants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static org.asdtm.goodweather.WeatherForecastActivity.sWeatherForecastList;

public class WeatherForecastService extends IntentService {

    private static final String TAG = "WeatherForecastService";

    public WeatherForecastService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        ConnectionDetector connectionDetector = new ConnectionDetector(this);
        if (!connectionDetector.isNetworkAvailableAndConnected()) {
            return;
        }

        SharedPreferences preferences = getSharedPreferences(Constants.APP_SETTINGS_NAME, 0);
        String latitude = preferences.getString(Constants.APP_SETTINGS_LATITUDE, "51.51");
        String longitude = preferences.getString(Constants.APP_SETTINGS_LONGITUDE, "-0.13");
        String locale = AppPreference.getLocale(this, Constants.APP_SETTINGS_NAME);
        String units = AppPreference.getTemperatureUnit(this);

        String requestResult = "";
        HttpURLConnection connection = null;
        try {
            URL url = getWeatherForecastUrl(latitude, longitude, units, locale);
            connection = (HttpURLConnection) url.openConnection();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                ByteArrayOutputStream byteArray = new ByteArrayOutputStream();
                InputStream inputStream = connection.getInputStream();

                int bytesRead;
                byte[] buffer = new byte[1024];
                while ((bytesRead = inputStream.read(buffer)) > 0) {
                    byteArray.write(buffer, 0, bytesRead);
                }
                byteArray.close();
                requestResult = byteArray.toString();
                AppPreference.saveLastUpdateTimeMillis(this);
            }

        } catch (IOException e) {
            Log.e(TAG, "IOException: " + requestResult);

        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        parseWeatherForecast(requestResult);

        Intent broadcastIntent = new Intent("org.asdtm.goodweather.action.FORECAST_UPDATED");
        sendBroadcast(broadcastIntent);
    }

    private void parseWeatherForecast(String data) {
        try {
            sWeatherForecastList.clear();

            JSONObject jsonObject = new JSONObject(data);
            JSONArray listArray = jsonObject.getJSONArray("list");

            int listArrayCount = listArray.length();
            for (int i = 0; i < listArrayCount; i++) {
                WeatherForecast weatherForecast = new WeatherForecast();
                JSONObject resultObject = listArray.getJSONObject(i);
                weatherForecast.setDateTime(resultObject.getLong("dt"));
                weatherForecast.setPressure(resultObject.getString("pressure"));
                weatherForecast.setHumidity(resultObject.getString("humidity"));
                weatherForecast.setWindSpeed(resultObject.getString("speed"));
                weatherForecast.setWindDegree(resultObject.getString("deg"));
                weatherForecast.setCloudiness(resultObject.getString("clouds"));
                if (resultObject.has("rain")) {
                    weatherForecast.setRain(resultObject.getString("rain"));
                }
                if (resultObject.has("snow")) {
                    weatherForecast.setSnow(resultObject.getString("snow"));
                }
                JSONObject temperatureObject = resultObject.getJSONObject("temp");
                weatherForecast.setTemperatureMin(Float.parseFloat(temperatureObject.getString("min")));
                weatherForecast.setTemperatureMax(Float.parseFloat(temperatureObject.getString("max")));
                weatherForecast.setTemperatureMorning(Float.parseFloat(temperatureObject.getString("morn")));
                weatherForecast.setTemperatureDay(Float.parseFloat(temperatureObject.getString("day")));
                weatherForecast.setTemperatureEvening(Float.parseFloat(temperatureObject.getString("eve")));
                weatherForecast.setTemperatureNight(Float.parseFloat(temperatureObject.getString("night")));
                JSONArray weatherArray = resultObject.getJSONArray("weather");
                JSONObject weatherObject = weatherArray.getJSONObject(0);
                weatherForecast.setDescription(weatherObject.getString("description"));
                weatherForecast.setIcon(weatherObject.getString("icon"));

                sWeatherForecastList.add(weatherForecast);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private URL getWeatherForecastUrl(String lat, String lon, String units, String lang) throws
                                                                                         MalformedURLException {
        String url = Uri.parse(Constants.WEATHER_FORECAST_ENDPOINT)
                        .buildUpon()
                        .appendQueryParameter("appid", ApiKeys.OPEN_WEATHER_MAP_API_KEY)
                        .appendQueryParameter("lat", lat)
                        .appendQueryParameter("lon", lon)
                        .appendQueryParameter("units", units)
                        .appendQueryParameter("lang", lang)
                        .build()
                        .toString();
        return new URL(url);
    }
}