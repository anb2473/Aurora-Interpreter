package Display.MeshObjects;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;

public class TriangularPyramidMesh {
    public Mesh3D mesh;

    public TriangularPyramidMesh(int x, int y, int z, int height, int w1, int w2, int w3, int res, Color color) {
        LinkedList<Triangle> tris = new LinkedList<>();

        tris.add(new Triangle(x, y, z, x + w3 / 3, y + height, z - (w2 / 3) , x, y + height, z + (w1 / 3), color));
        tris.add(new Triangle(x, y, z, x - w3 / 3, y + height, z - (w2 / 3) , x, y + height, z + (w1 / 3), color));
        tris.add(new Triangle(x, y, z, x - w3 / 3, y + height, z + (w1 / 3), x - w3 / 3, y + height, z + (w1 / 3), color));
        tris.add(new Triangle(x + w3 / 3, y + height, z- (w2 / 3), x - w3 / 3, y + height, z - (w2 / 3), x, y + height, z + (w1 / 3), color));

        mesh = new Mesh3D(Arrays.copyOf(tris.toArray(), tris.size(), Triangle[].class));
    }
}
