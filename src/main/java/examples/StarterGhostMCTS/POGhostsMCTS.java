package examples.StarterGhostMCTS;

import com.fossgalaxy.object.annotations.ObjectDef;
import pacman.controllers.Controller;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.game.Game;

import java.util.EnumMap;
import java.util.Random;

import static pacman.game.Constants.*;

/**
 * Created by Piers on 15/02/2016.
 */
public class POGhostsMCTS extends MASController {

    @ObjectDef("POG")
    public POGhostsMCTS() {
        super(true, new EnumMap<GHOST, IndividualGhostController>(GHOST.class));
        controllers.put(GHOST.BLINKY, new POGhostMCTS(GHOST.BLINKY));
        controllers.put(GHOST.INKY, new POGhostMCTS(GHOST.INKY));
        controllers.put(GHOST.PINKY, new POGhostMCTS(GHOST.PINKY));
        controllers.put(GHOST.SUE, new POGhostMCTS(GHOST.SUE));
    }

    @Override
    public String getName() {
        return "POG";
    }
}

