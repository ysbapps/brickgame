package ysb.apps.games.brick.game;

import android.graphics.PointF;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

public class Touch
{
  private final static int ACTION_UNDEFINED = 0;
  final static int ACTION_DOWN = 1;
  final static int ACTION_MOVE = 2;
  final static int ACTION_UP = 3;

  final static int DIR_UP = 0;
  final static int DIR_RIGHT = 1;
  final static int DIR_DOWN = 2;
  final static int DIR_LEFT = 3;

  PointF down = new PointF();
  int x;
  int y;
  int action = ACTION_UNDEFINED;
  int dir;
  int dist;
  boolean movedLeftRight;


  void onEvent(MotionEvent evt)
  {
    if (evt.getAction() == MotionEvent.ACTION_DOWN)
      down(evt.getX(), evt.getY());
    else if (evt.getAction() == MotionEvent.ACTION_MOVE)
    {
      if (action == ACTION_UP || action == ACTION_UNDEFINED)       // down action can be skipped by device
        down(evt.getX(), evt.getY());
      else
        move(evt.getX(), evt.getY());
    }
    else if (evt.getAction() == MotionEvent.ACTION_UP)
      action = ACTION_UP;
    else
      action = ACTION_UNDEFINED;

    x = Math.round(evt.getX());
    y = Math.round(evt.getY());
    int dx = (int) Math.abs(x - down.x);
    int dy = (int) Math.abs(y - down.y);
    dist = (int) Math.sqrt(dx * dx + dy * dy);
  }

  private void down(float x, float y)
  {
    down.x = x;
    down.y = y;
    action = ACTION_DOWN;
    movedLeftRight = false;
  }

  void movedLeftRight()
  {
    down.x = x;
    down.y = y;
    movedLeftRight = true;
  }

  private void move(float x, float y)
  {
    double angle = Math.atan2(x - down.x, down.y - y);    // 0 - PI (<180); -PI(>180) - 0
    if (angle < 0)    // 0 - 2PI
      angle += 2 * Math.PI;

    dir = (int) Math.round(2 * angle / Math.PI);
    if (dir == 4)
      dir = 0;

    action = ACTION_MOVE;
  }

  @NonNull
  @Override
  public String toString()
  {
    return "Touch{" +
        "down=" + down +
        ", x=" + x +
        ", y=" + y +
        ", action=" + action +
        ", dir=" + dir +
        ", dist=" + dist +
        '}';
  }
}