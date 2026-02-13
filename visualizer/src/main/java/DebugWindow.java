import processing.core.PApplet;

import java.awt.*;

public class DebugWindow extends PApplet {
    Main main;
    boolean isVisible = false;

    public DebugWindow(Main main) {
        this.main = main;
    }

    public void settings() {
        size(200, 250);
    }

    public void draw() {
        if (!isVisible) return;
        background(100);
        textSize(14);
        text("Listening on port: " + main.listenPort, 20, 30);
        text("Last Address: " + main.lastAddress, 20, 50);
        text("Protocol: " + main.lastProtocol, 20, 70);
        text("Length: " + main.lastLength, 20, 90);
        text("Source IP: " + main.lastSrcIp, 20, 110);
        text("Dest IP: " + main.lastDstIp, 20, 130);
        text("Packet NO: " + main.lastNumber, 20, 150);
        text("Particle Count: " + main.particles.size(), 20, 170);
        text("Particle Total Count: " + main.counter, 20, 190);
        text("Frame Rate: " + String.format("%.2f", main.frameRate) + " fps", 20, 210);
    }

    public void hide() {
        isVisible = false;
        surface.setVisible(false);
        surface.setAlwaysOnTop(false);
        noLoop();
    }

    public void show() {
        if(main.fullScreenMode){
            GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            int targetDisplayEnv = Integer.parseInt(main.dotenv.get("DEBUG_DISPLAY", "1"));
            int targetIndex = targetDisplayEnv - 1;
            if (targetIndex >= 0 && targetIndex < devices.length) {
                Rectangle bounds = devices[targetIndex].getDefaultConfiguration().getBounds();
                surface.setLocation(bounds.x + 50, bounds.y + 50);
            }
        }else{
            GraphicsConfiguration config = MouseInfo.getPointerInfo().getDevice().getDefaultConfiguration();
            Rectangle bounds = config.getBounds();
            surface.setLocation(bounds.x + 50, bounds.y + 50);
        }
        isVisible = true;
        surface.setVisible(true);
        surface.setAlwaysOnTop(true);
        loop();
    }

    @Override
    public void keyPressed() {
        if (key == 'e' || key == 'E') {
            exit();
        }
        if (key == 'c' || key == 'C') {
            main.counter = 0;
        }
        if (key == 'd' || key == 'D') {
            main.onToggleDebug();
        }
    }

    @Override
    public void exitActual() {
        this.dispose();
    }
}