package ysb.apps.games.brick.game;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;
import java.util.HashSet;

import ysb.apps.games.brick.BuildConfig;
import ysb.apps.games.brick.R;

public class DrawView extends View
{
  Game game = null;

  private Rect bounds = null;
  private Rect offsets;
  private float cupSquare;
  private Rect cupRect;
  private float[] cupEdges;
  private Button pauseBtn;
  private Button resumeBtn;
  private Button quitGameBtn;
  private Button sndOnBtn;
  private Button sndOffBtn;
  private Button startBtn;
  private Button contBtn;
  private Button optionsBtn;

  private Paints paints;
  private float dk;

  private final Touch touch = new Touch();

  private int count = 0;
  private int countPerSec = 0;
  private int fps = 0;
  private long lastTime = 0;
  private String event = "";
  public static String info = "";
  public static HashMap<Integer, Integer> figures = new HashMap<>();

  private final Bitmap[] bgs = new Bitmap[3];
  private final Bitmap left;
  private final Bitmap right;
  private final Bitmap rotate;
  private final Bitmap drop;
  private final HashSet<Integer> helpActions = new HashSet<>();
  private final HashMap<Integer, Animation> animations = new HashMap<>();

  private boolean soundsOn = true;


  public DrawView(Context context)
  {
    super(context);
    System.out.println("View created");

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = 2;
    options.inJustDecodeBounds = false;
    bgs[0] = BitmapFactory.decodeResource(getResources(), R.drawable.start_bg, options);
    bgs[1] = BitmapFactory.decodeResource(getResources(), R.drawable.bg, options);
    System.out.println("bg: " + bgs[1].getWidth() + 'x' + bgs[1].getHeight());
    bgs[2] = BitmapFactory.decodeResource(getResources(), R.drawable.pause_bg, options);

    options.inSampleSize = 4;
    left = BitmapFactory.decodeResource(getResources(), R.drawable.left, options);
    right = BitmapFactory.decodeResource(getResources(), R.drawable.right, options);
    rotate = BitmapFactory.decodeResource(getResources(), R.drawable.rotate, options);
    drop = BitmapFactory.decodeResource(getResources(), R.drawable.drop, options);
  }

  private void initialize(Canvas canvas)
  {
    bounds = canvas.getClipBounds();
    System.out.println("bounds: " + bounds);
    float dw = bounds.width() / 1000f;
    float dh = bounds.height() / 2000f;
    offsets = new Rect(Math.round(150 * dw), Math.round(200 * dh), Math.round(150 * dw), Math.round(50 * dh));
    System.out.println("offsets: " + offsets);

    float maxSquareW = (bounds.width() - offsets.left - offsets.right) / (float) Cup.W;
    float maxSquareH = (bounds.height() - offsets.top - offsets.bottom) / (float) Cup.H;
    cupSquare = Math.min(maxSquareW, maxSquareH);
    dk = cupSquare / 64;  // display coeff for smaller screens
    System.out.println("cupSquare, dk: " + cupSquare + ", " + dk);

    cupRect = new Rect(Math.round(bounds.centerX() - Cup.W * cupSquare / 2), offsets.top,
        Math.round(bounds.centerX() + Cup.W * cupSquare / 2), offsets.top + Math.round(Cup.H * cupSquare));
    float cupEdgeWidth = cupSquare / 5;
    cupEdges = new float[]{
        cupRect.left - cupEdgeWidth / 2, cupRect.top, cupRect.left - cupEdgeWidth / 2, cupRect.bottom + cupEdgeWidth,
        cupRect.left, cupRect.bottom + cupEdgeWidth / 2, cupRect.right, cupRect.bottom + cupEdgeWidth / 2,
        cupRect.right + cupEdgeWidth / 2, cupRect.bottom + cupEdgeWidth, cupRect.right + cupEdgeWidth / 2, cupRect.top};

    startBtn = new Button(BitmapFactory.decodeResource(getResources(), R.drawable.start), cupRect.left - 80 * dk, bounds.bottom - Math.round(600 * dk));
    contBtn = new Button(BitmapFactory.decodeResource(getResources(), R.drawable.cont), cupRect.left - 80 * dk, startBtn.rect.bottom + 50 * dk);
    optionsBtn = new Button(BitmapFactory.decodeResource(getResources(), R.drawable.opts), cupRect.right - 30 * dk, bounds.bottom - Math.round(300 * dk));

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = dk < 0.8 ? 2 : 1;
    options.inJustDecodeBounds = false;
    pauseBtn = new Button(BitmapFactory.decodeResource(getResources(), R.drawable.pause, options), cupRect.right + 20 * dk, cupRect.top + 470 * dk);
    resumeBtn = new Button(BitmapFactory.decodeResource(getResources(), R.drawable.resume, options), pauseBtn.rect.left, pauseBtn.rect.top);
    Bitmap img = BitmapFactory.decodeResource(getResources(), R.drawable.quit, options);
    float lx = cupRect.left - img.getWidth() - 18 * dk;
    quitGameBtn = new Button(img, lx, pauseBtn.rect.top);
    sndOnBtn = new Button(BitmapFactory.decodeResource(getResources(), R.drawable.snd_on, options), lx, pauseBtn.rect.top);
    sndOffBtn = new Button(BitmapFactory.decodeResource(getResources(), R.drawable.snd_off, options), lx, pauseBtn.rect.top);

    paints = new Paints(cupEdgeWidth);
  }

