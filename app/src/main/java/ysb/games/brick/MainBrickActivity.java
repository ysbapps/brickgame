package ysb.games.brick;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import ysb.games.brick.game.DrawView;
import ysb.games.brick.game.Game;

public class MainBrickActivity extends AppCompatActivity
{

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    System.out.println("Activity created");
    super.onCreate(savedInstanceState);

    // find the retained fragment on activity restarts
    FragmentManager fm = getSupportFragmentManager();
    GameFragment persistentData = (GameFragment) fm.findFragmentByTag("data");
    System.out.println("persistentData: " + persistentData);
    if (persistentData == null)
    {
      System.out.println("--------------------------------------------------------------------------");
      persistentData = new GameFragment();
      fm.beginTransaction().add(persistentData, "data").commit();
      Game game = new Game(this);
      persistentData.setGame(game);
    }

    DrawView view = new DrawView(this);
    persistentData.getGame().setView(view);
    setContentView(view);
  }

  @Override
  public void onPause()
  {
    System.out.println("Activity paused");
    GameFragment persistentData = (GameFragment) getSupportFragmentManager().findFragmentByTag("data");
    if (persistentData != null && persistentData.getGame() != null)
      persistentData.getGame().pause();

    super.onPause();
  }

  @Override
  public void onDestroy()
  {
    System.out.println("Activity destroyed");
    super.onDestroy();
  }

}
