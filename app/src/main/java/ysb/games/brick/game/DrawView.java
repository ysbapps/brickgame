package ysb.games.brick.game;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashMap;

import ysb.games.brick.R;

public class DrawView extends View
{
  Game game = null;

  private Rect bounds = null;
  private Rect offsets;
  private float cupSquare;
  private Rect cupRect;
  private float[] cupEdges;
  private Rect startTouch;
  private Rect pauseTouch;
  private Rect quitGameTouch;

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

  Bitmap bg;


  public DrawView(Context context)
  {
    super(context);
    System.out.println("View created");

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = 2;
    options.inJustDecodeBounds = false;
    bg = BitmapFactory.decodeResource(getResources(), R.drawable.bg, options);
    System.out.println("bg: " + bg.getWidth() + 'x' + bg.getHeight());
  }

  private void initialize(Canvas canvas)
  {
    bounds = canvas.getClipBounds();
    System.out.println("bounds: " + bounds);
    float dw = bounds.width() / 1000f;
    float dh = bounds.height() / 2000f;
    offsets = new Rect(Math.round(150 * dw), Math.round(200 * dh), Math.round(150 * dw), Math.round(50 * dh));
    System.out.println("offsets: " + offsets);

    int cw = bounds.width() / 2;
    int ch = bounds.height() / 12;
    int cx = bounds.centerX() - cw / 2;
    int cy = bounds.bottom - 30 * ch / 9;
    startTouch = new Rect(cx, cy, cx + cw, cy + ch);

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

    int pauseSquare = Math.min(cupRect.left, 200);
    int sx = cupRect.left - offsets.left;
    pauseTouch = new Rect(sx, 0, sx + pauseSquare, pauseSquare);
    quitGameTouch = new Rect(pauseTouch.right, 0, pauseTouch.right + pauseSquare, pauseSquare);

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
      if (startTouch.contains(x, y))
        game.newGame();

      performClick();
    }
    else    // game is started
    {
      if (touch.action == Touch.ACTION_MOVE && touch.dist > cupSquare &&
          (touch.dir == Touch.DIR_LEFT || touch.dir == Touch.DIR_RIGHT) && bounds.contains(x, y))    // touch down and touch move belong to the control rect
      {
        game.action(touch.dir == Touch.DIR_LEFT ? Game.MOVE_LEFT : Game.MOVE_RIGHT);
        touch.resetDist();
      }
      else if (touch.action == Touch.ACTION_UP)
      {
        if (touch.dist < 30)   // click
        {
          if (pauseTouch.contains(x, y))
          {
            if (game.state == Game.STATE_GAME)
              game.pause();
            else if (game.state == Game.STATE_PAUSED)
              game.resumeFromPause();
          }
          else if (quitGameTouch.contains(x, y))
            game.quitToStartPage();
        }
        else if (bounds.contains(x, y) && touch.dist > 50 && touch.dir == Touch.DIR_UP)
          game.action(Game.ROTATE);
        else if (bounds.contains(x, y) && touch.dist > 200 && touch.dir == Touch.DIR_DOWN)
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
    int bx = bounds.width() > bg.getWidth() ? (bounds.width() - bg.getWidth()) / 2 : 0;
    int by = cupRect.height() > bg.getHeight() ? (cupRect.height() - bg.getHeight()) / 2 : 0;
    canvas.drawBitmap(bg, bx, by, paints.cupContents);

    if (game.state == Game.STATE_NOT_STARTED)
    {
      drawStartPage(canvas);
    }
    else    // game in progress
    {
      drawGeneralControls(canvas);
      drawCup(canvas);
      drawGameInfo(canvas);
    }

//    canvas.drawRect(cupRect.right, cupRect.top - 100, bounds.right, cupRect.top, paints.debugLine);
//    for (Rect r : new Rect[]{pauseTouch, quitGameTouch, leftTouch, rightTouch, rotateTouch})
//      canvas.drawRect(r, paints.debugLine);
//    drawDebugInfo(canvas);
  }

  private void drawStartPage(Canvas canvas)
  {
    int scoreSize = game.scores.scoreTable.size();
    if (scoreSize > 0)
    {
      paints.text.setTextSize(40 * dk);
      paints.text.setColor(paints.controlColor);
      float sx1 = bounds.centerX() - 100 * dk;
      float sx2 = bounds.centerX() + 100 * dk;
      paints.text.setTextAlign(Paint.Align.RIGHT);
      canvas.drawText("best score", sx1, offsets.top, paints.text);
      paints.text.setTextAlign(Paint.Align.LEFT);
      canvas.drawText("best level", sx2, offsets.top, paints.text);
      paints.text.setTextSize(60 * dk);
      for (int i = 0; i < scoreSize; i++)
      {
        paints.text.setColor(Color.rgb(255, i * 255 / scoreSize, i * 255 / scoreSize));
        paints.text.setTextAlign(Paint.Align.RIGHT);
        int y = Math.round((i + 1) * 80 * dk);
        canvas.drawText("" + game.scores.scoreTable.get(i), sx1, offsets.top + y, paints.text);
        paints.text.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("" + game.scores.levelTable.get(i), sx2, offsets.top + y, paints.text);
      }
    }

    paints.text.setTextSize(60 * dk);
    paints.text.setColor(paints.controlColor);
    paints.text.setTextAlign(Paint.Align.CENTER);
    for (Rect rect : new Rect[]{startTouch})
    {
      paints.control.setColor(paints.controlColor);
      paints.control.setStyle(Paint.Style.STROKE);
      paints.control.setStrokeWidth(8);
      float r = (float) rect.height() / 2;
      canvas.drawCircle(rect.left, rect.top + r, r, paints.control);
      canvas.drawCircle(rect.right, rect.top + r, r, paints.control);
      paints.control.setColor(paints.controlColor);
      paints.control.setStyle(Paint.Style.STROKE);
      canvas.drawLine(rect.left, rect.top, rect.right, rect.top, paints.control);
      canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paints.control);
      canvas.drawText(rect == startTouch ? "START" : "QUIT", rect.centerX(), rect.top + 90 * dk, paints.text);
    }

  }

