package ysb.games.brick;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import ysb.games.brick.game.Game;

public class GameFragment extends Fragment
{
  private Game game;


  // this method is only called once for this fragment
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    // retain this fragment
    setRetainInstance(true);
  }

  public Game getGame()
  {
    return game;
  }

  public void setGame(Game game)
  {
    this.game = game;
  }
}
