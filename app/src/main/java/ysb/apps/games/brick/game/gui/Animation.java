package ysb.apps.games.brick.game.gui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;

public class Animation
{
  private final Bitmap image;
  private final PointF pos;
  private final long started;
  private final long dur;


  public Animation(Bitmap image, PointF pos, long dur)
  {
    this.image = image;
    this.pos = pos;
    this.dur = dur;
    started = System.currentTimeMillis();
  }

  public void draw(Canvas canvas)
  {
    int a = Math.round(255f * (System.currentTimeMillis() - started) / dur);
    if (a > 255)
      a = 255;
    Paint p = new Paint();
    p.setAlpha(a);
    canvas.drawBitmap(image, pos.x, pos.y, p);

  }

}