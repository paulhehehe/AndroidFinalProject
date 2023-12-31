package algonquin.cst2335.androidfinalproject.sun;
/**
 * Name: Yu Song 040873597
 * Course Section: CST2335 022
 * Description: This is the final project for the course CST2335 Mobile Graphical Interface Programming.
 *              This class represents the sun activity, which allows users to search, save, and review
 *              the sunrise sunset information of a certain location (determined by latitude and longitude)
 *              they are interested in. The users can also go to other activities by clicking
 *              the toolbar items.
 * */
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import algonquin.cst2335.androidfinalproject.MainActivity;
import algonquin.cst2335.androidfinalproject.R;
import algonquin.cst2335.androidfinalproject.databinding.ActivitySunBinding;
import algonquin.cst2335.androidfinalproject.databinding.SunRecordBinding;
import algonquin.cst2335.androidfinalproject.dictionary.DictActivity;
import algonquin.cst2335.androidfinalproject.music.MusicActivity;
import algonquin.cst2335.androidfinalproject.recipe.RecipeActivity;

/**
 * The main activity class for the Sun app.
 *
 * This class represents the main screen of the Sun app, where users can view and interact with Sun records.
 * It utilizes a ViewModel to manage data, a RecyclerView to display records, and a DAO for database operations.
 * This activity integrates with api.openweathermap.org for city name and latitude longitude conversion
 * This activity also integrates with api.sunrisesunset.io for sunrise sunset data
 * This activity use Room database
 *
 * @author Yu Song 040873597
 */
public class SunActivity extends AppCompatActivity {

    /**
     * Data binding for the activity layout.
     */
    ActivitySunBinding binding; // for binding
    /**
     * List of Sun records.
     */
    ArrayList<Sun> suns = null; // At the beginning, there are no messages; initialize in SunViewModel.java
    /**
     * ViewModel for managing Sun-related data.
     */
    SunViewModel sunModel; // use a ViewModel to make sure data survive the rotation change
    /**
     * Adapter for the RecyclerView to display Sun records.
     */
    private RecyclerView.Adapter sunAdapter; // to hold the object below
    /**
     * Data Access Object (DAO) for Sun records.
     */
    SunDAO sDAO; // DAO
    /**
     * Index of the selected row in the RecyclerView.
     */
    int selectedRow; // to hold the "position", find which row this is"
    /**
     * Sun object to pass to other classes or methods.
     */
    Sun sToPass; // to hold the "sun" object to pass to other classes or methods
    /**
     * City name input.
     */
    protected String cityName; // to hold the city name input

    /**
     * RequestQueue for Volley library for handling HTTP requests.
     */
    protected RequestQueue queue = null; // for volley

