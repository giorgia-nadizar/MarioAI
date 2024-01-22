package engine.core;

import engine.helper.MarioActions;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.Format;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rational;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import static engine.helper.GameStatus.LOSE;
import static engine.helper.GameStatus.RUNNING;

public class MarioGymGame extends MarioGame {

    private String level;
    private final ArrayList<MarioEvent> gameEvents;
    private final ArrayList<MarioAgentEvent> agentEvents;
    private final int actionSpace = MarioActions.numberOfActions();
    private final int observationSpace = 16 * 16;
    private boolean visual;
    private VisualRecord visualData;
    private List<BufferedImage> images;


    public int getActionSpace() {
        return actionSpace;
    }

    public int getObservationSpace() {
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


    public MarioGymGame() {
        this(null);
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
            enableVisual();
        }
    }

    public void enableVisual() {
        this.images = new ArrayList<>();
        this.visual = true;
        float scale = 2;
        this.window = new JFrame("Mario AI Framework");
        this.render = new MarioRender(scale);
        this.window.setContentPane(this.render);
        this.window.pack();
        this.window.setResizable(false);
        this.window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.render.init();
        this.window.setVisible(true);
    }

    public void disableVisual() {
        this.visual = false;
    }

    public void make(String level) throws IOException {
        if (level.indexOf("lvl") > 0) {
            this.level = new String(Files.readAllBytes(Paths.get("./levels/original/" + level)));
        } else {
            this.level = level;
        }
    }

    public ResetObject reset() {
        // TODO some default values which we will change later
        int marioState = 0;

        this.world = new MarioWorld(this.killEvents);
        this.world.visuals = visual;
        this.world.initializeLevel(level, 10000000);
        if (visual) {
            this.world.initializeVisuals(this.render.getGraphicsConfiguration());
            this.images.clear();
            this.window.setSize(500, 500);
        }
        this.world.mario.isLarge = marioState > 0;
        this.world.mario.isFire = marioState > 1;
        this.world.update(new boolean[MarioActions.numberOfActions()]);

        // reset events
        gameEvents.clear();
        agentEvents.clear();

        //initialize graphics
        if (visual) {
            VolatileImage renderTarget = this.render.createVolatileImage(MarioGame.width, MarioGame.height);
            Graphics backBuffer = this.render.getGraphics();
            Graphics currentBuffer = renderTarget.getGraphics();
            visualData = new VisualRecord(renderTarget, backBuffer, currentBuffer);
            this.render.addFocusListener(this.render);


            BufferedImage bufferedImage = new BufferedImage(
                    renderTarget.getWidth(), renderTarget.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            try {
                g2d.drawImage(renderTarget, 0, 0, null);
            } finally {
                g2d.dispose();
            }
            images.add(bufferedImage);
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
            BufferedImage bufferedImage = new BufferedImage(
                    visualData.renderTarget.getWidth(), visualData.renderTarget.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = bufferedImage.createGraphics();
            try {
                g2d.drawImage(visualData.renderTarget, 0, 0, null);
            } finally {
                g2d.dispose();
            }
            images.add(bufferedImage);
        }

        MarioResult currentResult = new MarioResult(world, gameEvents, agentEvents);
        double completionPercentage = currentResult.getCompletionPercentage();
        boolean done = currentResult.getGameStatus() != RUNNING;
        boolean dead = currentResult.getGameStatus() == LOSE;

        int[][] observation = new MarioForwardModel(this.world.clone()).getMarioCompleteObservation();
        return new StepObject(convertObservation(observation), completionPercentage, done, dead, null);

    }


    public void saveVideo(double frameRate, String fileName) throws IOException {
        File file = new File(fileName);
        SeekableByteChannel channel = NIOUtils.writableChannel(file);
        SequenceEncoder encoder =
                new SequenceEncoder(
                        channel,
                        Rational.R((int) Math.round(frameRate), 1),
                        Format.MOV,
                        org.jcodec.common.Codec.H264,
                        null);
        // encode
        try {
            for (BufferedImage image : images) {
                Picture picture = Picture.create(image.getWidth(), image.getHeight(), ColorSpace.RGB);
                bufImgToPicture(image, picture);
                encoder.encodeNativeFrame(picture);
            }
        } catch (IOException ex) {
            System.out.printf("Cannot encode image due to %s", ex);
        }
        encoder.finish();
        NIOUtils.closeQuietly(channel);
    }


    private static void bufImgToPicture(BufferedImage src, Picture dst) {
        byte[] dstData = dst.getPlaneData(0);
        int off = 0;
        for (int i = 0; i < src.getHeight(); i++) {
            for (int j = 0; j < src.getWidth(); j++) {
                int rgb1 = src.getRGB(j, i);
                int alpha = (rgb1 >> 24) & 0xff;
                if (alpha == 0xff) {
                    dstData[off++] = (byte) (((rgb1 >> 16) & 0xff) - 128);
                    dstData[off++] = (byte) (((rgb1 >> 8) & 0xff) - 128);
                    dstData[off++] = (byte) ((rgb1 & 0xff) - 128);
                } else {
                    int nalpha = 255 - alpha;
                    dstData[off++] = (byte) (((((rgb1 >> 16) & 0xff) * alpha + 0xff * nalpha) >> 8) - 128);
                    dstData[off++] = (byte) (((((rgb1 >> 8) & 0xff) * alpha + 0xff * nalpha) >> 8) - 128);
                    dstData[off++] = (byte) ((((rgb1 & 0xff) * alpha + 0xff * nalpha) >> 8) - 128);
                }
            }
        }
    }


    private void saveVideoFF(
            double frameRate, String fileName, int compression) throws IOException {
        File file = new File(fileName);
        // save all files
        String workingDirName = file.getAbsoluteFile().getParentFile().getPath();
        String imagesDirName = workingDirName + File.separator + "imgs." + System.currentTimeMillis();
        Files.createDirectories(Path.of(imagesDirName));
        List<Path> toDeletePaths = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            File imageFile =
                    new File(imagesDirName + File.separator + String.format("frame%06d", i) + ".jpg");
            ImageIO.write(images.get(i), "jpg", imageFile);
            toDeletePaths.add(imageFile.toPath());
        }
        toDeletePaths.add(Path.of(imagesDirName));
        // invoke ffmpeg
        String command =
                String.format(
                        "ffmpeg -y -r %d -i %s/frame%%06d.jpg -vcodec libx264 -crf %d -pix_fmt yuv420p %s",
                        (int) Math.round(frameRate), imagesDirName, compression, file.getPath());
        ProcessBuilder pb = new ProcessBuilder(command.split(" "));
        pb.directory(new File(workingDirName));
        StringBuilder sb = new StringBuilder();
        try {
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            int exitVal = process.waitFor();
            if (exitVal < 0) {
                throw new IOException(
                        String.format("Unexpected exit val: %d. Full output is:%n%s", exitVal, sb));
            }
        } catch (IOException | InterruptedException e) {
            throw (e instanceof IOException) ? (IOException) e : (new IOException(e));
        } finally {
            // delete all files
            for (Path path : toDeletePaths) {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    System.out.println("Cannot delete file.");
                }
            }
        }
    }
}

