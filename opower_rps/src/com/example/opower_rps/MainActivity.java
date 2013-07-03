package com.example.opower_rps;

import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;

//
//This is the main activity for a Rock-Paper-Scissors game
//From here the user selects whether they want to a "New Game"
//or to "Continue Game". From there they select options for the
//game. The user can also select a game "About" which provides basic
//information about the game. Lastly they can exit.
//
public class MainActivity extends Activity implements OnClickListener {

	private static final String TAG = "RPS";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		//
		// Set up listeners for the button
		//
		// New Game Button
		View newButton = findViewById(R.id.new_game_button);
		newButton.setOnClickListener(this);

		// Continue Button
		View continueButton = findViewById(R.id.continue_game_button);
		continueButton.setOnClickListener(this);

		// About Button
		View aboutButton = findViewById(R.id.about_button);
		aboutButton.setOnClickListener(this);

		// Exit Button
		View exitButton = findViewById(R.id.exit_button);
		exitButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.new_game_button:
			//openNewGameDialog();
			openGameTypeDialog(Game.DIFFICULTY_NORMAL);
			break;
		case R.id.continue_game_button:
			Log.d(TAG, "clicked on continue game");
			startGame(Game.DIFFICULTY_CONTINUE, 1);
			break;
		case R.id.about_button:
			Log.d(TAG, "clicked on about");
			Intent intent = new Intent(this, About.class);
			startActivity(intent);
			break;
		case R.id.exit_button:
			Log.d(TAG, "clicked on exit");
			finish();
			break;
		}
	}

	/*private void openNewGameDialog() {
		Log.d(TAG, "clicked on new game");
		new AlertDialog.Builder(this)
		.setTitle(R.string.new_game_title)
		.setItems(R.array.difficulty, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int difficulty) {
				openGameTypeDialog(difficulty);
			}
		})
		.show();
	}*/
	
	private void openGameTypeDialog(final int difficulty) {
		Log.d(TAG, "Game Type selection");
		new AlertDialog.Builder(this)
		.setTitle(R.string.game_type_title)
		.setItems(R.array.game_types, new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int game_type) {
				startGame(difficulty, game_type);
			}
		})
		.show();
	}

	private void startGame(final int difficulty, final int game_type) {

		Log.d(TAG, "game_type = " + game_type);
		// Store our variables
		final Intent intent = new Intent(this, Game.class);
		intent.putExtra(Game.KEY_DIFFICULTY, difficulty);
		intent.putExtra(Game.KEY_TYPE, game_type == 0 ? Game.SERIES : Game.INFINITE);
		
		final LayoutInflater inflater = this.getLayoutInflater();
		
		// If the user selected Series as the Game Type, prompt for the series win total cap
		if(game_type == 0) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(R.string.series_cap_title);
			View view = inflater.inflate(R.layout.series_cap_alert, null);
			alert.setView(view);
			final EditText series_cap = (EditText)view.findViewById(R.id.series_cap_content);
			alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Log.d(TAG, "series cap = " + series_cap.getText().toString());
					int cap = Integer.parseInt(series_cap.getText().toString());
					// Error check the series cap, store, and start the game
					if(cap > 0 && cap < 100) {
						intent.putExtra(Game.KEY_NUM, cap);
						startActivity(intent);
					} else {
						// Show an dialog with an error message
						showErrorDialog();
					}
				}
			});
			alert.show();
		} else {
			// Game type is infinite so just start the game
			startActivity(intent);
		}
	}
	
	//
	// Function to pop up a dialog for series cap error
	//
	private void showErrorDialog() {
		new AlertDialog.Builder(this)
		.setTitle(R.string.error_title)
		.setMessage(R.string.series_error)
		.show();
	}
}