    /**
     * Called when the activity is first created. Initializes UI, sets up listeners,
     * and fetches data from the API and local database.
     *
     * @param savedInstanceState The saved state of the activity, if available.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySunBinding.inflate(getLayoutInflater()); // binding the layout variables
        queue = Volley.newRequestQueue(this); //HTTP Connections: Volley. A Volley object that will connect to a server
        setContentView(binding.getRoot());

        // SharedPreferences for saving the data from last launch
        SharedPreferences prefs = getSharedPreferences("sunSharedData", Context.MODE_PRIVATE);
        binding.latInput.setText(prefs.getString("latitude",""));
        binding.lngInput.setText(prefs.getString("longitude",""));
        binding.editCity.setText(prefs.getString("cityName",""));

        // Set up InputFilter for latitude input validation. Range within the range of -90 to +90, up to 6 decimal places
        InputFilter latitudeFilter = new InputFilter() {
            final Pattern pattern = Pattern.compile("^(-?\\d{0,2}(\\.\\d{0,6})?|\\d{0,1}(\\.\\d{0,6})?|90(\\.0{0,6})?)$");

            /**
             * This method is called to filter user input for longitude.
             *
             * @param source The new sequence being appended.
             * @param start The start index of the source.
             * @param end The end index of the source.
             * @param dest The existing text where the new text is to be placed.
             * @param dstart The start index of the destination.
             * @param dend The end index of the destination.
             * @return The CharSequence that will replace the specified range of dest from dstart to dend.
             *         Return null to accept the input, or an empty string to reject the input.
             */
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String input = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());

                if (!pattern.matcher(input).matches()) {
                    showInvalidInputWarning(getString(R.string.valid_input_lat));
                    Log.d("Latitude input invalid", "Latitude input invalid");
                    return "";
                }

                return null;
            }
        };


        // Set up InputFilter for longitude input validation, Range within the range of -180 to +180, up to 6 decimal places
        InputFilter longitudeFilter = new InputFilter() {
            final Pattern pattern = Pattern.compile("^(-?\\d{0,3}(\\.\\d{0,6})?|\\d{0,2}(\\.\\d{0,6})?|180(\\.0{0,6})?)$");
            /**
             * This method is called to filter user input for longitude.
             *
             * @param source The new sequence being appended.
             * @param start The start index of the source.
             * @param end The end index of the source.
             * @param dest The existing text where the new text is to be placed.
             * @param dstart The start index of the destination.
             * @param dend The end index of the destination.
             * @return The CharSequence that will replace the specified range of dest from dstart to dend.
             *         Return null to accept the input, or an empty string to reject the input.
             */
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                String input = dest.subSequence(0, dstart) + source.toString() + dest.subSequence(dend, dest.length());

                if (!pattern.matcher(input).matches()) {
                    showInvalidInputWarning(getString(R.string.valid_input_lng));
                    Log.d("Longitude input invalid", "Longitude input invalid");
                    return "";
                }

                return null;
            }
        };

        // Apply the InputFilter to the EditText
        binding.latInput.setFilters(new InputFilter[]{latitudeFilter});
        // Apply the InputFilter to the EditText
        binding.lngInput.setFilters(new InputFilter[]{longitudeFilter});

        // onCreateOptionMenu
        setSupportActionBar(binding.sunToolbar);// initialize the toolbar
        getSupportActionBar().setTitle(getString(R.string.sun_toolbar_title));

        // ViewModel for saving the screen when screen config changes
        sunModel = new ViewModelProvider(this).get(SunViewModel.class);
        suns = sunModel.suns.getValue(); //get the array list from ViewModelProvider, might be NULL

        //listener to the MutableLiveData object
        sunModel.selectedSun.observe(this,(selectedSun) ->{
            if(selectedSun != null) {

                FragmentManager fMgr = getSupportFragmentManager();
                // Find fragment if exists
                SunDetailsFragment sunFragment = (SunDetailsFragment)fMgr.findFragmentByTag(SunDetailsFragment.TAG);

                if(sunFragment == null){ //if no existing fragment, create a new one
                    sunFragment = new SunDetailsFragment(selectedSun);
                    FragmentTransaction transaction = fMgr.beginTransaction();
                    transaction.addToBackStack("Add to back stack"); // adds to the history
                    transaction.replace(R.id.sunFragmentLocation, sunFragment,SunDetailsFragment.TAG);//The add() function needs the id of the FrameLayout where it will load the fragment
                    transaction.commit();// This line actually loads the fragment into the specified FrameLayout
                }

            }
        });

        //load sun records from the database:
        SunDatabase db = Room.databaseBuilder(getApplicationContext(),SunDatabase.class, "sunDatabase").build();
        //initialize the variable
        sDAO = db.sunDAO();

        if (suns == null) { // first launch
            sunModel.suns.postValue(suns = new ArrayList<Sun>());

            Executor thread = Executors.newSingleThreadExecutor();
            thread.execute(() ->
            {
                suns.addAll(sDAO.getAllSuns()); //Once you get the data from database
                runOnUiThread(() -> binding.sunRecycleView.setAdapter(sunAdapter)); //Load the RecyclerView
            });
        }

        //listener to the switch object
        binding.switch1.setOnCheckedChangeListener((sw, isChecked) ->{
            if (isChecked) {
                binding.latLngConst.setVisibility(View.VISIBLE);
                binding.cityConst.setVisibility(View.GONE);
                binding.switch1.setTextColor(ContextCompat.getColor(this,R.color.sun_text_gray));
                binding.sunSwitchByLatLng.setTextColor(ContextCompat.getColor(this,R.color.my_primary));
            } else {
                binding.latLngConst.setVisibility(View.GONE);
                binding.cityConst.setVisibility(View.VISIBLE);
                binding.switch1.setTextColor(ContextCompat.getColor(this,R.color.my_primary));
                binding.sunSwitchByLatLng.setTextColor(ContextCompat.getColor(this,R.color.sun_text_gray));
            }
        });

        //listener to the citySearchButton object
        binding.citySearchButton.setOnClickListener(cli->{
            cityName = binding.editCity.getText().toString();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("cityName", cityName);
            editor.apply();

            String cityNameEncode = "0";
            try {
                cityNameEncode = URLEncoder.encode(cityName, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            String urlCity = "https://api.openweathermap.org/data/2.5/weather?q=" + cityNameEncode + "&appid=" + "f5e255b0ecc652c392230100b5230cdb" + "&units=metric";
            //this goes in the button click handler:
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, urlCity, null,
                    (response) -> {
                        try {
                            if (response.has("coord")) {
                                Log.d("City API response", "City API response has coord");
                            }
                        } catch (Exception e) {
                            Log.e("City API response: ", "City API response don't have coord");
                            e.printStackTrace();
                            runOnUiThread(() ->
                                Toast.makeText(SunActivity.this, getString(R.string.sun_sun_api_not_available), Toast.LENGTH_SHORT).show());
                        }

                        try {
                            JSONObject coord = response.getJSONObject("coord"); // Get the "coord" object
                            if (coord.length() == 0) {
                                Toast.makeText(this, getString(R.string.sun_found_nothing), Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("City API response ok", "City API response ok, has coord");
                            }

                            // Extract "lat" and "lon" values
                            double latitude = coord.getDouble("lat");
                            double longitude = coord.getDouble("lon");

                            // bind the lat and lng to the input views
                            binding.latInput.setText(String.valueOf(latitude));
                            binding.lngInput.setText(String.valueOf(longitude));

                            // force click the 2nd button automatically
                            binding.sunSearchButton.performClick();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                        Log.d("City Response", "Received " + response.toString()); //this gets called if the server responded
                    },
                    (error) -> {
                        Log.e("City JsonObjReq Error", "City Sunset JsonObjectRequest Error");
                        if (error.networkResponse != null && (error.networkResponse.statusCode == 404 || error.networkResponse.statusCode == 400)) {
                            // Handle 400 / 404 Internal Server Error
                            Log.e("Volley Error", "Internal Server Error: " + error.networkResponse.statusCode);
                            Toast.makeText(this, getString(R.string.sun_invalid_city), Toast.LENGTH_SHORT).show();
                        } else {
                                // Handle other network errors
                                Log.e("Volley Error", "Network error: " + error.toString());
                                Toast.makeText(this, getString(R.string.sun_try_again), Toast.LENGTH_SHORT).show();
                        }
            }
            );
            queue.add(request);

            //clear the previous text
            binding.latInput.setText("");
            binding.lngInput.setText("");
        });

        //listener to the sunSearchButton object
        binding.sunSearchButton.setOnClickListener( cli ->{

            String sunLatitude = binding.latInput.getText().toString();
            String sunLongitude = binding.lngInput.getText().toString();
            String sunrise = "sunrise";
            String sunset = "sunset";
            String solar_noon = "noon";
            String golden_hour = "golden hour";
            String timezone = "Qingdao";

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("latitude", sunLatitude);
            editor.putString("longitude", sunLongitude);
            editor.apply();

            // Prepare Sunrise & Sunset api url
            // can add try and catch (UnsupportedEncodingException e) here if need encode - URLEncoder.encode(varTextInput, "UTF-8")
//            String url = "https://api.sunrisesunset.io/json?lat=" + sunLatitude + "&lng=" + sunLongitude + "&timezone=UTC&date=today"; // if using UTC
            String url = "https://api.sunrisesunset.io/json?lat=" + sunLatitude + "&lng=" + sunLongitude;
            Log.d("Sunrise Sunset", "Request URL: " + url);

            //this goes in the button click handler:
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    (response) -> {
                        try {
                            if (response.has("results")) {
                                Log.d("Sun API Results", "Sun API Request has results");
                                JSONObject results = response.getJSONObject("results");
                                String status = response.getString("status"); // get the JSONArray associated with "status"
                                Log.d("Sun API status", "Status" + status);
                            } else{
                                Log.e("API response: ", "response don't have results");
                                runOnUiThread(() ->
                                        Toast.makeText(SunActivity.this, getString(R.string.sun_sun_api_not_available), Toast.LENGTH_SHORT).show()
                                );
                            }
                        } catch (JSONException e) {
                            Log.e("API response: ", "response don't have results");
                            e.printStackTrace();
                            runOnUiThread(() ->
                                    Toast.makeText(SunActivity.this, getString(R.string.sun_sun_api_not_available), Toast.LENGTH_SHORT).show()
                            );
                        }

                        try {
                            JSONObject results = response.getJSONObject("results");
                            String status = response.getString("status"); // get the JSONArray associated with "status"

                            if (results.length() == 0) {
                                Toast.makeText(this, getString(R.string.sun_found_nothing), Toast.LENGTH_SHORT).show();
                            } else if (!"OK".equals(status)) {
                                // Status is not OK
                                Log.e("Sun API Status not OK", "The Sun API status is not OK");
                                Toast.makeText(this, getString(R.string.sun_sun_api_status_not_ok), Toast.LENGTH_SHORT).show();
                            } else {
                                // When sunArray and sunStatus both ok:
                                Log.d("Sun API ResultsStatusOK", "Sun API Results and Status OK");

                                // Read the values in the "results" in JSON
                                String sunriseResult = results.getString("sunrise");
                                String sunsetResult = results.getString("sunset");
                                String solar_noonResult = results.getString("solar_noon");
                                String golden_hourResult = results.getString("golden_hour");
                                String timezoneResult = results.getString("timezone");
                                String cityNameFromInput;
                                if(cityName != null) {
                                    cityNameFromInput = cityName;
                                } else {
                                    cityNameFromInput = getResources().getString(R.string.sun_no_name_location);
                                }

                                Sun s = new Sun(sunLatitude, sunLongitude, sunriseResult, sunsetResult, solar_noonResult, golden_hourResult, timezoneResult, cityNameFromInput);
                                sToPass = s; // pass the sun obj to the class level

                                // tell the recycle view that there is new data SetChanged()
                                sunAdapter.notifyDataSetChanged();//redraw the screen

                                FragmentManager fMgr = getSupportFragmentManager();
                                //create a Sun fragment
                                SunDetailsFragment sunFragment = (SunDetailsFragment)fMgr.findFragmentByTag(SunDetailsFragment.TAG);
                                // create a new fragment to display the selected sun details
                                if(sunFragment == null){
                                    sunFragment = new SunDetailsFragment(s);
                                    FragmentTransaction transaction = fMgr.beginTransaction();
                                    transaction.addToBackStack("Add to back stack"); // adds to the history
                                    transaction.replace(R.id.sunFragmentLocation, sunFragment, SunDetailsFragment.TAG);//The add() function needs the id of the FrameLayout where it will load the fragment
                                    transaction.commit();// This line actually loads the fragment into the specified FrameLayout
//
                                }
//                                SunDetailsFragment sunFragment = new SunDetailsFragment(s);
//
//                                FragmentManager fMgr = getSupportFragmentManager();
//                                FragmentTransaction transaction = fMgr.beginTransaction();
//                                transaction.addToBackStack("Add to back stack"); // adds to the history
//                                transaction.replace(R.id.sunFragmentLocation, sunFragment);//The add() function needs the id of the FrameLayout where it will load the fragment
//                                transaction.commit();// This line actually loads the fragment into the specified FrameLayout

                                //clear the previous text
                                binding.latInput.setText("");
                                binding.lngInput.setText("");
                                binding.editCity.setText("");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    },
                    (error) -> {
                        Log.e("Sun JsonObjReq Error", "Sunrise Sunset JsonObjectRequest Error");
                        if (error.networkResponse != null && error.networkResponse.statusCode == 500) {
                            // Handle 500 Internal Server Error
                            Log.e("Volley Error", "Internal Server Error (500)");
                            Toast.makeText(this, getString(R.string.sun_invalid_lat_lng), Toast.LENGTH_SHORT).show();
                        } else {
                            // Handle other network errors
                            Log.e("Volley Error", "Network error: " + error.toString());
                            Toast.makeText(this, getString(R.string.sun_try_again), Toast.LENGTH_SHORT).show();
                        }
                    });
            queue.add(request);

            //clear the previous text
            binding.latInput.setText("");
            binding.lngInput.setText("");
            binding.editCity.setText("");
        });

        // Will draw the recycle view
        binding.sunRecycleView.setAdapter(sunAdapter = new RecyclerView.Adapter<MyRowHolder>() {
            /**
             * Called when RecyclerView needs a new {@link MyRowHolder} of the given type to represent
             * an item.
             *
             * @param parent   The ViewGroup into which the new View will be added after it is bound to
             *                 an adapter position.
             * @param viewType The view type of the new View.
             * @return A new {@link MyRowHolder} that holds a View of the given view type.
             */
            @NonNull
            @Override
            public MyRowHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                SunRecordBinding binding2 = SunRecordBinding.inflate(getLayoutInflater(),parent,false);
                return new MyRowHolder(binding2.getRoot());
            }

            /**
             * Called by RecyclerView to display the data at the specified position. This method updates the
             * contents of the {@link MyRowHolder} to reflect the item at the given position in the dataset.
             *
             * @param holder   The ViewHolder that should be updated to represent the contents of the item
             *                 at the given position in the dataset.
             * @param position The position of the item within the adapter's data set.
             */
            @Override
            public void onBindViewHolder(@NonNull MyRowHolder holder, int position) {
                // Retrieve the Sun object at the specified position
                Sun obj = suns.get(position);

                // Update the views in the ViewHolder with the corresponding data from the Sun object
                holder.sunLatitudeView.setText(obj.getSunLatitude());
                holder.sunLongitudeView.setText(obj.getSunLongitude());
                holder.cityNameView.setText(obj.getCityName().toUpperCase());

//                holder.sunriseView.setText(obj.getSunrise());
//                holder.sunsetView.setText(obj.getSunset());
//                holder.solar_noonView.setText(obj.getSolar_noon());
//                holder.golden_hourView.setText(obj.getGolder_hour());
//                holder.timezoneView.setText(obj.getTimezone());
            }

            /**
             * Gets the total number of items in the suns dataset.
             *
             * @return The total number of items in the dataset.
             */
            @Override
            public int getItemCount() {
                return suns.size();
            }
        });

        // To specify a single column scrolling in a Vertical direction
        binding.sunRecycleView.setLayoutManager(new LinearLayoutManager(this));
    }

    /**
     * ViewHolder class for managing the individual rows in the RecyclerView.
     *
     * This class represents a single row in the RecyclerView, displaying information about a Sun record.
     * It handles click events on the row, triggering actions like updating data from an API and notifying the adapter.
     *
     * @author Yu Song
     */
    public class MyRowHolder extends RecyclerView.ViewHolder {

        /**
         * TextView to display latitude information
         */
        public TextView sunLatitudeView;

        /**
         * TextView to display longitude information.
         */
        public TextView sunLongitudeView;

        /**
         * TextView to display sunrise time information.
         */
        public TextView sunriseView;

        /**
         * TextView to display sunset time information.
         */
        public TextView sunsetView;

        /**
         * TextView to display solar noon time information.
         */
        public TextView solar_noonView;

        /**
         * TextView to display golden hour time information.
         */
        public TextView golden_hourView;

        /**
         * TextView to display timezone information.
         */
        public TextView timezoneView;

        public TextView cityNameView;

        /**
         * Constructor for MyRowHolder.
         *
         * @param theRootConstraintLayout The root layout of the row.
         */
        public MyRowHolder(@NonNull View theRootConstraintLayout){
            super(theRootConstraintLayout);

            // Feature: deleting a message from the RecyclerView
            theRootConstraintLayout.setOnClickListener(clk ->{
                int position = getAbsoluteAdapterPosition();//find which row this is
                Sun selected = suns.get(position);

                // Prepare api url
                // can add try and catch (UnsupportedEncodingException e) here if need encode - URLEncoder.encode(varTextInput, "UTF-8")
//            String url = "https://api.sunrisesunset.io/json?lat=" + sunLatitude + "&lng=" + sunLongitude + "&timezone=UTC&date=today"; // if using UTC
                String url = "https://api.sunrisesunset.io/json?lat=" + selected.getSunLatitude() + "&lng=" + selected.getSunLongitude();
                Log.d("Sunrise Sunset", "Request URL: " + url);

                JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                        (response) -> {
                    try {
                        JSONObject results = response.getJSONObject("results");
                        String status = response.getString("status"); // get the JSONArray associated with "status"
                        if (results.length() == 0) {
//                              Toast.makeText(SunActivity.this, "Found nothing, Array length = 0", Toast.LENGTH_SHORT).show();
                            Toast.makeText(SunActivity.this, getString(R.string.sun_found_nothing), Toast.LENGTH_SHORT).show();
                        } else if (!"OK".equals(status)) {
                            Log.e("Sun API Status not OK", "The Sun API status is not OK");
//                            Toast.makeText(SunActivity.this, "Sunrise sunset API status not OK", Toast.LENGTH_SHORT).show();
                            Toast.makeText(SunActivity.this, getString(R.string.sun_sun_api_status_not_ok), Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("Sun API ResultsStatusOK", "Sun API Results and Status OK");

                            // Read the values in the "results" in JSON
                            String sunriseResult = results.getString("sunrise");
                            String sunsetResult = results.getString("sunset");
                            String solar_noonResult = results.getString("solar_noon");
                            String golden_hourResult = results.getString("golden_hour");
                            String timezoneResult = results.getString("timezone");

                            selected.setSunrise(sunriseResult);
                            selected.setSunset(sunsetResult);
                            selected.setSolar_noon(solar_noonResult);
                            selected.setGolder_hour(golden_hourResult);

                            Executor threadUpdate = Executors.newSingleThreadExecutor();
                            threadUpdate.execute(()->{
                                sDAO.updateSun(selected); //Update selected sun
                            });

                            sunModel.selectedSun.postValue(selected); // Post value to view model and trigger observing fragment generator. will create an extra fragment

                        }sunAdapter.notifyDataSetChanged();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                        }, error -> {                });
                queue.add(request);

                selectedRow = position; // pass position to the whole class scope variable to use in another class
            });

            // theRootConstraintLayout.findViewById
            sunLatitudeView = theRootConstraintLayout.findViewById(R.id.lat_detail);// maybe not needed?
            sunLongitudeView = theRootConstraintLayout.findViewById(R.id.lng_detail);// maybe not needed?
            sunriseView= theRootConstraintLayout.findViewById(R.id.sun_sunrise_detail);
            sunsetView= theRootConstraintLayout.findViewById(R.id.sun_sunset_detail);
            solar_noonView= theRootConstraintLayout.findViewById(R.id.sun_noon_detail);
            golden_hourView= theRootConstraintLayout.findViewById(R.id.sun_golden_detail);
            timezoneView= theRootConstraintLayout.findViewById(R.id.sun_timezone_detail);
            cityNameView = theRootConstraintLayout.findViewById(R.id.sun_city);
        }
    }

    /**
     * Initializes the options menu in the toolbar.
     *
     * @param menu The menu to be inflated.
     * @return True if the menu is created successfully.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        //inflate a menu into the toolbar
        getMenuInflater().inflate(R.menu.sun_menu, menu);
        return true;
    }

    /**
     * Handles item selection in the options menu.
     *
     * @param item The selected menu item.
     * @return True if the item selection is handled successfully.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.favoriteSun: // Go to saved suns
                Intent nextPage = new Intent(SunActivity.this, SunActivity.class);

                startActivity(nextPage);
                //clear the previous text

                break;

            case R.id.saveSun: // Add the selected recipe to suns

                Executor threadS = Executors.newSingleThreadExecutor();
                threadS.execute(() -> {
                    try {
                        Log.d("Sun Save", "try insert existing record");
                        sDAO.insertSun(sToPass);
                        runOnUiThread(() -> {
                            Log.d("Sun Save", "Sun saved successfully");
                            // tell the recycle view that there is new data SetChanged()
                            sunAdapter.notifyDataSetChanged();//redraw the screen
                            Toast.makeText(this, getResources().getString(R.string.sun_save_success), Toast.LENGTH_SHORT).show();
                        });
                    } catch (Exception e) {
                        Log.d("Sun Save", "Exception, sun already in Fav");
                        runOnUiThread(() -> Toast.makeText(SunActivity.this, getString(R.string.sun_save_dupe_record_warning), Toast.LENGTH_SHORT).show());
                    }
                });

                break;

            case R.id.deleteSun:

                //put your Sun deletion code here. If you select this item, you should show the alert dialog
                //asking if the user wants to delete this message

                int position = suns.indexOf(sunModel.selectedSun.getValue());
                if (position != RecyclerView.NO_POSITION) {
                    // temporarily stores the sun location before it is removed from the ArrayList
                    Sun toDelete = suns.get(position);

                    AlertDialog.Builder builder = new AlertDialog.Builder(SunActivity.this);

                    builder.setMessage(getString(R.string.sun_del_warning_text) + " " + getString(R.string.sun_lat_hint) + toDelete.getSunLatitude() + ", " + getString(R.string.sun_lng_hint) + toDelete.getSunLongitude());
                    builder.setTitle(getString(R.string.sun_del_warning_title));

                    builder.setNegativeButton(getString(R.string.sun_no), (btn, obj) -> { /* if no is clicked */ });
                    builder.setPositiveButton(getString(R.string.sun_yes), (btn, obj) -> { /* if yes is clicked */
                        Executor thread = Executors.newSingleThreadExecutor();
                        thread.execute(() -> {
                            //delete from database
                            sDAO.deleteSun(toDelete); //which sun location to delete?
                        });
                        suns.remove(position); //remove from the array list

                        sunAdapter.notifyDataSetChanged(); //redraw the list
                        getSupportFragmentManager().popBackStack(); // go back to message list

                        Snackbar.make(binding.sunRecycleView, getString(R.string.sun_del_after) + (position + 1), Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.sun_undo), click -> {
                                    Executor thread2 = Executors.newSingleThreadExecutor();
                                    thread2.execute(() -> {
                                        sDAO.insertSun(toDelete);
                                    });

                                    suns.add(position, toDelete); // add the toDelete back to ArrayList to undo delete action
                                    sunAdapter.notifyDataSetChanged(); //redraw the list

                                    // after undo, can go back to the fragment
//                                    SunDetailsFragment sunFragment = new SunDetailsFragment(selectedSun);
//
//                                      FragmentManager fMgr = getSupportFragmentManager();
//                                      FragmentTransaction transaction = fMgr.beginTransaction();
//                                      transaction.addToBackStack("Add to back stack"); // adds to the history
//                                      transaction.replace(R.id.sunFragmentLocation, sunFragment);//The add() function needs the id of the FrameLayout where it will load the fragment
//                                      transaction.commit();// This line actually loads the fragment into the specified FrameLayout
                                }).show();
                    });
                    builder.create().show(); //this has to be last
                }
                break;

            case R.id.sunBackToMainItem:
                Intent nextPage1 = new Intent(SunActivity.this, MainActivity.class);
                startActivity(nextPage1);
                break;

            case R.id.sunGotoRecipeItem:
                Intent nextPage2 = new Intent(SunActivity.this, RecipeActivity.class);
                startActivity(nextPage2);
                break;

            case R.id.sunGotoMusicItem:
                Intent nextPage3 = new Intent(SunActivity.this, MusicActivity.class);
                startActivity(nextPage3);
                break;

            case R.id.sunGotoDictItem:
                Intent nextPage4 = new Intent(SunActivity.this, DictActivity.class);
                startActivity(nextPage4);
                break;

            case R.id.sunHelp:
                AlertDialog.Builder builder = new AlertDialog.Builder(SunActivity.this);
                builder.setMessage(getResources().getString(R.string.sun_help2))
                        .setTitle(getResources().getString(R.string.sun_help1))
                        .setPositiveButton("OK", (dialog, cl) -> {
                        })
                        .create().show();
                break;

            case R.id.aboutSun:
                Toast.makeText(this, getString(R.string.sun_about_detail), Toast.LENGTH_LONG).show();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * Displays an AlertDialog to notify the user about invalid input.
     *
     * @param message The message to be displayed in the AlertDialog.
     */
    // Method to show an AlertDialog for invalid input
    protected void showInvalidInputWarning(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.invalid_input_title));
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    /**
     * Called by the system when the device configuration changes while the activity is running.
     *
     * @param newConfig The new device configuration.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        if (!isChangingConfigurations()) {
            // Check the current state of the back stack and pop if needed
            getSupportFragmentManager().popBackStack(SunDetailsFragment.TAG, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

}