package ysb.apps.games.brick;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import ysb.apps.games.brick.game.DrawView;
import ysb.apps.games.brick.game.Game;
import ysb.apps.utils.logs.L;

public class MainBrickActivity extends AppCompatActivity
{

  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    L.i("Activity created");
    super.onCreate(savedInstanceState);

    // find the retained fragment on activity restarts
    FragmentManager fm = getSupportFragmentManager();
    GameFragment persistentData = (GameFragment) fm.findFragmentByTag("data");
    L.i("persistentData: " + persistentData);
    if (persistentData == null)
    {
      L.i("new game created");
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
    L.i("Activity paused");
    GameFragment persistentData = (GameFragment) getSupportFragmentManager().findFragmentByTag("data");
    if (persistentData != null && persistentData.getGame() != null)
      persistentData.getGame().pause();

    super.onPause();
  }

  @Override
  public void onDestroy()
  {
    FragmentManager fm = getSupportFragmentManager();
    GameFragment persistentData = (GameFragment) fm.findFragmentByTag("data");
    if (persistentData != null)
      persistentData.getGame().release();

    L.i("Activity destroyed");
    super.onDestroy();
  }

}
