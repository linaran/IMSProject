package reimplement;

import com.jme3.math.Vector3f;
import mmaracic.gameaiframework.AgentAI;
import mmaracic.gameaiframework.PacmanAgent;
import mmaracic.gameaiframework.PacmanVisibleWorld;
import mmaracic.gameaiframework.WorldEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;

/**
 *
 */
public class PacmanAI extends AgentAI {
  protected static class Location implements Comparable<Location> {
    int x = 0, y = 0;

    Location(int x, int y) {
      this.x = x;
      this.y = y;
    }

    int getX() {
      return x;
    }

    int getY() {
      return y;
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof Location) {
        Location temp = (Location) o;
        return (temp.x == this.x) && (temp.y == this.y);
      } else
        return false;
    }

    @Override
    public int hashCode() {
      int hash = 7;
      hash = 79 * hash + this.x;
      hash = 79 * hash + this.y;
      return hash;
    }

    public float distanceTo(Location other) {
      int distanceX = other.x - x;
      int distanceY = other.y - y;

      return (float) Math.abs(distanceX) + Math.abs(distanceY);
//            return (float) Math.sqrt(distanceX*distanceX + distanceY+distanceY);
    }

    @Override
    public int compareTo(Location o) {
      if (x == o.x) {
        return Integer.compare(y, o.y);
      } else {
        return Integer.compare(x, o.x);
      }
    }
  }

  private HashSet<Location> points = new HashSet<>();
  private Location myLocation = new Location(0, 0);

  private Date now = new Date();
  private Random r = new Random(now.getTime());

  private Location targetLocation = myLocation;
  private float targetDistance = Float.MAX_VALUE;
  private int targetDuration = 0;

  @Override
  public int decideMove(ArrayList<int[]> moves, PacmanVisibleWorld mySurroundings, WorldEntity.WorldEntityInfo myInfo) {
    int radiusX = mySurroundings.getDimensionX() / 2;
    int radiusY = mySurroundings.getDimensionY() / 2;

    boolean powerUP = myInfo.hasProperty(PacmanAgent.powerupPropertyName);
    Vector3f pos = myInfo.getPosition();
//        printStatus("Location x: "+pos.x+" y: "+pos.y);

    float ghostDistance = Float.MAX_VALUE;
    Location ghostLocation = null;
    for (int i = -radiusX; i <= radiusX; i++) {
      for (int j = -radiusY; j <= radiusY; j++) {
        if (i == 0 && j == 0) continue;
        Location tempLocation = new Location(myLocation.getX() + i, myLocation.getY() + j);
        ArrayList<WorldEntity.WorldEntityInfo> neighPosInfos = mySurroundings.getWorldInfoAt(i, j);
        if (neighPosInfos != null) {
          for (WorldEntity.WorldEntityInfo info : neighPosInfos) {
            if (info.getIdentifier().compareToIgnoreCase("Pacman") == 0) {
              //Ignore myself
            } else if (info.getIdentifier().compareToIgnoreCase("Wall") == 0) {
              //Its a wall, who cares!
            } else if (info.getIdentifier().compareToIgnoreCase("Point") == 0 ||
                info.getIdentifier().compareToIgnoreCase("Powerup") == 0) {
              //Remember where it is!
              float currPointDistance = myLocation.distanceTo(tempLocation);
              points.add(tempLocation);
            } else if (info.getIdentifier().compareToIgnoreCase("Ghost") == 0) {
              //Remember him!
              float currGhostDistance = myLocation.distanceTo(tempLocation);
              if (currGhostDistance < ghostDistance) {
                ghostDistance = currGhostDistance;
                ghostLocation = tempLocation;
              }
            } else {
              printStatus("I dont know what " + info.getIdentifier() + " is!");
            }
          }
        }
      }
    }

    //move toward the point
    //pick next if arrived
    if (targetLocation == myLocation) {
      targetLocation = points.iterator().next();
      targetDistance = myLocation.distanceTo(targetLocation);
      targetDuration = 0;
    }

    targetDuration++;

    //sticking with target too long -> got stuck
    //dont get stuck
    if (targetDuration > 10) {
      ArrayList<Location> pointList = new ArrayList<>(points);
      int choice = r.nextInt(pointList.size());

      targetLocation = pointList.get(choice);
      targetDistance = myLocation.distanceTo(targetLocation);
      targetDuration = 0;
    }

    //select move
    float currMinPDistance = Float.MAX_VALUE;
    Location nextLocation = myLocation;
    int moveIndex = 0;

    for (int i = moves.size() - 1; i >= 0; i--) {
      int[] move = moves.get(i);
      Location moveLocation = new Location(myLocation.getX() + move[0], myLocation.getY() + move[1]);
      float newPDistance = moveLocation.distanceTo(targetLocation);
      float newGDistance = (ghostDistance < Float.MAX_VALUE) ? moveLocation.distanceTo(ghostLocation) : Float.MAX_VALUE;
      if (newPDistance <= currMinPDistance && newGDistance > 1) {
        //that way
        currMinPDistance = newPDistance;
        nextLocation = moveLocation;
        moveIndex = i;
      }
    }

    points.remove(myLocation);
    myLocation = nextLocation;
    points.remove(myLocation);

    return moveIndex;
  }
}