  @Override
  public boolean performClick()
  {
    super.performClick();
    return false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent evt)
  {
    event = evt.toString();
    touch.onEvent(evt);
//    System.out.println(touch);
    int x = touch.x;
    int y = touch.y;
    if (game.state == Game.STATE_NOT_STARTED && touch.action == Touch.ACTION_UP)
    {
      if (startBtn.rect.contains(x, y))
        game.newGame((byte) 1);
      else if (contBtn.rect.contains(x, y))
        game.newGame(game.scores.getMaxAchievedLevel());
      else if (optionsBtn.rect.contains(x, y))
        game.newGame((byte) 1);

      play(R.raw.click);
      performClick();
    }
    else    // game is started
    {
      double minSlideDist = 0.7 * cupSquare * dk;
      if (touch.action == Touch.ACTION_MOVE && touch.dist > minSlideDist &&
          (touch.dir == Touch.DIR_LEFT || touch.dir == Touch.DIR_RIGHT) && bounds.contains(x, y))    // touch down and touch move belong to the control rect
      {
        game.action(touch.dir == Touch.DIR_LEFT ? Game.MOVE_LEFT : Game.MOVE_RIGHT);
        play(R.raw.move);
        touch.movedLeftRight();
      }
      else if (touch.action == Touch.ACTION_UP && !touch.movedLeftRight)
      {
        if (touch.dist < minSlideDist)   // click
        {
          if (pauseBtn.rect.contains(x, y))
          {
            if (game.state == Game.STATE_GAME)
            {
              game.pause();
              animations.clear();
            }
            else if (game.state == Game.STATE_PAUSED)
              game.resumeFromPause();

            play(R.raw.click);
          }
          else if (game.state == Game.STATE_GAME && sndOnBtn.rect.contains(x, y))
          {
            soundsOn = !soundsOn;
            play(R.raw.click);
          }
          else if (game.state == Game.STATE_PAUSED && quitGameBtn.rect.contains(x, y))
          {
            game.quitToStartPage();
            play(R.raw.click);
          }
          else if (BuildConfig.DEBUG && x > cupRect.right && y < cupRect.top)
          {
            game.level++;
            game.cup.loadLevel(game.level);
          }
          else if (touch.y > cupRect.top && touch.x < bounds.width() / 2f)
          {
            game.action(Game.MOVE_LEFT);
            play(R.raw.move);
          }
          else if (touch.y > cupRect.top && touch.x > bounds.width() / 2f)
          {
            game.action(Game.MOVE_RIGHT);
            play(R.raw.move);
          }

          performClick();
        }
        else if (bounds.contains(x, y) && touch.dist > minSlideDist / 2 && touch.dir == Touch.DIR_UP)
          game.action(Game.ROTATE);
        else if (bounds.contains(x, y) && touch.dist > 3 * minSlideDist && touch.dir == Touch.DIR_DOWN)
          game.action(Game.DROP);
      }

    }

    return true;
  }

