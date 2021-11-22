package ysb.games.brick.game;

import java.util.HashSet;

import ysb.games.brick.MainBrickActivity;

public class Game extends Thread
{
  final static int STATE_NOT_STARTED = 0;
  final static int STATE_GAME = 1;
  final static int STATE_PAUSED = 2;
  final static int STATE_DROPPING = 3;
  final static int STATE_GAME_OVER = 4;

  final static int MOVE_LEFT = 1;
  final static int MOVE_RIGHT = 2;
  final static int ROTATE = 3;
  final static int DROP = 4;

  final Cup cup;
  private DrawView view = null;
  int state = STATE_NOT_STARTED;
  Figure currentFigure;
  Figure nextFigure;
  private long levelStarted;
  private long lastActionTime;
  private long wasOnPause;
  byte level;
  int score;
  int prize;
  int prizeCycle;
  String message;

  private int figureCount = 0;
  private long figureStartTime;
  private HashSet<Integer> figureActions = new HashSet<>();

  Scores scores;

  boolean isAlive = true;


  public Game(MainBrickActivity activity)
  {
    super();

    cup = new Cup(activity.getAssets());
    scores = new Scores(activity.getApplicationContext());
    start();
  }

  public void setView(DrawView view)
  {
    this.view = view;
    this.view.game = this;
  }

  private void repaint()
  {
    if (view != null)
      view.postInvalidate();
  }

  void newGame()
  {
    if (state != STATE_NOT_STARTED)
      return;

    levelStarted = System.currentTimeMillis();
    level = 1;
    score = 0;
    prize = 0;
    message = null;
    cup.loadLevel(level);
    currentFigure = new Figure();
    nextFigure = new Figure();
    figureStartTime = lastActionTime = System.currentTimeMillis();
    wasOnPause = 0;

    state = STATE_GAME;
  }

  public void run()
  {
    System.out.println("Game is started.");
    try
    {
      while (isAlive)
      {
        repaint();
        if (state != STATE_GAME && state != STATE_DROPPING)
        {
          sleepMs(100);
          continue;
        }

        if (state == STATE_GAME)
        {
          int delay = 800 - speed() * 50;    // 750 -> 300 (at speed 10) -> 0 (at speed 16)
          while (delay-- > 0 && state == STATE_GAME && isAlive)
          {
            sleepMs(1);
            if (delay == 100 * (int) (delay / 100.0))
              repaint();
          }
        }
        else if (state == STATE_DROPPING)
          sleepMs(10);
        else
          throw new Exception("Invalid state");

        currentFigure.pos.y++;
        if (cup.isFigurePositionValid(currentFigure))      // end of normal loop (figure is falling or dropping)
          continue;

        currentFigure.pos.y--;    // figure has reached a bottom shape - appending to the cup
        currentFigure.movable = false;
        cup.appendFigure(currentFigure);
        if (state == STATE_DROPPING)
          state = STATE_GAME;

        repaint();
        sleepMs(100);

        int mergedRows = cup.premerge();
        if (mergedRows > 0)
          merge(mergedRows);

        if (cup.isLevelComplete())
          nextLevel();

        currentFigure = nextFigure;
        figureCount++;
        figureActions = new HashSet<>();
        figureStartTime = lastActionTime = System.currentTimeMillis();
        if (!cup.isFigurePositionValid(currentFigure))
        {
          finishGame();
          repaint();
        }

        nextFigure = new Figure();
        repaint();
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    System.out.println("Game is stopped.");
  }

  public void pause()
  {
    if (state != STATE_GAME)
      return;

    state = STATE_PAUSED;
    message = "PAUSED";
    lastActionTime = System.currentTimeMillis();
    repaint();
  }

  public void resumeFromPause()
  {
    state = STATE_GAME;
    message = null;
    wasOnPause += (System.currentTimeMillis() - lastActionTime);
    lastActionTime = System.currentTimeMillis();
    repaint();
  }

  void quitToStartPage()
  {
    if (state != STATE_PAUSED && state != STATE_GAME_OVER)
      return;

    if (state == STATE_PAUSED)
      finishGame();

    state = STATE_NOT_STARTED;
    message = null;

    repaint();
  }

  private void merge(int mergedRows)
  {
    prize = Math.round(level * speed() * mergedRows * mergedRows);
    for (prizeCycle = 0; prizeCycle < 200; prizeCycle++)
    {
      repaint();
      sleepMs(3);
    }
    score += prize;
    prize = 0;
    cup.merge();
    currentFigure = null;
    repaint();
  }

  private void finishGame()
  {
    state = STATE_GAME_OVER;
    message = "GAME OVER";
    if (score > 0)
      scores.addScore(score, level);
  }

  void action(int action)
  {
    if (state != STATE_GAME || currentFigure == null || !currentFigure.movable)
      return;

    switch (action)
    {
      case MOVE_LEFT:
        currentFigure.pos.x--;
        if (!cup.isFigurePositionValid(currentFigure))
          currentFigure.pos.x++;
        break;
      case MOVE_RIGHT:
        currentFigure.pos.x++;
        if (!cup.isFigurePositionValid(currentFigure))
          currentFigure.pos.x--;
        break;
      case ROTATE:
        currentFigure.rotate();
        if (!cup.isFigurePositionValid(currentFigure))
          currentFigure.rotateBack();
        break;
      case DROP:
        state = STATE_DROPPING;
        currentFigure.movable = false;
        break;
    }

    figureActions.add(action);
    lastActionTime = System.currentTimeMillis();
    repaint();
  }

  HashSet<Integer> needHelp()
  {
    HashSet<Integer> actions = new HashSet<>();
    if (level == 1 && figureCount < 10 && (time() - figureStartTime) / 1000 > figureCount)
    {
      if (!figureActions.contains(MOVE_LEFT) && !figureActions.contains(MOVE_RIGHT))
      {
        actions.add(MOVE_LEFT);
        actions.add(MOVE_RIGHT);
      }
      if (!figureActions.contains(ROTATE))
        actions.add(ROTATE);
      if (!figureActions.contains(DROP) && currentFigure.pos.y > Cup.H / 2)
        actions.add(DROP);
    }

    return actions;
  }

  private void nextLevel()
  {
    currentFigure = null;
    figureCount = 0;
    repaint();
    sleepMs(500);
    prize = 10 * level * level;
    message = "Bonus  +" + prize;
    repaint();
    sleepMs(1500);
    score += prize;
    prize = 0;
    message = null;
    repaint();
    sleepMs(500);
    level++;
    if (cup.loadLevel(level))
      message = "Level " + level;
    else
    {

//      state = STATE_NO_MORE_LEVELS;
//              tetris.updateScores("", score, true);
    }
    repaint();
    sleepMs(2000);
    message = null;
    levelStarted = System.currentTimeMillis();
    wasOnPause = 0;
  }

  private void sleepMs(long ms)
  {
    try
    {
      int stateBeforeSleep = state;
      while (ms-- > 0 && isAlive && state == stateBeforeSleep)
      {
        sleep(1);
        if (state == STATE_PAUSED)
          ms++;
      }

    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }
  }

  long time()
  {
    if (state != STATE_GAME)
      return lastActionTime - levelStarted - wasOnPause;
    else
      return System.currentTimeMillis() - levelStarted - wasOnPause;
  }

  int speed()
  {
    return (int) (1 + time() / 60000);    // speed is increasing once a minute
  }
}
