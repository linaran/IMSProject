package reimplement;

import mmaracic.gameaiframework.AgentAI;
import mmaracic.gameaiframework.PacmanVisibleWorld;
import mmaracic.gameaiframework.WorldEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

/**
 *
 */
public class GhostAI extends AgentAI {
  private Date now = new Date();
  private Random r = new Random(now.getTime());

  private int findClosest(ArrayList<int[]> moves, int[] pos) {
    int index = 0;
    int[] move = moves.get(0);
    float dist = Math.abs(pos[0] - move[0]) + Math.abs(pos[1] - move[1]);
    for (int i = 1; i < moves.size(); i++) {
      move = moves.get(i);
      float currDist = Math.abs(pos[0] - move[0]) + Math.abs(pos[1] - move[1]);
      if (currDist < dist) {
        dist = currDist;
        index = i;
      }
    }
    return index;
  }

  @Override
  public int decideMove(ArrayList<int[]> moves, PacmanVisibleWorld mySurroundings, WorldEntity.WorldEntityInfo myInfo) {
    int[] move = null;

    for (int i = -mySurroundings.getDimensionX() / 2; i <= mySurroundings.getDimensionX() / 2; i++) {
      for (int j = -mySurroundings.getDimensionY() / 2; j <= mySurroundings.getDimensionY() / 2; j++) {
        if (i == 0 && j == 0) continue;
        //find pacman
        ArrayList<WorldEntity.WorldEntityInfo> elements = mySurroundings.getWorldInfoAt(i, j);
        HashMap<Integer, Object> metaHash = mySurroundings.getWorldMetadataAt(i, j);
        if (elements != null && metaHash != null) {
          for (WorldEntity.WorldEntityInfo el : elements) {
            if (el.getIdentifier().compareToIgnoreCase("Pacman") == 0) {
              int index = findClosest(moves, new int[]{i, j});
              metaHash.clear();
              metaHash.put(myInfo.getID(), moves.get(index));
              return index;
            }
          }
          //Check if someone else found him
          if (!metaHash.isEmpty()) {
            for (Integer id : metaHash.keySet()) {
              if (id != myInfo.getID()) {
                move = (int[]) metaHash.remove(id);
                //printStatus(myInfo.getID()+": Found pacman trail left by ghost: "+id+"!");
              }
            }
          }
        }
      }
    }
    //Go where metadata pointed
    if (move != null) {
      for (int i = 0; i < moves.size(); i++) {
        int[] m = moves.get(i);
        if (m[0] == move[0] && m[1] == move[1]) {
          return i;
        }
      }
    }
    //Go random
    return r.nextInt(moves.size());
  }
}