  protected void onDraw(Canvas canvas)
  {
//    System.out.println("onDraw");
    if (bounds == null)
      initialize(canvas);

    canvas.drawColor(Color.BLACK);
    int bx = bounds.width() > bgs[0].getWidth() ? (bounds.width() - bgs[0].getWidth()) / 2 : 0;
    int by = cupRect.height() > bgs[0].getHeight() ? (cupRect.height() - bgs[0].getHeight()) / 2 : 0;
    int ii = game.state < 3 ? game.state : 1;
    canvas.drawBitmap(bgs[ii], bx, by, paints.cupContents);

    if (game.state == Game.STATE_NOT_STARTED)
      drawStartPage(canvas);
//    else if (game.state == Game.STATE_CHOOSE_LEVEL)
//      drawChooseLevelScreen(canvas);
    else    // game in progress
    {
      drawGameControls(canvas);
      drawCup(canvas);
      drawGameInfo(canvas);

      if (game.state == Game.STATE_GAME)
      {
        game.needHelp(helpActions);
        syncAnimations();
//        System.out.println(animations);
        for (Animation animation : animations.values())
          animation.update(canvas);
      }
    }

//    canvas.drawRect(cupRect.right, cupRect.top - 100, bounds.right, cupRect.top, paints.debugLine);
//    for (Rect r : new Rect[]{pauseTouch, quitGameTouch})
//      canvas.drawRect(r, paints.debugLine);
//    drawDebugInfo(canvas);
  }

  private void syncAnimations()
  {
    for (int i = 0; i < animations.size(); i++)
    {
      for (int id : animations.keySet())
        if (!helpActions.contains(id))
        {
          animations.remove(id);
          break;
        }
    }

    for (int id : helpActions)
      if (!animations.containsKey(id))
      {
        Bitmap[] ba = {null, left, right, rotate, drop};
        float lry = cupRect.bottom - 300 * dk;
        float rdx = cupRect.centerX() - rotate.getWidth() / 2f;
        PointF[] pa = {null, new PointF(cupRect.left, lry), new PointF(cupRect.right - right.getWidth(), lry), new PointF(rdx, lry - 100 * dk), new PointF(rdx, lry)};
        animations.put(id, new Animation(ba[id], pa[id], 1000));
      }
  }

