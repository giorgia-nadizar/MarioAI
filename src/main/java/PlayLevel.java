import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import engine.core.MarioGame;
import engine.core.MarioResult;

public class PlayLevel {
    public static void printResults(MarioResult result) {
        System.out.println("****************************************************************");
        System.out.println("Game Status: " + result.getGameStatus().toString() +
                " Percentage Completion: " + result.getCompletionPercentage());
        System.out.println("Lives: " + result.getCurrentLives() + " Coins: " + result.getCurrentCoins() +
                " Remaining Time: " + (int) Math.ceil(result.getRemainingTime() / 1000f));
        System.out.println("Mario State: " + result.getMarioMode() +
                " (Mushrooms: " + result.getNumCollectedMushrooms() + " Fire Flowers: " + result.getNumCollectedFireflower() + ")");
        System.out.println("Total Kills: " + result.getKillsTotal() + " (Stomps: " + result.getKillsByStomp() +
                " Fireballs: " + result.getKillsByFire() + " Shells: " + result.getKillsByShell() +
                " Falls: " + result.getKillsByFall() + ")");
        System.out.println("Bricks: " + result.getNumDestroyedBricks() + " Jumps: " + result.getNumJumps() +
                " Max X Jump: " + result.getMaxXJump() + " Max Air Time: " + result.getMaxJumpAirTime());
        System.out.println("****************************************************************");
    }

    public static String getLevel(String filepath) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (IOException e) {
        }
        return content;
    }

    private static String LEVEL = "----------------------------------------------------------------------------------------------------\n" +
            "----------------------------------------------------------------------------------------------------\n" +
            "----------------------------------------------------------------------------------------------------\n" +
            "----------------------------------------------------------------------------------------------------\n" +
            "---------------------------------------E------------------------------------------------------------\n" +
            "-----------------------------------SSSSSSSSSS-------------------------------------------------------\n" +
            "----------------------------------------------------------------------------------------------------\n" +
            "----------------------------------------------------------------------------------------------------\n" +
            "B--------------------------------------------------X------------------------------------------------\n" +
            "b------------------------------------xxx---SSSSSSSSX-------------------------xxx--------------------\n" +
            "----?QQ-----------------------------xxX-x-----------------------------------xxX-x-------------------\n" +
            "x------------------xxx------xxxxx--xx-X--x-------------------------------B-xx-X--x------------------\n" +
            "-xxxxxxxxxxxxxxxxxxx--xxxxxxx----xxx--X---xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx--X---xxxxxxxxxxxxxxxxxx\n" +
            "XXXXXXXXXXXXXXXXXXXX--XXXXXXX--X-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX--XXXXXXXXXXXXXXXXXXXXXX";

    public static void main(String[] args) {
        MarioGame game = new MarioGame();
        // printResults(game.playGame(getLevel("./levels/original/lvl-trial.txt"), 200, 0));
        printResults(game.runGame(new agents.robinBaumgarten.Agent(), LEVEL, 20, 0, true));
    }
}
