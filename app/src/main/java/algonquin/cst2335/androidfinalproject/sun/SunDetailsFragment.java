package algonquin.cst2335.androidfinalproject.sun;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import algonquin.cst2335.androidfinalproject.databinding.SunDetailsLayoutBinding;

public class SunDetailsFragment extends Fragment {
    Sun selected;

    public SunDetailsFragment(){}
    //constructor for your class which takes a Sun object that it will use as a data source for the TextViews:
    public SunDetailsFragment(Sun toDisplay){
        selected = toDisplay;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        //inflate an XML layout for this Fragment
        SunDetailsLayoutBinding binding = SunDetailsLayoutBinding.inflate(getLayoutInflater());

        // set the latitude and longitude views:
        binding.sunLatDetail.setText(selected.sunLatitude);
        binding.sunLngDetail.setText(selected.sunLongitude);

        //set the text views:
        binding.sunSunriseDetail.setText(selected.sunrise);
        binding.sunNoonDetail.setText(selected.solar_noon);
        binding.sunGoldenDetail.setText(selected.golder_hour);
        binding.sunSunsetDetail.setText(selected.sunset);
        binding.sunTimezoneDetail.setText(selected.timezone);

        // Get the current date and time
        Date currentDate = Calendar.getInstance().getTime();

        // Format the date
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd-MMM-yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(currentDate);

        // Format the time
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
        String formattedTime = timeFormat.format(currentDate);

        // Combine date and time
        String dateTimeString = formattedDate + " " + formattedTime;
        //set the text views:
        binding.sunDateDetailTitle.setText(formattedDate);
        binding.sunTimeDetailTitle.setText(formattedTime);

        return binding.getRoot();
    }

    public class DateTimeGenerator {

        public void main(String[] args) {
            // Get the current date and time
            Date currentDate = Calendar.getInstance().getTime();

            // Format the date
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd-MMM-yyyy", Locale.getDefault());
            String formattedDate = dateFormat.format(currentDate);

            // Format the time
            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm:ss a", Locale.getDefault());
            String formattedTime = timeFormat.format(currentDate);

            // Combine date and time
            String dateTimeString = formattedDate + " " + formattedTime;

            // Print the result
            System.out.println("Formatted Date: " + formattedDate);
            System.out.println("Formatted Time: " + formattedTime);
            System.out.println("Combined Date and Time: " + dateTimeString);
        }
    }
}