  private void drawStartPage(Canvas canvas)
  {
    int scoreSize = game.scores.scoreTable.size();
    if (scoreSize > 0)
    {
      paints.text.setTextSize(40 * dk);
      paints.text.setColor(paints.controlColor);
      float sx1 = cupRect.left - offsets.left + 60 * dk;
      float sx2 = cupRect.left - offsets.left + 260 * dk;
      paints.text.setTextAlign(Paint.Align.LEFT);
      canvas.drawText("best score      best level", sx1, offsets.top, paints.text);
      paints.text.setTextSize(50 * dk);
      for (int i = 0; i < scoreSize; i++)
      {
        int c = (scoreSize - i) * 255 / scoreSize;
        paints.text.setColor(Color.rgb(c, c, 255));
        paints.text.setTextAlign(Paint.Align.RIGHT);
        int y = Math.round((i + 1) * 60 * dk);
        canvas.drawText("" + game.scores.scoreTable.get(i), sx2 - 20 * dk, offsets.top + 10 * dk + y, paints.text);
        paints.text.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("" + game.scores.levelTable.get(i), sx2 + 100 * dk, offsets.top + 10 * dk + y, paints.text);
      }
    }

    paints.control.setColor(paints.controlColor);
    paints.control.setStyle(Paint.Style.STROKE);
    paints.control.setStrokeWidth(8);
    paints.text.setTextSize(50 * dk);
    paints.text.setTextAlign(Paint.Align.LEFT);
    paints.text.setColor(Color.WHITE);
    startBtn.draw(canvas);
    contBtn.enabled = game.scores.getMaxAchievedLevel() > 1;
    contBtn.draw(canvas);
//    canvas.drawText("start new game", startBtn.rect.right + dk * 20, startBtn.rect.centerY() + dk * 18, paints.text);
    if (game.scores.getMaxAchievedLevel() > 1)
    {
//      canvas.drawText("continue game", contBtn.rect.right + dk * 20, contBtn.rect.centerY() + dk * 18, paints.text);
      paints.text.setTextSize(74 * dk);
      paints.text.setTextAlign(Paint.Align.CENTER);
      paints.text.setColor(Color.rgb(80, 80, 255));
      float x = contBtn.rect.centerX() - 10 * dk;
      float y = contBtn.rect.centerY() + dk * 26;
      String lvl = "" + game.scores.getMaxAchievedLevel();
      paints.text.setColor(Color.BLACK);
      canvas.drawText(lvl, x + 4 * dk, y + 4 * dk, paints.text);
      paints.text.setColor(Color.rgb(255, 255, 180));
      canvas.drawText(lvl, x, y, paints.text);

    }

    optionsBtn.draw(canvas);
  }

//  private void drawChooseLevelScreen(Canvas canvas)
//  {
//    paints.control.setColor(paints.controlColor);
//    paints.control.setStyle(Paint.Style.STROKE);
//    paints.control.setStrokeWidth(8);
//    paints.text.setTextSize(70 * dk);
//    paints.text.setTextAlign(Paint.Align.CENTER);
//    paints.text.setColor(Color.BLUE);
//    float r = (float) startTouch.height() / 2;
//    int sx =
//    for (int x = 0; x<3;x++)
//    for (int y = 0; y<10;y++)
//    canvas.drawCircle(startTouch.left + r, startTouch.top + r, r, paints.control);
//
//
//  }

  private void drawGameInfo(Canvas canvas)
  {
    paints.text.setTextSize(30 * dk);
    paints.text.setTextAlign(Paint.Align.CENTER);
    paints.text.setColor(paints.controlColor);
    float lx = cupRect.left - 74 * dk;
    float rx = cupRect.right + 74 * dk;
    canvas.drawText("next", lx, cupRect.top + 30 * dk, paints.text);
    canvas.drawText("level", lx, cupRect.top + 280 * dk, paints.text);
    canvas.drawText("time", rx, cupRect.top + 30 * dk, paints.text);
    canvas.drawText("speed", rx, cupRect.top + 280 * dk, paints.text);
    canvas.drawText("score", rx, 50 * dk, paints.text);
    paints.text.setTextSize(100 * dk);
    canvas.drawText("" + game.level, lx, cupRect.top + 400 * dk, paints.text);
    paints.text.setTextSize(40 * dk);
    long time = game.time() / 1000;
    String min = "0" + time / 60;
    String sec = "0" + time % 60;
    canvas.drawText(min.substring(min.length() - 2) + ":" + sec.substring(sec.length() - 2), rx, cupRect.top + 90 * dk, paints.text);
    paints.text.setTextSize(60 * dk);
    canvas.drawText("" + game.speed(), rx, cupRect.top + 350 * dk, paints.text);

    paints.text.setTextSize(70 * dk);
    paints.text.setColor(game.state == Game.STATE_GAME_OVER ? Color.RED : Color.WHITE);
    if (game.message != null)
    {
      int c = paints.text.getColor();
      paints.text.setColor(Color.BLACK);
      float y = cupRect.centerY() - 150 * dk;
      canvas.drawText(game.message, cupRect.centerX() + 4 * dk, y + 4 * dk, paints.text);
      paints.text.setColor(c);
      canvas.drawText(game.message, cupRect.centerX(), y, paints.text);
    }

    paints.text.setTextAlign(Paint.Align.RIGHT);
    canvas.drawText("" + game.score, cupRect.right + 140 * dk, 130 * dk, paints.text);
    if (game.prize > 0 && game.currentFigure != null)
    {
      paints.text.setTextAlign(Paint.Align.CENTER);
      paints.text.setColor(Color.rgb(255 - game.prizeCycle, 255 - game.prizeCycle, 0));
      canvas.drawText("+" + game.prize, cupRect.left + (game.currentFigure.pos.x + 2) * cupSquare,
          cupRect.top + game.currentFigure.pos.y * cupSquare - game.prizeCycle, paints.text);
    }
  }

