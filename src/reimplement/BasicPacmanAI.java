package reimplement;

import mmaracic.gameaiframework.AgentAI;
import mmaracic.gameaiframework.PacmanVisibleWorld;
import mmaracic.gameaiframework.WorldEntity;

import java.util.*;

/**
 * BasicPacmanAI
 */
public class BasicPacmanAI extends AgentAI {
  private int radiusX = 0;
  private int radiusY = 0;

  private PacmanVisibleWorld visibleWorld = null;
  private int[] lastPoint = null;

  @Override
  public int decideMove(ArrayList<int[]> moves, PacmanVisibleWorld mySurroundings, WorldEntity.WorldEntityInfo myInfo) {
    //region Init
    radiusX = mySurroundings.getDimensionX() / 2;
    radiusY = mySurroundings.getDimensionY() / 2;

    visibleWorld = mySurroundings;
    //endregion

//    StringBuilder s = new StringBuilder();
//    for (int[] move : moves)
//      s.append(Arrays.toString(move)).append(" ");
//    printStatus(s.toString());

    int choice = 0;
    int[] useless = new int[]{0, 0};

    try {
      List<int[]> safe = new ArrayList<>();
      List<int[]> score = new ArrayList<>();

      for (int[] move : moves) {
        int report = scanner(useless, move, "Ghost");
        if (report != 1)
          safe.add(move);
      }

      if (safe.size() != 0)
        for (int[] move : safe) {
          int report = scanner(useless, move, "Point");
          System.out.println("Hunting a point: " + Arrays.toString(move));
          if (report == 1)
            score.add(move);
        }

      Date now = new Date();
      Random r = new Random(now.getTime());
      if (score.size() != 0) {
        choice = moves.indexOf(score.get(r.nextInt(score.size())));
      } else if (safe.size() != 0) {
        choice = moves.indexOf(safe.get(r.nextInt(safe.size())));
      } else {
        choice = r.nextInt(moves.size());
      }

    } catch (Exception e) {
      e.printStackTrace();
    }

    printStatus(Arrays.toString(moves.get(choice)));
    return choice;
  }

  private int scanner(int[] currentPosition, int[] movement, String target) {
    int[] nextPosition = vecAdd(currentPosition, movement);

    while (true) {
      if (outRange(-radiusX, radiusX, nextPosition[0]) || outRange(-radiusY, radiusY, nextPosition[1])) {
//        printStatus(radiusX + " " + radiusY + " " + Arrays.toString(nextPosition));
        return 0;
      }

//      printStatus("Checking out " + " " + nextPosition[0] + " " + nextPosition[1]);

      List<WorldEntity.WorldEntityInfo> infos = visibleWorld.getWorldInfoAt(nextPosition[0], nextPosition[1]);
      if (infos == null) {
//        printStatus("NULL");
        return 0;
      }

      for (WorldEntity.WorldEntityInfo info : infos) {
        if (isEntity(info, target) || (target.equals("Point") && isEntity(info, "Powerup"))) {
//          printStatus(target + ": " + nextPosition[0] + " " + nextPosition[1]);
          return 1;
        }

        if (isEntity(info, "Wall")) {
//          printStatus("WALL!");
          return 0;
        }
      }

      nextPosition[0] += movement[0];
      nextPosition[1] += movement[1];
    }
  }

  private boolean isEntity(WorldEntity.WorldEntityInfo info, String entity) {
    return info.getIdentifier().compareToIgnoreCase(entity) == 0;
  }

  private boolean isWall(int[] location) {
    List<WorldEntity.WorldEntityInfo> infos = visibleWorld.getWorldInfoAt(location[0], location[1]);

    for (WorldEntity.WorldEntityInfo info : infos) {
      if (isEntity(info, "Wall"))
        return true;
    }

    return false;
  }

  private int[] vecAdd(int[] o1, int[] o2) {
    return new int[]{
        o1[0] + o1[0],
        o2[1] + o2[1]
    };
  }

  private boolean outRange(int bottom, int up, int value) {
    return value < bottom || value > up;
  }

//  private int[] turnRight(int[] movement) {
//    int[] retValue = new int[2];
//
//    retValue[0] = movement[1];
//    retValue[1] = -movement[0];
//
//    return retValue;
//  }
//
//  private int[] turnLeft(int[] movement) {
//    int[] retValue = new int[2];
//
//    retValue[0] = -movement[1];
//    retValue[1] = movement[0];
//
//    return retValue;
//  }

//  private int manhattanDistance(int[] o1, int[] o2) {
//    return Math.abs(o1[0] - o2[0]) + Math.abs(o1[1] - o2[1]);
//  }
}
