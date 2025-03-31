package Display.MeshObjects;

import java.awt.*;
import java.util.Arrays;
import java.util.LinkedList;

public class RectangleMesh {
    public Mesh3D mesh;

    public RectangleMesh(int x, int y, int z, int width, int height, int depth, int res, Color color){
        LinkedList<Triangle> tris = new LinkedList<>();

        int faceNorthWidth = (int) Math.ceil((double) width / res);
        int faceNorthHeight = (int) Math.ceil((double) height / res);
        int faceEastWidth = (int) Math.ceil((double) depth / res);
        int faceEastHeight = (int) Math.ceil((double) height / res);
        int faceTopWidth = (int) Math.ceil((double) width / res);
        int faceTopHeight = (int) Math.ceil((double) depth / res);
        for (int face = 0; face < 6; face++)
            for (int faceX = 0; faceX < res; faceX++)
                for (int faceY = 0; faceY < res; faceY++)
                    if (face == 0)
                        tris.addAll(buildNorthSouthFace(x + faceX * faceNorthWidth, y + faceY * faceNorthHeight, z, faceNorthWidth, faceNorthHeight, color));
                    else if (face == 1)
                        tris.addAll(buildEastWestFace(x + width, y + faceY * faceEastHeight, z + faceX * faceEastWidth, faceEastHeight, faceEastWidth, color));
                    else if (face == 2)
                        tris.addAll(buildNorthSouthFace(x + faceX * faceNorthWidth, y + faceY * faceNorthHeight, z + depth, faceNorthWidth, faceNorthHeight, color));
                    else if (face == 3)
                        tris.addAll(buildEastWestFace(x, y + faceY * faceEastHeight, z + faceX * faceEastWidth, faceEastHeight, faceEastWidth, color));
                    else if (face == 4)
                        tris.addAll(buildTopBottomFace(x + faceX * faceTopWidth, y, z + faceY * faceTopHeight, faceTopWidth, faceTopHeight, color));
                    else
                        tris.addAll(buildTopBottomFace(x + faceX * faceTopWidth, y + height, z + faceY * faceTopHeight, faceTopWidth, faceTopHeight, color));

        mesh = new Mesh3D(Arrays.copyOf(tris.toArray(), tris.size(), Triangle[].class));
    }

    public LinkedList<Triangle> buildNorthSouthFace(int x, int y, int z, int width, int height, Color color){
        LinkedList<Triangle> result = new LinkedList<>();
        result.add(new Triangle(x, y, z, x + width, y, z, x + width, y + height, z, color));
        result.add(new Triangle(x, y, z, x, y + height, z, x + width, y + height, z, color));
        return result;
    }

    public LinkedList<Triangle> buildEastWestFace(int x, int y, int z, int height, int depth, Color color){
        LinkedList<Triangle> result = new LinkedList<>();
        result.add(new Triangle(x, y, z, x, y + height, z, x, y + height, z + depth, color));
        result.add(new Triangle(x, y, z, x, y, z + depth, x, y + height, z + depth, color));
        return result;
    }

    public LinkedList<Triangle> buildTopBottomFace(int x, int y, int z, int width, int depth, Color color){
        LinkedList<Triangle> result = new LinkedList<>();
        result.add(new Triangle(x, y, z, x, y, z + depth, x + width, y, z, color));
        result.add(new Triangle(x, y, z + depth, x + width, y, z, x + width, y, z + depth, color));
        return result;
    }
}
