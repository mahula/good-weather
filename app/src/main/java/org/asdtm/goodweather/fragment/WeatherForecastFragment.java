package org.asdtm.goodweather.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.asdtm.goodweather.R;
import org.asdtm.goodweather.WeatherForecastActivity;

public class WeatherForecastFragment extends Fragment {

    private final String TAG = "WeatherForecastFragment";

    public WeatherForecastFragment newInstance() {
        return new WeatherForecastFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_recycler_view, container, false);
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.forecast_recycler_view);
        WeatherForecastActivity activity = (WeatherForecastActivity) getActivity();
        recyclerView.setAdapter(activity.getWeatherAdapter());
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        return v;
    }
}