  private void drawGameInfo(Canvas canvas)
  {
    paints.text.setTextSize(30 * dk);
    paints.text.setTextAlign(Paint.Align.CENTER);
    paints.text.setColor(paints.controlColor);
    float lx = cupRect.left - 90 * dk;
    float rx = cupRect.right + 90 * dk;
    canvas.drawText("next", lx, cupRect.top + 30 * dk, paints.text);
    canvas.drawText("level", lx, cupRect.top + 280 * dk, paints.text);
    canvas.drawText("time", rx, cupRect.top + 30 * dk, paints.text);
    canvas.drawText("speed", rx, cupRect.top + 280 * dk, paints.text);
    canvas.drawText("score", rx, 50 * dk, paints.text);
    paints.text.setTextSize(120 * dk);
    canvas.drawText("" + game.level, lx, cupRect.top + 400 * dk, paints.text);
    paints.text.setTextSize(40 * dk);
    long time = game.time() / 1000;
    String min = "0" + time / 60;
    String sec = "0" + time % 60;
    canvas.drawText(min.substring(min.length() - 2) + ":" + sec.substring(sec.length() - 2), rx, cupRect.top + 90 * dk, paints.text);
    paints.text.setTextSize(60 * dk);
    canvas.drawText("" + game.speed(), rx, cupRect.top + 350 * dk, paints.text);

    if (game.message != null)
    {
      paints.text.setColor(Color.RED);
      paints.text.setTextSize(100 * dk);
      canvas.drawText("" + game.message, cupRect.centerX(), cupRect.centerY(), paints.text);
    }

    paints.text.setTextSize(70 * dk);
    paints.text.setColor(Color.RED);
    paints.text.setTextAlign(Paint.Align.RIGHT);
    canvas.drawText("" + game.score, cupRect.right + 140 * dk, 130 * dk, paints.text);
    if (game.prize > 0 && game.currentFigure != null)
    {
      paints.text.setTextAlign(Paint.Align.CENTER);
      paints.text.setColor(Color.rgb(255 - game.prizeCycle, 255 - game.prizeCycle, 0));
      canvas.drawText("" + game.prize, cupRect.left + (game.currentFigure.pos.x + 2) * cupSquare,
          cupRect.top + game.currentFigure.pos.y * cupSquare - game.prizeCycle, paints.text);
    }
  }

