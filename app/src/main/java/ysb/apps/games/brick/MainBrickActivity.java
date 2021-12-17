package ysb.apps.games.brick;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

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

    L.i("viewModel.game: " + Game.game);
    if (Game.game == null)
    {
      Game.game = new Game(this);
      L.i("new game created");
    }

    DrawView view = new DrawView(this);
    Game.game.setView(view);
    setContentView(view);
    Game.game.updateProducts();
  }

  @Override
  public void onPause()
  {
    L.i("Activity paused");
    L.i("viewModel.game: " + Game.game);
    if (Game.game != null)
      Game.game.pause();

    super.onPause();
  }

  @Override
  public void onDestroy()
  {
    L.i("Activity destroyed");
    L.i("viewModel.game: " + Game.game);
    if (Game.game != null)
      Game.game.release();

    super.onDestroy();
  }

}
