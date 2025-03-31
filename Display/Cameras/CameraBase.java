package Display.Cameras;

import org.jetbrains.annotations.NotNull;

public class CameraBase {
    public double x;
    public double y;
    public double z = 0;

    public double xAxis;
    public double yAxis;
    public double zAxis = 0;

    public int speed = 6;

    public void update(boolean @NotNull [] keys){}
}
