package com.example.opower_rps;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Random;

//
// This activity is the main game activity and contains all the 
// game mechanics. It determines a winner for each round and if
// applicable a winner for the game.
//
public class Game extends Activity implements OnClickListener {

	//
	// Constants for Difficulty, and Game Type
	//
	public static final String KEY_DIFFICULTY 	= "com.example.opower_rps.difficulty";
	public static final String KEY_TYPE 		= "com.example.opower_rps.type";
	public static final String KEY_NUM 			= "com.example.opower_rps.num";
	public static final int DIFFICULTY_NORMAL 	= 0;
	public static final int DIFFICULTY_HARD 	= 1;
	public static final int DIFFICULTY_CONTINUE = -1;
	public static final int SERIES	 			= 3;
	public static final int INFINITE			= 4;
	private static final String TAG 			= "RPS";
	private static final String ROUND_NUMBER	= "round";
	private static final String PLAYER_SCORE 	= "player_score";
	private static final String COMPUTER_SCORE 	= "computer_score";
	private static final String SERIES_CAP 		= "series_cap";
	private static final String GAME_MODE		= "game_mode";
	
	//
	// Selection definitions
	//
	private static final int ROCK				= 20;
	private static final int PAPER				= 21;
	private static final int SCISSORS			= 22;
	private static final int weapon_type[] 		= {ROCK, PAPER, SCISSORS};
	
	// Random generator for computer selection
	Random computerRandom = new Random();