  private void drawCup(Canvas canvas)
  {
    canvas.drawLines(cupEdges, paints.cupEdge);   // cup edges

    paints.cupContents.setColor(paints.figureColor);    // figures
    for (Figure figure : new Figure[]{game.currentFigure, game.nextFigure})
      for (int y = 0; y < Figure.SIZE; y++)
        for (int x = 0; x < Figure.SIZE; x++)
          if (figure != null && figure.getCurrContents()[y][x])
            if (figure == game.currentFigure)
              drawCupSquare(canvas, figure.pos.x + x, figure.pos.y + y);
            else
              drawNextFigureSquare(canvas, x, y, figure.alignShift());

    if (game.state == Game.STATE_PAUSED)
      return;

    for (int y = 0; y < Cup.H; y++)   // cup contents
    {
      boolean isRowComplete = game.cup.isRowComplete(y);
      for (int x = 0; x < Cup.W; x++)
        if (game.cup.contents[y][x] > 0)
        {
          paints.cupContents.setColor(isRowComplete ? paints.cupColors[0] : paints.cupColors[game.cup.contents[y][x]]);
          drawCupSquare(canvas, x, y);
        }
    }
  }

  private void drawCupSquare(Canvas canvas, int x, int y)    // draw cupSquare on position
  {
    canvas.drawRect(cupRect.left + x * cupSquare, cupRect.top + y * cupSquare, cupRect.left + (x + 1) * cupSquare,
        cupRect.top + (y + 1) * cupSquare, paints.cupContents);
  }

  private void drawNextFigureSquare(Canvas canvas, int x, int y, byte alignShift)    // draw next figure cupSquare
  {
    long square = Math.round(offsets.left / 4.2);
    float sx = cupRect.left - 24 * dk - 4 * square + alignShift * 0.5f * square;
    float sy = cupRect.top + 30 * dk;
    canvas.drawRect(sx + x * square, sy + y * square, sx + (x + 1) * square, sy + (y + 1) * square, paints.cupContents);
  }

  private void drawGeneralControls(Canvas canvas)
  {
    paints.control.setColor(paints.controlColor);
    int x1 = pauseTouch.centerX() - pauseTouch.width() / 11;
    int x2 = pauseTouch.centerX() + pauseTouch.width() / 11;
    int y1 = pauseTouch.centerY() - pauseTouch.height() / 6;
    int y2 = pauseTouch.centerY() + pauseTouch.height() / 6;
    if (game.state == Game.STATE_PAUSED)
    {
      Path path = new Path();
      path.moveTo(x1, y1);
      path.lineTo(pauseTouch.centerX() + (float) pauseTouch.width() / 8, pauseTouch.centerY());
      path.lineTo(x1, y2);
      path.lineTo(x1, y1);
      paints.control.setStrokeWidth(1);
      paints.control.setStyle(Paint.Style.FILL);
      canvas.drawPath(path, paints.control);    // resume
    }

    if (game.state == Game.STATE_PAUSED || game.state == Game.STATE_GAME_OVER)
    {
      paints.control.setStrokeWidth(20);
      paints.control.setStyle(Paint.Style.STROKE);
      x1 = quitGameTouch.centerX() - quitGameTouch.width() / 8;
      x2 = quitGameTouch.centerX() + quitGameTouch.width() / 8;
      y1 = pauseTouch.centerY() - pauseTouch.height() / 8;
      y2 = pauseTouch.centerY() + pauseTouch.height() / 8;
      canvas.drawLine(x1, y1, x2, y2, paints.control);
      canvas.drawLine(x2, y1, x1, y2, paints.control);

      paints.control.setStrokeWidth(8);
      canvas.drawCircle(quitGameTouch.centerX(), quitGameTouch.centerY(), (float) quitGameTouch.width() / 3, paints.control);   // quit
    }

    if (game.state == Game.STATE_GAME)
    {
      paints.control.setStrokeWidth(20);
      paints.control.setStyle(Paint.Style.STROKE);
      canvas.drawLine(x1, y1, x1, y2, paints.control);
      canvas.drawLine(x2, y1, x2, y2, paints.control);    // pause
    }

    if (game.state == Game.STATE_GAME || game.state == Game.STATE_PAUSED)
    {
      paints.control.setStrokeWidth(8);
      paints.control.setStyle(Paint.Style.STROKE);
      canvas.drawCircle(pauseTouch.centerX(), pauseTouch.centerY(), (float) pauseTouch.width() / 3, paints.control);    // pause or resume
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

    Figure f = new Figure();
    Integer n = figures.get(f.type);
    if (n == null)
      figures.put(f.type, 1);
    else
      figures.put(f.type, n + 1);

    canvas.drawText("figures: " + figures, sx, sy + 270, paints.debugText);
  }

}