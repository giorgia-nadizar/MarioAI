package engine.core;

import engine.helper.MarioActions;

import javax.swing.*;
import java.awt.*;
import java.awt.image.VolatileImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import static engine.helper.GameStatus.LOSE;
import static engine.helper.GameStatus.RUNNING;

public class MarioGymGame extends MarioGame {

    private final String level;
    private final ArrayList<MarioEvent> gameEvents;
    private final ArrayList<MarioAgentEvent> agentEvents;
    private final int actionSpace = MarioActions.numberOfActions();
    private final int observationSpace = 16 * 16;
    private final boolean visual;
    private VisualRecord visualData;


    public int getActionSpace() {
        return actionSpace;
    }

    public int getObservationSpace(){
        return observationSpace;
    }

    private List<List<Integer>> convertObservation(int[][] observation) {
        List<List<Integer>> listObservation = new ArrayList<>();
        for (int[] observationInstance : observation) {
            List<Integer> observationInstanceList = new ArrayList<>();
            for (int i : observationInstance) {
                observationInstanceList.add(i);
            }
            listObservation.add(observationInstanceList);
        }
        return listObservation;
    }

    public record ResetObject(List<List<Integer>> observation, Map<String, Object> information) {
    }

    public record StepObject(List<List<Integer>> observation, double reward, boolean terminated, boolean truncated,
                             Map<String, Object> information) {
    }

    private record VisualRecord(VolatileImage renderTarget, Graphics backBuffer, Graphics currentBuffer) {
    }


    public MarioGymGame(String level) {
        this(level, false);
    }

    public MarioGymGame(String level, boolean visual) {
        this.level = level;
        this.gameEvents = new ArrayList<>();
        this.agentEvents = new ArrayList<>();
        this.visual = visual;
        if (visual) {
            float scale = 2;
            this.window = new JFrame("Mario AI Framework");
            this.render = new MarioRender(scale);
            this.window.setContentPane(this.render);
            this.window.pack();
            this.window.setResizable(false);
            this.window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.render.init();
            this.window.setVisible(true);
        }
    }

    public ResetObject reset() {
        // TODO some default values which we will change later
        int timer = 10000;
        int marioState = 0;

        this.world = new MarioWorld(this.killEvents);
        this.world.visuals = visual;
        this.world.initializeLevel(level, 1000 * timer);
        if (visual) {
            this.world.initializeVisuals(this.render.getGraphicsConfiguration());
        }
        this.world.mario.isLarge = marioState > 0;
        this.world.mario.isFire = marioState > 1;
        this.world.update(new boolean[MarioActions.numberOfActions()]);

        //initialize graphics
        VolatileImage renderTarget = null;
        Graphics backBuffer = null;
        Graphics currentBuffer = null;
        if (visual) {
            renderTarget = this.render.createVolatileImage(MarioGame.width, MarioGame.height);
            backBuffer = this.render.getGraphics();
            currentBuffer = renderTarget.getGraphics();
            visualData = new VisualRecord(renderTarget, backBuffer, currentBuffer);
            this.render.addFocusListener(this.render);
        }

        int[][] observation = new MarioForwardModel(this.world.clone()).getMarioCompleteObservation();
        List<List<Integer>> listObservation = convertObservation(observation);
        return new ResetObject(listObservation, null);

    }

    public StepObject step(List<Boolean> actionList) {

        if (!this.pause) {
            boolean[] actions = new boolean[actionList.size()];
            for (int i = 0; i < actionList.size(); i++) {
                actions[i] = actionList.get(i);
            }

            // update world
            this.world.update(actions);
            gameEvents.addAll(this.world.lastFrameEvents);
            agentEvents.add(new MarioAgentEvent(actions, this.world.mario.x,
                    this.world.mario.y, (this.world.mario.isLarge ? 1 : 0) + (this.world.mario.isFire ? 1 : 0),
                    this.world.mario.onGround, this.world.currentTick));
        }

        //render world
        if (visual) {
            this.render.renderWorld(this.world, visualData.renderTarget, visualData.backBuffer, visualData.currentBuffer);
        }

        MarioResult currentResult = new MarioResult(world, gameEvents, agentEvents);
        double completionPercentage = currentResult.getCompletionPercentage();
        boolean done = currentResult.getGameStatus() != RUNNING;
        boolean dead = currentResult.getGameStatus() == LOSE;

        int[][] observation = new MarioForwardModel(this.world.clone()).getMarioCompleteObservation();
        return new StepObject(convertObservation(observation), completionPercentage, done, dead, null);

    }


}
