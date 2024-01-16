import engine.core.MarioGymGame;
import py4j.GatewayServer;


public class PlayPython {

    // https://www.py4j.org/getting_started.html

    public static void main(String[] args) {
        boolean visual = false;
        String levelString = PlayLevel.getLevel("./levels/original/lvl-1.txt");
        if (args.length > 0) {
            visual = Boolean.parseBoolean(args[0]);
            levelString = args[1];
        }

        // TODO pass these as params
        int nServers = 50;
        int startPort = 25000;

        for (int port = startPort; port < startPort + nServers; port++){
            MarioGymGame marioGymGame = new MarioGymGame(levelString, visual);
            GatewayServer server = new GatewayServer(marioGymGame, port);
            System.out.printf("Server starting on port %d...\n", port);
            server.start();
        }

    }


}