  private void drawCup(Canvas canvas)
  {
    canvas.drawLines(cupEdges, paints.cupEdge);   // cup edges

    float sw = paints.cupEdge.getStrokeWidth();
    int c = paints.cupEdge.getColor();
    paints.cupEdge.setStrokeWidth(sw / 3);
    paints.cupEdge.setColor(Color.WHITE);
    canvas.drawLine(cupRect.left - sw / 2 - dk, cupRect.top, cupRect.left - sw / 2 - dk, cupRect.bottom, paints.cupEdge);
    canvas.drawLine(cupRect.right + sw / 2 - dk, cupRect.top, cupRect.right + sw / 2 - dk, cupRect.bottom, paints.cupEdge);
    canvas.drawLine(cupRect.left - sw, cupRect.bottom + sw / 2, cupRect.right + sw, cupRect.bottom + sw / 2, paints.cupEdge);
    paints.cupEdge.setStrokeWidth(sw);
    paints.cupEdge.setColor(c);

    paints.cupContents.setColor(paints.figureColor);    // figures
    drawFigure(canvas, game.currentFigure);
    drawFigure(canvas, game.nextFigure);
    if (game.state == Game.STATE_PAUSED)
      return;

    for (int y = 0; y < Cup.H; y++)   // cup contents
    {
      boolean isRowComplete = game.cup.isRowComplete(y);
      for (int x = 0; x < Cup.W; x++)
        if (game.cup.contents[y][x] > 0)
        {
          paints.cupContents.setColor(isRowComplete ? paints.cupColors[0] : paints.cupColors[game.cup.contents[y][x]]);
          drawCupSquare(canvas, x, y, !isRowComplete && game.cup.contents[y][x] == 1);
        }
    }
  }

  private void drawFigure(Canvas canvas, Figure figure)
  {
    for (int y = 0; y < Figure.SIZE; y++)
      for (int x = 0; x < Figure.SIZE; x++)
        if (figure != null && figure.getCurrContents()[y][x])
          if (figure == game.currentFigure)
            drawCupSquare(canvas, figure.pos.x + x, figure.pos.y + y, true);
          else
            drawNextFigureSquare(canvas, x, y, figure.alignShift());
  }