	//
	// Holders for game mechanisms and score
	//
	private static int best_of_cap 			= 1;
	private static int playerScore 			= 0;
	private static int computerScore 		= 0;
	private static int roundNumber 			= 0;
	private static int gameType				= INFINITE;
	private static int computerSelection 	= ROCK;
	private static int playerSelection		= ROCK;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "Game onCreate");

		// Grab the difficulty
		int difficulty = getIntent().getIntExtra(KEY_DIFFICULTY, DIFFICULTY_NORMAL);
		
		// Get the other game variables
		getGameVariables(difficulty);
		
		// Logs for debugging
		Log.d(TAG, "difficulty = " + difficulty);
		Log.d(TAG, "gameType = " + gameType);
		Log.d(TAG, "best_of_cap = " + best_of_cap);
		Log.d(TAG, "round number = " + roundNumber);
		Log.d(TAG, "playerScore = " + playerScore + " computerScore = " + computerScore);

		// Set the view
		setContentView(R.layout.game);
		
		// Connect the buttons
		View beginButton = findViewById(R.id.begin_button);
		beginButton.setOnClickListener(this);
		View rockButton = findViewById(R.id.player_rock_button);
		rockButton.setOnClickListener(this);
		View paperButton = findViewById(R.id.player_paper_button);
		paperButton.setOnClickListener(this);
		View scissorsButton = findViewById(R.id.player_scissors_button);
		scissorsButton.setOnClickListener(this);
		
		// Disable the user buttons for now
		enabledisableButtons(false);
		
		// Set text in the view
		updateScoreboard();
		TextView game_type_view = (TextView)findViewById(R.id.game_type_textview);
		game_type_view.setText(gameType == SERIES ? "Series: First to " + best_of_cap : "Total Score");

		// If the activity is restarted, do a continue next time
		getIntent().putExtra(KEY_DIFFICULTY, DIFFICULTY_CONTINUE);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(TAG, "onDestroy clearing variables");
		// Reset the game variables
		best_of_cap = 1;
		playerScore = 0;
		computerScore = 0;
		roundNumber = 0;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		Log.d(TAG, "onPause saving game state");
		// Save the game variables
		getPreferences(MODE_PRIVATE).edit().putInt(ROUND_NUMBER, roundNumber).commit();
		getPreferences(MODE_PRIVATE).edit().putInt(PLAYER_SCORE, playerScore).commit();
		getPreferences(MODE_PRIVATE).edit().putInt(COMPUTER_SCORE, computerScore).commit();
		getPreferences(MODE_PRIVATE).edit().putInt(SERIES_CAP, best_of_cap).commit();
		getPreferences(MODE_PRIVATE).edit().putInt(GAME_MODE, gameType).commit();
	}

	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.begin_button:
			Log.d(TAG, "clicked on begin button");
			highlightButtons(playerSelection, computerSelection, true);
			int done = isGameDone();
			if(done != 0) {
				popRoundDialog(0, done, ROCK, ROCK);
			} else {
				openBeginToast();
			}	
			break;
		case R.id.player_rock_button:
			Log.d(TAG, "clicked on ROCK button");
			playRound(0);
			break;
		case R.id.player_paper_button:
			Log.d(TAG, "clicked on PAPER button");
			playRound(1);
			break;
		case R.id.player_scissors_button:
			Log.d(TAG, "clicked on SCISSORS button");
			playRound(2);
		default:
			break;
		}
	}
	
	//
	// Function that sets the game variables. Useful for the continue game
	// so we can resume game state.
	//
	private void getGameVariables(int difficulty) {
		Log.d(TAG, "getGameVariables");
		
		// Continuing a game
		if(difficulty == DIFFICULTY_CONTINUE) {
			gameType = getPreferences(MODE_PRIVATE).getInt(GAME_MODE, SERIES);
			roundNumber = getPreferences(MODE_PRIVATE).getInt(ROUND_NUMBER, 1);
			playerScore = getPreferences(MODE_PRIVATE).getInt(PLAYER_SCORE, 1);
			computerScore = getPreferences(MODE_PRIVATE).getInt(COMPUTER_SCORE, 1);
			best_of_cap = getPreferences(MODE_PRIVATE).getInt(SERIES_CAP, 2);
		}
		// Starting a fresh game
		else {
			gameType = getIntent().getIntExtra(KEY_TYPE, SERIES);

			// Set the series limit if applicable
			if(gameType == SERIES) {
				best_of_cap = getIntent().getIntExtra(KEY_NUM, 3);
			}
		}
	}
	
	//
	// Show a pop up and count down until the round begins and buttons are enabled
	//
	private void openBeginToast() {
		
		Log.d(TAG, "beginToast");
		// Show a Toast with a count down for the player's choice
		new CountDownTimer(4000, 1500) {
			public void onTick(long msUntilDone) {
				Context context = getApplicationContext();
				Log.d(TAG, "msUntilDone = " + msUntilDone);
				String toast_text = msUntilDone > 3000 ? "Ready?" : "Shoot!";
				Toast toast = Toast.makeText(context, toast_text, Toast.LENGTH_SHORT);
				toast.show();
			}
			
			public void onFinish() {
				enabledisableButtons(true);
			}
		}.start();
	}
	
	//
	// Function to enable the buttons as clickable after the round has begun
	//
	private void enabledisableButtons(boolean enable) {
		View rockButton = findViewById(R.id.player_rock_button);
		View paperButton = findViewById(R.id.player_paper_button);
		View scissorsButton = findViewById(R.id.player_scissors_button);
		rockButton.setEnabled(enable);
		paperButton.setEnabled(enable);
		scissorsButton.setEnabled(enable);
	}

	//
	// Function to play the round. Takes player's selection, makes the computer's
	// selection and determines winner for the round. It then increments the score
	// for the winner, and checks to see if the game is over.
	//
	private void playRound(int selection) {
		Log.d(TAG, "player selected = " + selection);
		playerSelection = weapon_type[selection];
		Log.d(TAG, "player selected = " + playerSelection);
		
		// Randomly select for the computer
		computerSelection = weapon_type[computerRandom.nextInt(3)];
		Log.d(TAG, "computer selected = " + computerSelection);
		// Highlight the computer selection
		highlightButtons(playerSelection, computerSelection, false);
		
		// Determine the winner for the round
		int winner = determineWinner(playerSelection, computerSelection);
		roundNumber++;
		
		if(winner == 0) {
			// Tie! Pop a dialog and do nothing
		} else if(winner == 1) {
			// Player Win! Increment player score
			playerScore++;
		} else if(winner == -1) {
			// Computer Win! Increment computer score
			computerScore++;
		}
		Log.d(TAG, "winner = " + winner);
		Log.d(TAG, "playerScore = " + playerScore + " computerScore = " + computerScore);
		
		// Update the score in the view
		updateScoreboard();
		
		// disable buttons
		enabledisableButtons(false);
		
		// Check if the game is over
		// Show a dialog with the round or game result
		popRoundDialog(winner, isGameDone(), playerSelection, computerSelection);
	}
	
	//
	// Function to determine the winner of the round
	//
	private int determineWinner(int player_selection, int computer_selection) {
		if(player_selection == computer_selection) {
			// Tie
			return 0;
		} else if(player_selection == ROCK && computer_selection == SCISSORS) {
			// Player win
			return 1;
		} else if(player_selection == SCISSORS && computer_selection == PAPER) {
			// Player win
			return 1;
		} else if(player_selection == PAPER && computer_selection == ROCK) {
			// Player win
			return 1;
		} else {
			// Computer win
			return -1;
		}
	}
	
	//
	// Function to determine if the game is over
	//
	private int isGameDone() {
		
		// Game can only end if it's a series
		if(gameType == SERIES) {
			// Player Won
			if(playerScore == best_of_cap) {
				return 1;
			} 
			// Computer Won
			else if(computerScore == best_of_cap) {
				return -1;
			} 
			// Game still going
			else {
				return 0;
			}
		} else {
			return 0;
		}
		
	}
	
	//
	// Function to pop up a dialog depending on the outcome of the round. A different pop up occurs
	// for Win, Tie, and Lose. If the game is over a separate pop up appears telling the user if
	// they won or lost the game.
	//
	private void popRoundDialog(int winner, int game_over, int player_selection, int computer_selection) {
		
		LayoutInflater inflater = this.getLayoutInflater();
		Log.d(TAG, "popRoundDialog: player_selection = " + player_selection + " computer_selection = " + computer_selection);
		
		// Game isn't over
		if(game_over == 0) {
			// Tie Round
			if(winner == 0) {
				new AlertDialog.Builder(this)
				.setView(inflater.inflate(R.layout.tie, null))
				.show();
			}
			// Win Round
			else if(winner == 1) {
				new AlertDialog.Builder(this)
				.setView(inflater.inflate(R.layout.win_round, null))
				.show();
			}
			// Lose Round
			else{
				new AlertDialog.Builder(this)
				.setView(inflater.inflate(R.layout.lose_round, null))
				.show();
			}
		}
		// Game is over
		else {
			// Player won the game
			if(game_over == 1) {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				View wingameView = inflater.inflate(R.layout.win_game, null);
				alert.setView(wingameView);
				alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// End the game
						finish();
					}
				});
				//alert.create();
				alert.show();
			}
			// Computer won the game
			else {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				View losegameView = inflater.inflate(R.layout.lose_game, null);
				alert.setView(losegameView);
				alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// End the game
						finish();
					}
				});
				alert.show();
			}
		}
	}
	
	//
	// Function to update the scoreboard in the view after each round
	//
	private void updateScoreboard() {
		Log.d(TAG, "Updating scoreboard");
		TextView round_view = (TextView)findViewById(R.id.round_num_textview);
		round_view.setText(Integer.toString(roundNumber));
		TextView player_score = (TextView)findViewById(R.id.player_score_number);
		player_score.setText(Integer.toString(playerScore));
		TextView computer_score = (TextView)findViewById(R.id.computer_score_number);
		computer_score.setText(Integer.toString(computerScore));
	}
	
	//
	// Function to highlight the computer selection on the buttons
	//
	private void highlightButtons(int player_selection, int computer_selection, boolean clear) {

		// Highlight the computer button
		View compRockButton = findViewById(R.id.rock_button);
		View compPaperButton = findViewById(R.id.paper_button);
		View compScissorsButton = findViewById(R.id.scissors_button);
		
		if(computer_selection == ROCK) {
			if(clear) {
				compRockButton.setBackgroundResource(0);
			} else {
				compRockButton.setBackgroundColor(getResources().getColor(R.color.red));
			}
		} else if(computer_selection == PAPER) {
			if(clear) {
				compPaperButton.setBackgroundResource(0);
			} else {
				compPaperButton.setBackgroundColor(getResources().getColor(R.color.red));
			}
		} else {
			if(clear) {
				compScissorsButton.setBackgroundResource(0);
			} else {
				compScissorsButton.setBackgroundColor(getResources().getColor(R.color.red));
			}
		}
		
		// Highlight the player button
		View playerRockButton = findViewById(R.id.player_rock_button);
		View playerPaperButton = findViewById(R.id.player_paper_button);
		View playerScissorsButton = findViewById(R.id.player_scissors_button);
		
		if(player_selection == ROCK) {
			if(clear) {
				playerRockButton.setBackgroundResource(android.R.drawable.btn_default);
			} else {
				playerRockButton.setBackgroundColor(getResources().getColor(R.color.red));
			}
		} else if(player_selection == PAPER) {
			if(clear) {
				playerPaperButton.setBackgroundResource(android.R.drawable.btn_default);
			} else {
				playerPaperButton.setBackgroundColor(getResources().getColor(R.color.red));
			}
		} else {
			if(clear) {
				playerScissorsButton.setBackgroundResource(android.R.drawable.btn_default);
			} else {
				playerScissorsButton.setBackgroundColor(getResources().getColor(R.color.red));
			}
		}
	}
}