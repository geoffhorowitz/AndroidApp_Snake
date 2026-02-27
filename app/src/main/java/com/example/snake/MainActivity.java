package com.example.snake;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Button;
import android.view.View;
import android.app.Activity;

public class MainActivity extends Activity {

    // variable inits
    SeekBar speed_slider_obj;
    int speed_set_val = 1;

    Button start_game_button_obj;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get speed slider object
        speed_slider_obj = findViewById(R.id.speed_slider);

        // set up callbacks for the speed slider
        speed_slider_obj.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int val, boolean b) {
                speed_set_val = val;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // This method will automatically get called when the user starts touching the SeekBar
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // This method will automatically get called when the user stops touching the SeekBar
            }
        });

        // get start game button object
        // // Ref: https://stackoverflow.com/a/41389737 - since I want to pass in additional data (speed), using direct callback from button is insufficient
        start_game_button_obj = findViewById(R.id.start_game_button);

        // if statement is just to ensure an object was retrieved (robustness!)
        if (start_game_button_obj != null)
        {
            // set up callback for button click
            start_game_button_obj.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //activate Snake Activity
                    Intent intent = new Intent(view.getContext(), SnakeActivity.class);
                    intent.putExtra("speed", Math.max(1, speed_set_val));
                    view.getContext().startActivity(intent);
                }
            });
        }
    }
}