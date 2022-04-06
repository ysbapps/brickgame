package ysb.apps.games.brick.game;

import java.util.HashSet;

import ysb.apps.games.brick.InAppsProductsManager;
import ysb.apps.games.brick.MainBrickActivity;
import ysb.apps.games.brick.Product;
import ysb.apps.games.brick.R;
import ysb.apps.utils.logs.L;

public class Game extends Thread
{
  final static int STATE_NOT_STARTED = 0;
  final static int STATE_GAME = 1;
  final static int STATE_PAUSED = 2;
  final static int STATE_DROPPING = 3;
  final static int STATE_GAME_OVER = 4;
  final static int STATE_OPTIONS = 5;
  final static int STATE_LOGS = 9;
//  final static int STATE_CHOOSE_LEVEL = 6;

  final static int MOVE_LEFT = 1;
  final static int MOVE_RIGHT = 2;
  final static int ROTATE = 3;
  final static int DROP = 4;

  final Cup cup;
  private DrawView view = null;
  int state = STATE_NOT_STARTED;
  Figure currentFigure;
  Figure nextFigure;
  Figure droppedFigure;
  long levelStarted;
  long lastActionTime;
  private long wasOnPause;
  byte level;
  int score;
  int prize;
  int prizeCycle;
  String message;

  private int figureCount = 0;
  private long figureStartTime;
  private final HashSet<Integer> figureActions = new HashSet<>();
  Scores scores;
  final InAppsProductsManager prodManager;
  boolean isAlive = true;
  public static Game game = null;
  public final MainBrickActivity activity;


  public Game(MainBrickActivity activity)
  {
    super();

    this.activity = activity;
    prodManager = new InAppsProductsManager(activity);

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

  void newGame(byte level)
  {
    if (state != STATE_NOT_STARTED)
      return;

    L.i("newGame, level: " + level);
    levelStarted = System.currentTimeMillis();
    this.level = level;
    score = 0;
    prize = 0;
    message = null;
    cup.loadLevel(level);
    currentFigure = new Figure(level == 1);
    makeDroppedFigure();
    nextFigure = new Figure(level == 1);
    figureCount = 0;
    figureActions.clear();
    figureStartTime = lastActionTime = System.currentTimeMillis();
    wasOnPause = 0;

    state = STATE_GAME;
  }

  public void updateProducts()
  {
    prodManager.update();
  }

  void showOptions()
  {
    state = STATE_OPTIONS;
    if (!prodManager.purchasesUpdated || !prodManager.isConnected())
      updateProducts();
  }

  void purchaseProduct(Product p)
  {
    L.i("purchasing product: " + p);
    if (p != null)
      prodManager.purchase(p.id);
    else
      prodManager.message = "Products not available";

  }

  public void run()
  {
    L.i("App is started.");
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
          if (System.currentTimeMillis() < levelStarted + 1000)
          {
            message = "Level " + level;
            repaint();
            sleepMs(1000);
            message = null;
          }

          int delay = 800 - speed() * 50;    // 750 (speed 1) -> 550 (speed 5) -> 300(10) -> 250(11)->200(12)->150(13) -> 0 (at speed 16)
          if (delay < 300)    // 300(10) -> 275(11)->220(12)->165(13)
            delay = (int) (1.1 * delay);
          while (delay-- > 0 && state == STATE_GAME && isAlive)
          {
            sleepMs(1);
            if (delay % 50 == 0)
              repaint();
          }
        }
        else //if (state == STATE_DROPPING)
          sleepMs(10);

        currentFigure.pos.y++;
        if (droppedFigure != null && droppedFigure.pos.y - currentFigure.pos.y < 4)
          droppedFigure = null;

        if (cup.isFigurePositionValid(currentFigure))      // end of normal loop (figure is falling or dropping)
          continue;

        currentFigure.pos.y--;    // figure has reached a bottom shape - appending to the cup
        currentFigure.movable = false;
        cup.appendFigure(currentFigure);
        if (state == STATE_DROPPING)
        {
          state = STATE_GAME;
          view.sndManager.play(R.raw.drop);
        }
        repaint();
        sleepMs(100);

        int mergedRows = cup.premerge();
        if (mergedRows > 0)
          merge(mergedRows);

        boolean cont = true;
        if (cup.isLevelComplete())
          cont = nextLevel();

        if (!cont)
          continue;

        currentFigure = nextFigure;
        makeDroppedFigure();
        figureCount++;
        figureActions.clear();
        figureStartTime = lastActionTime = System.currentTimeMillis();
        if (!cup.isFigurePositionValid(currentFigure))
        {
          finishGame();
          repaint();
        }

        nextFigure = new Figure(false);
        repaint();
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    L.i("Game is stopped.");
  }

