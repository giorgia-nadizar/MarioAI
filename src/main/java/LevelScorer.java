import engine.core.MarioGame;
import engine.core.MarioGymGame;
import engine.core.MarioResult;
import py4j.GatewayServer;

public class LevelScorer {

    public static void main(String[] args) {
        LevelScorer levelScorer = new LevelScorer();
        GatewayServer server = new GatewayServer(levelScorer);
        System.out.println("Server starting...");
        server.start();
    }


    public MarioResult score(String level) {
        MarioGame game = new MarioGame();
        return game.runGame(new agents.robinBaumgarten.Agent(), level, 20, 0, false);
    }


}
