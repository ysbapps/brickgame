package ysb.apps.games.brick.game;

import android.content.res.AssetManager;

import java.io.InputStream;

class Cup
{
  final static int W = 10, H = 20;    // cup width and height

  private final AssetManager am;
  int[][] contents = new int[H][W];
  private int[][] mergeContents;


  Cup(AssetManager am)
  {
    this.am = am;
  }

  boolean isFigurePositionValid(Figure figure)
  {
    for (int y = 0; y < Figure.SIZE; y++)
      for (int x = 0; x < Figure.SIZE; x++)
      {
        if (!figure.getCurrContents()[y][x])
          continue;

        int cx = figure.pos.x + x;
        int cy = figure.pos.y + y;
        if (cx < 0 || cx >= W)
          return false;

        if (cy >= H)
          return false;

        if (cy < 0)
          continue;

        if (contents[cy][cx] > 0)
          return false;
      }

    return true;
  }

  void appendFigure(Figure figure)
  {
    for (int y = 0; y < Figure.SIZE; y++)
      for (int x = 0; x < Figure.SIZE; x++)
      {
        if (!figure.getCurrContents()[y][x])
          continue;

        int cx = figure.pos.x + x;
        int cy = figure.pos.y + y;
        if (cx < 0 || cx >= W || cy < 0 || cy >= H)
          continue;

        contents[cy][cx] = 1;
      }
  }

  int premerge()
  {
    mergeContents = new int[H][W];
    int mergedRows = 0;
    int targetRow = H - 1;
    for (int sourceRow = H - 1; sourceRow >= 0; sourceRow--)
    {
      boolean rowComplete = true;
      for (int x = 0; x < W; x++)
      {
        mergeContents[targetRow][x] = contents[sourceRow][x];
        if (contents[sourceRow][x] == 0)
          rowComplete = false;
      }

      if (rowComplete)    // if row is complete we don't decrease targetRow and thus it will be overwritten on the next loop cycle by the next row
        mergedRows++;
      else
        targetRow--;
    }

    for (int y = targetRow; y >= 0; y--)
      for (int x = 0; x < W; x++)
        mergeContents[targetRow][x] = 0;

    return mergedRows;
  }

  boolean isRowComplete(int row)
  {
    for (int x = 0; x < W; x++)
      if (contents[row][x] == 0)
        return false;

    return true;
  }

  void merge()
  {
    contents = mergeContents;
  }

  boolean isLevelComplete()
  {
    for (int y = 0; y < H; y++)
      for (int x = 0; x < W; x++)
        if (contents[y][x] > 1)
          return false;

    return true;
  }

  boolean loadLevel(byte level)
  {
    try
    {
      contents = new int[H][W];
      String fileName = level + ".dat";
      System.out.println("loading level: " + fileName);
      InputStream is = am.open(fileName);
      byte[] ba = new byte[W * H];
      //noinspection ResultOfMethodCallIgnored
      is.read(ba);
      is.close();
      int i = 0;
      for (int y = 0; y < H; y++)
        for (int x = 0; x < W; x++)
          contents[y][x] = ba[i++];

      return true;
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return false;
  }

}
