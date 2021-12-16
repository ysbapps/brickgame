package ysb.apps.games.brick.game;

import android.content.Context;
import android.media.MediaPlayer;

import java.util.HashMap;

import ysb.apps.utils.logs.L;

public class SndManager
{
  private final HashMap<Integer, MediaPlayer> players = new HashMap<>();
  private final Context context;
  public boolean soundsOn = true;


  SndManager(Context context)
  {
    this.context = context;
  }

  MediaPlayer addSound(int id)
  {
    MediaPlayer mp = MediaPlayer.create(context, id);
    players.put(id, mp);
    L.i("sound added: " + id);

    return mp;
  }

  void play(int id)
  {
    if (soundsOn)
    {
      MediaPlayer mp = players.get(id);
      if (mp == null)
        mp = addSound(id);

      mp.start();
    }
  }

  void release()
  {
    L.i("releasing sound resources.. size: " + players.size());
    for (MediaPlayer mp : players.values())
    {
      mp.stop();
      mp.release();
    }
  }

}