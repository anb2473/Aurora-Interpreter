package Display.MeshObjects;

import java.awt.*;
import java.util.Vector;

public class Triangle {
    public Vector<Integer> verticesA = new Vector<>();

    public Vector<Integer> verticesB = new Vector<>();

    public Vector<Integer> verticesC = new Vector<>();

    public Color color;

    public Triangle(int Ax, int Ay, int Az, int Bx, int By, int Bz, int Cx, int Cy, int Cz, Color color){
        this.color = color;

        verticesA.add(Ax);
        verticesA.add(Ay);
        verticesA.add(Az);

        verticesB.add(Bx);
        verticesB.add(By);
        verticesB.add(Bz);

        verticesC.add(Cx);
        verticesC.add(Cy);
        verticesC.add(Cz);
    }
}