  private void makeDroppedFigure()
  {
    if (level > 5)
    {
      droppedFigure = null;
      return;
    }

    droppedFigure = currentFigure.cloneDropped();
    while (cup.isFigurePositionValid(droppedFigure))
      droppedFigure.pos.y++;

    droppedFigure.pos.y--;

    if (droppedFigure.pos.y - currentFigure.pos.y < 4)
      droppedFigure = null;
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

    prodManager.disconnect();
    updateProducts();

    repaint();
  }

//  void openChooseLevel()
//  {
//    state = STATE_CHOOSE_LEVEL;
//    repaint();
//  }

  private void merge(int mergedRows)
  {
    view.sndManager.play(R.raw.remove_line);
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
    L.i("finishGame");
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
        else
          view.sndManager.play(R.raw.rotate);
        break;
      case DROP:
        state = STATE_DROPPING;
        currentFigure.movable = false;
        droppedFigure = null;
        break;
    }

    if (state != STATE_DROPPING)
      makeDroppedFigure();

    figureActions.add(action);
    lastActionTime = System.currentTimeMillis();
    repaint();
  }

  void needHelp(HashSet<Integer> actions)
  {
    actions.clear();
    long now = System.currentTimeMillis();
    double seconds = (now - figureStartTime) / 1000.0;
    if (level == 1 && figureCount < 10 && !figureActions.contains(DROP) && now - lastActionTime > (figureCount > 5 ? 2000 : 600))
    {
      if (!figureActions.contains(MOVE_LEFT) && !figureActions.contains(MOVE_RIGHT) && seconds < 6)
      {
        actions.add(MOVE_LEFT);
        actions.add(MOVE_RIGHT);
      }

      currentFigure.rotate();
      boolean canRotate = cup.isFigurePositionValid(currentFigure);
      currentFigure.rotateBack();
      if (currentFigure.type != Figure.TYPE_SQUARE && canRotate && !figureActions.contains(ROTATE) && seconds < 6)
        actions.add(ROTATE);
      if (actions.size() == 0 && !figureActions.contains(DROP))
        actions.add(DROP);
    }
  }

  private boolean nextLevel()
  {
    L.i("nextLevel..");
    view.sndManager.play(R.raw.level);
    currentFigure = null;
    figureCount = 0;
    repaint();
    sleepMs(state == STATE_NOT_STARTED ? 0 : 500);   // handle game quit
    prize = 10 * level * level;
    score += prize;
    message = "Bonus  +" + prize;
    scores.addScore(score, level);
    repaint();
    sleepMs(state == STATE_NOT_STARTED ? 0 : 1500);   // handle game quit
    prize = 0;
    message = null;
    repaint();
    sleepMs(state == STATE_NOT_STARTED ? 0 : 500);   // handle game quit
    if (state == STATE_NOT_STARTED)
      return false;

    if (hasMoreLevels())
    {
      level++;
      scores.saveMaxLevel(level);
      if (cup.loadLevel(level))
        message = "Level " + level;
      repaint();
      sleepMs(1000);
      message = null;
      levelStarted = System.currentTimeMillis();
      wasOnPause = 0;
      return true;
    }
    else
    {
      state = STATE_GAME_OVER;
      message = "All levels completed";
      return false;
    }
  }

  boolean hasMoreLevels()
  {
    return level < (prodManager.isProductPurchased(InAppsProductsManager.PROD_20_LEVELS) ? 31 : 10);
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
      return System.currentTimeMillis() - levelStarted - wasOnPause;// + 600000;
  }

  int speed()
  {
    return (int) (1 + time() / 60000);    // speed is increasing once a minute
  }

  public void release()
  {
    L.i("game release()");
    view.sndManager.release();
    prodManager.disconnect();
  }

}
