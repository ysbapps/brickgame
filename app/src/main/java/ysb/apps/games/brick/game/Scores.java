package ysb.apps.games.brick.game;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

class Scores
{
  private static final int ROWS = 10;
  private static final String FILE_NAME = "scores.dat";
  private static final String LEVEL_FILE_NAME = "max_level.dat";

  private final Context context;
  ArrayList<Integer> scoreTable = new ArrayList<>();
  ArrayList<Byte> levelTable = new ArrayList<>();

  private byte maxAchievedLevel = 1;


  Scores(Context context)
  {
    this.context = context;
    load();
  }

  void addScore(int score, byte level)
  {
    boolean added = false;
    for (int i = 0; i < scoreTable.size(); i++)
      if (score > scoreTable.get(i))    // add in the middle
      {
        scoreTable.add(i, score);
        levelTable.add(i, level);
        if (scoreTable.size() > ROWS)
        {
          scoreTable.remove(scoreTable.size() - 1);
          levelTable.remove(levelTable.size() - 1);
        }
        added = true;
        break;
      }

    if (!added && scoreTable.size() < ROWS)   // add to the end
    {
      scoreTable.add(score);
      levelTable.add(level);
    }

//    scoreTable.clear();   // to clean up scores
//    levelTable.clear();
    save();
  }

  private void load()
  {
    try
    {
      if (new File(context.getFilesDir(), FILE_NAME).exists())
      {
        InputStream is = context.openFileInput(FILE_NAME);
        byte[] ba = new byte[5 * ROWS];
        int length = is.read(ba);
        is.close();
        int i = 0;
        while (i < length)
        {
          scoreTable.add(ByteBuffer.wrap(new byte[]{ba[i++], ba[i++], ba[i++], ba[i++]}).getInt());
          levelTable.add(ba[i++]);
        }
      }

      if (new File(context.getFilesDir(), LEVEL_FILE_NAME).exists())
      {
        InputStream is = context.openFileInput(LEVEL_FILE_NAME);
        byte[] ba = new byte[1];
        //noinspection ResultOfMethodCallIgnored
        is.read(ba);
        is.close();
        maxAchievedLevel = ba[0];
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  private void save()
  {
    try
    {
      FileOutputStream outputStream = context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
      ByteBuffer buffer = ByteBuffer.allocate(5 * scoreTable.size());
      for (int y = 0; y < scoreTable.size(); y++)
      {
        buffer.putInt(scoreTable.get(y));
        buffer.put(levelTable.get(y));
      }

      outputStream.write(buffer.array());
      outputStream.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  void saveMaxLevel(byte level)
  {
    if (level <= maxAchievedLevel)
      return;

    maxAchievedLevel = level;
    try
    {
      FileOutputStream outputStream = context.openFileOutput(LEVEL_FILE_NAME, Context.MODE_PRIVATE);
      ByteBuffer buffer = ByteBuffer.allocate(1);
      buffer.put(maxAchievedLevel);
      outputStream.write(buffer.array());
      outputStream.close();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  byte getMaxAchievedLevel()
  {
    return maxAchievedLevel;
  }
}
