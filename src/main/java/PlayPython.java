import engine.core.MarioGymGame;
import py4j.GatewayServer;


public class PlayPython {

    // https://www.py4j.org/getting_started.html

    public static void main(String[] args) {
        int nServers = 100;
        int startPort = 25000;
        if (args.length > 0) {
            startPort = Integer.parseInt(args[0]);
            nServers = Integer.parseInt(args[1]);
        }

        for (int port = startPort; port < startPort + nServers; port++) {
            MarioGymGame marioGymGame = new MarioGymGame();
            GatewayServer server = new GatewayServer(marioGymGame, port);
            System.out.printf("Server starting on port %d...\n", port);
            server.start();
        }

    }


}
