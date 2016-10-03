package org.asdtm.goodweather;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.asdtm.goodweather.adapter.ViewPagerAdapter;
import org.asdtm.goodweather.adapter.WeatherForecastAdapter;
import org.asdtm.goodweather.fragment.WeatherForecastFragment;
import org.asdtm.goodweather.model.WeatherForecast;
import org.asdtm.goodweather.service.WeatherForecastService;

import java.util.ArrayList;
import java.util.List;

public class WeatherForecastActivity extends AppCompatActivity {

    private final String TAG = "WeatherForecastActivity";

    private DrawerLayout mDrawerLayout;
    private BroadcastReceiver mReceiver;
    private Menu mToolbarMenu;

    public static List<WeatherForecast> sWeatherForecastList;
    private WeatherForecastAdapter mForecastAdapter;

    private ConnectionDetector mConnectionDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather_forecast);

        mConnectionDetector = new ConnectionDetector(this);
        sWeatherForecastList = new ArrayList<>();

        Toolbar toolbar = (Toolbar) findViewById(R.id.forecast_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ViewPager viewPager = (ViewPager) findViewById(R.id.forecast_view_pager);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.forecast_tab_layout);
        tabLayout.setupWithViewPager(viewPager);

        NavigationView navigationView = (NavigationView) findViewById(
                R.id.forecast_navigation_view);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.forecast_drawer);
        navigationView.setNavigationItemSelectedListener(navigationViewListener);

        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter viewPagerAdapter = new ViewPagerAdapter(getSupportFragmentManager());
        viewPagerAdapter.addFragment(new WeatherForecastFragment().newInstance(), "Forecast");
        viewPager.setAdapter(viewPagerAdapter);
    }

    public WeatherForecastAdapter getWeatherAdapter() {
        mForecastAdapter = new WeatherForecastAdapter(this, sWeatherForecastList,
                                                      getSupportFragmentManager());
        return mForecastAdapter;
    }

    private void updateUI() {
        mForecastAdapter.notifyDataSetChanged();
    }

    private NavigationView.OnNavigationItemSelectedListener navigationViewListener =
            new NavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    int itemId = item.getItemId();
                    switch (itemId) {
                        case R.id.nav_menu_current_weather:
                            Intent mainActivityIntent = new Intent(WeatherForecastActivity.this,
                                                                   MainActivity.class);
                            mainActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(mainActivityIntent);
                            break;
                        case R.id.nav_menu_weather_forecast:
                            Intent weatherForecastIntent = new Intent(WeatherForecastActivity.this,
                                                                      WeatherForecastActivity.class);
                            startActivity(weatherForecastIntent);
                            break;
                        case R.id.nav_settings:
                            Intent goToSettings = new Intent(WeatherForecastActivity.this,
                                                             SettingsActivity.class);
                            startActivity(goToSettings);
                            break;
                        case R.id.nav_feedback:
                            Intent sendMessage = new Intent(Intent.ACTION_SEND);
                            sendMessage.setType("message/rfc822");
                            sendMessage.putExtra(Intent.EXTRA_EMAIL, new String[]{
                                    getResources().getString(R.string.feedback_email)});
                            try {
                                startActivity(Intent.createChooser(sendMessage, "Send feedback"));
                            } catch (android.content.ActivityNotFoundException e) {
                                Toast.makeText(WeatherForecastActivity.this,
                                               "Communication app not found",
                                               Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case R.id.nav_menu_bitcoin_donation:
                            BitcoinDonationDialog dialog = BitcoinDonationDialog.newInstance();
                            dialog.show(getFragmentManager(), "bitcoinDonationDialog");
                            break;
                    }

                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
            };

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter =
                new IntentFilter("org.asdtm.goodweather.action.FORECAST_UPDATED");
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setUpdateButtonState(false);
                updateUI();
            }
        };

        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mToolbarMenu = menu;
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.weather_forecast_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                if (mConnectionDetector.isNetworkAvailableAndConnected()) {
                    startService(new Intent(this, WeatherForecastService.class));
                    setUpdateButtonState(true);
                } else {
                    Toast.makeText(WeatherForecastActivity.this,
                                   R.string.connection_not_found,
                                   Toast.LENGTH_SHORT).show();
                    setUpdateButtonState(false);
                }
                return true;
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpdateButtonState(boolean isUpdate) {
        if (mToolbarMenu != null) {
            MenuItem updateItem = mToolbarMenu.findItem(R.id.menu_refresh);
            ProgressBar progressUpdate = (ProgressBar) findViewById(R.id.forecast_progress_bar);
            if (isUpdate) {
                updateItem.setVisible(false);
                progressUpdate.setVisibility(View.VISIBLE);
            } else {
                progressUpdate.setVisibility(View.GONE);
                updateItem.setVisible(true);
            }
        }
    }
}
