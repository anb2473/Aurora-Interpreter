package Display.Cameras;

import org.jetbrains.annotations.NotNull;

import java.awt.event.KeyEvent;

public class FirstPersonCamera extends CameraBase {
    double upwardForce = 0;
    boolean isJump = false;

    @Override
    public void update(boolean @NotNull [] keys){
        if (keys[KeyEvent.VK_A]){
            x += Math.cos(Math.toRadians(-yAxis)) * speed;
            z -= Math.sin(Math.toRadians(-yAxis)) * speed;
        }
        if (keys[KeyEvent.VK_D]){
            x -= Math.cos(Math.toRadians(-yAxis)) * speed;
            z += Math.sin(Math.toRadians(-yAxis)) * speed;
        }
        if (keys[KeyEvent.VK_W]){
            x += Math.sin(Math.toRadians(yAxis)) * speed;
            z -= Math.cos(Math.toRadians(yAxis)) * speed;
        }
        if (keys[KeyEvent.VK_S]){
            x -= Math.sin(Math.toRadians(yAxis)) * speed;
            z += Math.cos(Math.toRadians(yAxis)) * speed;
        }

        if (keys[KeyEvent.VK_SPACE] && !isJump){
            upwardForce = 5;
            isJump = true;
        }

        if (isJump){
            upwardForce -= 0.05;
            if (upwardForce < -4.95){
                upwardForce = 0;
                isJump = false;
            }
        }

        y += upwardForce;

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
