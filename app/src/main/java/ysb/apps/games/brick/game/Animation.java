package ysb.apps.games.brick.game;

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


  Animation(Bitmap image, PointF pos, long dur)
  {
    this.image = image;
    this.pos = pos;
    this.dur = dur;
    started = System.currentTimeMillis();
  }

  void update(Canvas canvas)
  {
    int a = Math.round(255f * (System.currentTimeMillis() - started) / dur);
    Paint p = new Paint();
    p.setAlpha(a);
    canvas.drawBitmap(image, pos.x, pos.y, p);

  }

}