  private void drawCupSquare(Canvas canvas, int x, int y, boolean border)    // draw cupSquare on position
  {
    paints.cupContents.setStyle(Paint.Style.FILL);
    paints.cupContents.setAlpha(255);

    float left = cupRect.left + x * cupSquare;
    float top = cupRect.top + y * cupSquare;
    float right = cupRect.left + (x + 1) * cupSquare;
    float bottom = cupRect.top + (y + 1) * cupSquare;
    canvas.drawRect(left, top, right, bottom, paints.cupContents);

    int c = paints.cupContents.getColor();

    paints.cupContents.setColor(Color.DKGRAY);
    paints.cupContents.setStyle(Paint.Style.STROKE);
    paints.cupContents.setStrokeWidth(Math.round(2 * dk));
    paints.cupContents.setAlpha(border ? 255 : 30);
    canvas.drawRect(left, top, right - dk, bottom - dk, paints.cupContents);
    float ld = cupSquare / 3.5f;
    paints.cupContents.setColor(Color.GRAY);
    paints.cupContents.setAlpha(border ? 255 : 150);
    canvas.drawLine(left + ld, bottom - 1.5f * ld, right - 1.5f * ld, top + ld, paints.cupContents);
    canvas.drawLine(left + ld, bottom - ld, right - ld, top + ld, paints.cupContents);
    canvas.drawLine(left + 1.5f * ld, bottom - ld, right - ld, top + 1.5f * ld, paints.cupContents);

    paints.cupContents.setStyle(Paint.Style.FILL);

    float dd = cupSquare / 6;
    float dw = cupSquare / 24;
    right -= 3;
    bottom -= 3;
    canvas.drawCircle(left + dd, top + dd, dw, paints.cupContents);
    canvas.drawCircle(right - dd, top + dd, dw, paints.cupContents);
    canvas.drawCircle(left + dd, bottom - dd, dw, paints.cupContents);
    canvas.drawCircle(right - dd, bottom - dd, dw, paints.cupContents);

    paints.cupContents.setColor(c);
  }

  private void drawNextFigureSquare(Canvas canvas, int x, int y, byte alignShift)    // draw next figure cupSquare
  {
    long square = Math.round(offsets.left / 4.2);
    float sx = cupRect.left - 18 * dk - 4 * square + alignShift * 0.5f * square;
    float sy = cupRect.top + 30 * dk;
    canvas.drawRect(sx + x * square, sy + y * square, sx + (x + 1) * square, sy + (y + 1) * square, paints.cupContents);
  }

  private void drawGameControls(Canvas canvas)
  {
    if (game.state == Game.STATE_PAUSED && game.hasMoreLevels())
      resumeBtn.draw(canvas);

    if (game.state == Game.STATE_PAUSED || game.state == Game.STATE_GAME_OVER)
      quitGameBtn.draw(canvas);

    if (game.state == Game.STATE_GAME)
    {
      pauseBtn.draw(canvas);
      if (soundsOn)
        sndOffBtn.draw(canvas);
      else
        sndOnBtn.draw(canvas);
    }
  }

  void play(int id)
  {
    if (soundsOn)
    {
      MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), id);
      mediaPlayer.start();
    }
  }

  private void drawDebugInfo(Canvas canvas)
  {
    canvas.drawRect(1.1f, 3, bounds.right, bounds.bottom - 0.1f, paints.debugLine);
    int sx = cupRect.left + 5;
    int sy = 250;

    canvas.drawText("" + bounds.right + "x" + bounds.bottom, sx, sy, paints.debugText);
    canvas.drawText("count: " + (count++), sx, sy + 30, paints.debugText);
    if (System.currentTimeMillis() - lastTime > 1000)
    {
      fps = countPerSec;
      countPerSec = 0;
      lastTime = System.currentTimeMillis();
    }
    canvas.drawText("FPS: " + fps, sx, sy + 60, paints.debugText);
    countPerSec++;

    canvas.drawText("" + cupSquare, sx, sy + 90, paints.debugText);
    canvas.drawText("" + game.state + " - " + game.level, sx, sy + 120, paints.debugText);
    canvas.drawText(info, sx, sy + 150, paints.debugText);
    canvas.drawText(event, sx, sy + 180, paints.debugText);
    canvas.drawText("" + touch.down, sx, sy + 210, paints.debugText);
    canvas.drawText("" + touch.dist, sx, sy + 240, paints.debugText);

    Figure f = new Figure(false);
    Integer n = figures.get(f.type);
    if (n == null)
      figures.put(f.type, 1);
    else
      figures.put(f.type, n + 1);

    canvas.drawText("figures: " + figures, sx, sy + 270, paints.debugText);
  }

}