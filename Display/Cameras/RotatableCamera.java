package Display.Cameras;

import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;

public class RotatableCamera extends CameraBase {
    @Override
    public void update(boolean @NotNull [] keys){
        if (keys[KeyEvent.VK_LEFT]){
            yAxis += 0.25 * speed;
        }
        if (keys[KeyEvent.VK_RIGHT]){
            yAxis -= 0.25 * speed;
        }
        if (keys[KeyEvent.VK_UP]){
            zAxis += 0.7 * speed;
        }
        if (keys[KeyEvent.VK_DOWN]){
            zAxis -= 0.7 * speed;
        }
    }
}
