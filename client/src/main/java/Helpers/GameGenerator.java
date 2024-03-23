package main.java.Helpers;

import java.util.Date;

public class GameGenerator {

    private int random_int;
    private static boolean gameExists = false;
    private long gameStartTime;

    Date date = new Date();


    public GameGenerator() {
        this.random_int = generateRandomNumber();
        this.gameExists = true;
        gameStartTime = date.getTime();
    }

    public long getGameStartTime() {
        return gameStartTime;
    }

    public long calculateGuessTime() {
        Date currentDate = new Date();
        return currentDate.getTime() - gameStartTime;
    }


    private int generateRandomNumber() {
          int min = 1;
          int max = 50;

          return (int)Math.floor(Math.random() * (max - min + 1) + min);
      }

      public String correctGuess(int guess) {
        if (guess > random_int) {
            return "Too high";
        } else if (guess < random_int){
            return "Too low";
        } else {
            return "Correct!";
        }
      }

      public String guess(int guess){
        if (guess > random_int) {
            return "Too high";
        } else if (guess < random_int) {
            return "Too low";
        } else {
            return "Correct";
        }
      }
      public boolean gameExists() {
          return gameExists;
      }

    public int getRandom_int() {
        return random_int;
    }
}
