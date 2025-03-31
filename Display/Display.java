package Display;

import Display.Cameras.CameraBase;
import Display.Cameras.FirstPersonCamera;
import Display.Cameras.RotatableCamera;
import Display.Cameras.StationairyCamera;
import Display.Input.Keys;
import Display.Input.Mouse;
import Display.MeshObjects.Mesh3D;
import Display.MeshObjects.Triangle;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.*;

public class Display {
    private JFrame frame;
    private Canvas canvas;

    private final int width,  height;

    private Mouse mouseListener;
    private Keys keyListener;

    public BufferStrategy BS;
    private Graphics2D g;

    private final Image iconImage;

    private String title = "";

    Mesh3D combinedMesh;

    CameraBase camera;
    LinkedList<Mesh3D> meshes = new LinkedList<>();
    HashMap<String, Integer> tags = new HashMap<>();
    HashMap<Mesh3D, String> tagQuerier = new HashMap<>();

    int fov = 300;

    public int focus_range = 18;

    BufferedImage background;

    int sortTick = 0;

    //<editor-fold desc="Display initiation">
    public Display(String title, int width, int height, String iconImage, boolean resizable, boolean subWindow) throws Exception {
        this.width = width;
        this.height = height;
        this.iconImage = loadImage(iconImage);
        if (title != null){
            this.title = title;
        }

        CreateDisplay(subWindow, resizable);

        generateBackground();
    }

    public Display(String title, int width, int height, BufferedImage iconImage, boolean resizable, boolean subWindow) throws Exception {
        this.width = width;
        this.height = height;
        this.iconImage = iconImage;
        if (title != null){
            this.title = title;
        }

        CreateDisplay(subWindow, resizable);

        generateBackground();
    }

    public Display(String title, int width, int height, String iconImage,  boolean resizable) throws Exception {
        this.width = width;
        this.height = height;
        this.iconImage = loadImage(iconImage);
        if (title != null){
            this.title = title;
        }

        CreateDisplay(false, resizable);

        generateBackground();
    }

    public Display(String title, int width, int height, BufferedImage iconImage, boolean resizable) throws Exception {
        this.width = width;
        this.height = height;
        this.iconImage = iconImage;
        if (title != null){
            this.title = title;
        }

        CreateDisplay(false, resizable);

        generateBackground();
    }

    public Display(String title, int width, int height, String iconImage) throws Exception {
        this.width = width;
        this.height = height;
        this.iconImage = loadImage(iconImage);
        if (title != null){
            this.title = title;
        }

        CreateDisplay(false, true);

        generateBackground();
    }

    public Display(String title, int width, int height, BufferedImage iconImage) throws Exception {
        this.width = width;
        this.height = height;
        this.iconImage = iconImage;
        if (title != null){
            this.title = title;
        }

        CreateDisplay(false, true);

        generateBackground();
    }
    //</editor-fold>

    public void generateBackground(){
        background = new BufferedImage(getFrameWidth(), getFrameHeight(), BufferedImage.TYPE_INT_RGB);

        g = background.createGraphics();

        g.setColor(new Color(80, 100, 200));
        g.fillRect(0, 0, getFrameWidth(), getFrameHeight());
        g.setColor(new Color(80, 104, 202));
        g.fillRect(0, getFrameHeight() / 2 - 100, getFrameWidth(), getFrameHeight());
        g.setColor(new Color(80, 108, 204));
        g.fillRect(0, getFrameHeight() / 2 - 95, getFrameWidth(), getFrameHeight());
        g.setColor(new Color(80, 112, 206));
        g.fillRect(0, getFrameHeight() / 2 - 90, getFrameWidth(), getFrameHeight());
        g.setColor(new Color(80, 116, 208));
        g.fillRect(0, getFrameHeight() / 2 - 85, getFrameWidth(), getFrameHeight());
        g.setColor(new Color(80, 120, 210));
        g.fillRect(0, getFrameHeight() / 2 - 75, getFrameWidth(), getFrameHeight());
        g.setColor(new Color(80, 124, 212));
        g.fillRect(0, getFrameHeight() / 2 - 65, getFrameWidth(), getFrameHeight());
        g.setColor(new Color(80, 128, 214));
        g.fillRect(0, getFrameHeight() / 2 - 55, getFrameWidth(), getFrameHeight());
        g.setColor(new Color(80, 132, 216));
        g.fillRect(0, getFrameHeight() / 2 - 35, getFrameWidth(), getFrameHeight());
        g.setColor(new Color(80, 136, 218));
        g.fillRect(0, getFrameHeight() / 2 - 15, getFrameWidth(), getFrameHeight());
        g.setColor(new Color(80, 140, 220));
        g.fillRect(0, getFrameHeight() / 2 - 5, getFrameWidth(), getFrameHeight());
    }

    private void CreateDisplay(boolean subWindow, boolean resizable) throws IOException {
        frame = new JFrame();

        frame.setSize(width, height);

        mouseListener = new Mouse();
        keyListener = new Keys();

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (subWindow) {
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        }

        frame.setResizable(resizable);

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);

        frame.setIconImage(iconImage);

        frame.setTitle(title);

        canvas = new Canvas();

        frame.setPreferredSize(new Dimension(width, height));

        canvas.setFocusable(false);

        frame.add(canvas);

        frame.addMouseListener(mouseListener);
        frame.addMouseMotionListener(mouseListener);
        frame.addKeyListener(keyListener);

        canvas.addMouseListener(mouseListener);
        canvas.addMouseMotionListener(mouseListener);
        canvas.addKeyListener(keyListener);

        frame.pack();
    }

    //<editor-fold desc="Graphics Managers">
    //<editor-fold desc="Graphics initiation">
    public Graphics2D graphicsInit(){
        BS = canvas.getBufferStrategy();

        if (BS == null) {
            canvas.createBufferStrategy(3);
            BS = canvas.getBufferStrategy();
        }

        g = (Graphics2D) BS.getDrawGraphics();

        g.clearRect(0, 0, frame.getWidth(), frame.getHeight());

        return g;
    }

    public Graphics2D graphicsInit(Color color){
        BS = canvas.getBufferStrategy();

        if(BS == null) {
            canvas.createBufferStrategy(3);
            BS = canvas.getBufferStrategy();
        }

        g = (Graphics2D) BS.getDrawGraphics();

        g.clearRect(0, 0, frame.getWidth(), frame.getHeight());

        g.setColor(color);
        g.fillRect(0, 0, frame.getWidth(), frame.getHeight());

        return (Graphics2D) g;
    }
    //</editor-fold>

    public void render() {
        BS.show();
        g.dispose();
    }
    //</editor-fold>

    //<editor-fold desc="Alter display variables">
    public void changeTitle(String title){frame.setTitle(title);}

    public void changeIcon(String path) throws IOException {
        frame.setIconImage(loadImage(path));
    }

    public void changeIcon(BufferedImage img) throws IOException {
        frame.setIconImage(img);
    }

    public void changeMaxSize(int maxWidth, int maxHeight) throws IOException {
        frame.setMaximumSize(new Dimension(maxWidth, maxHeight));
    }

    public void changeMinSize(int minWidth, int minHeight) throws IOException {
        frame.setMinimumSize(new Dimension(minWidth, minHeight));
    }

    public void changeSize(int width, int height) throws IOException {
        frame.setSize(new Dimension(width, height));
    }
    //</editor-fold>

    //<editor-fold desc="Get info">
    public boolean mouseCollide(Rectangle rect){return mouseListener.mouseCollide(rect);}

    public int getMouseX(){return mouseListener.mouseX;}

    public int getMouseY(){return mouseListener.mouseY;}

    public boolean getMousePressed(){return mouseListener.mousePressed;}

    public boolean[] getKeys(){return keyListener.getKeys();}

    public Canvas getCanvas() {return canvas;}

    public JFrame getFrame() {return frame;}

    public int getFrameWidth(){return frame.getWidth();}

    public int getFrameHeight(){return frame.getHeight();}

    public Mouse getMouseListener(){return mouseListener;}
    //</editor-fold>

    //<editor-fold desc="Graphics tools">
    //<editor-fold desc="Draw graphics">
    public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3){
        g.drawPolygon(new int[] {x1, x2, x3}, new int[] {y1, y2, y3}, 3);
    }

    public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, Color color){
        g.setColor(color);
        g.drawPolygon(new int[] {x1, x2, x3}, new int[] {y1, y2, y3}, 3);
    }

    public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        g.drawPolygon(new int[] {x1, x2, x3}, new int[] {y1, y2, y3}, 3);
    }

    public void drawTriangle(int x1, int y1, int x2, int y2, int x3, int y3, Color color, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        g.setColor(color);
        g.drawPolygon(new int[] {x1, x2, x3}, new int[] {y1, y2, y3}, 3);
    }

    public void drawPolygon(int @NotNull [] xPoints, int @NotNull [] yPoints){
        if (xPoints.length != yPoints.length)
            throw new IllegalArgumentException("xPoints and yPoints must have the same length");

        g.drawPolygon(xPoints, yPoints, xPoints.length);
    }

    public void drawPolygon(int @NotNull [] xPoints, int @NotNull [] yPoints, Color color){
        g.setColor(color);
        if (xPoints.length != yPoints.length)
            throw new IllegalArgumentException("xPoints and yPoints must have the same length");

        g.drawPolygon(xPoints, yPoints, xPoints.length);
    }

    public void drawPolygon(int @NotNull [] xPoints, int @NotNull [] yPoints, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        if (xPoints.length != yPoints.length)
            throw new IllegalArgumentException("xPoints and yPoints must have the same length");

        g.drawPolygon(xPoints, yPoints, xPoints.length);
    }

    public void drawPolygon(int @NotNull [] xPoints, int @NotNull [] yPoints, Color color, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        g.setColor(color);
        if (xPoints.length != yPoints.length)
            throw new IllegalArgumentException("xPoints and yPoints must have the same length");

        g.drawPolygon(xPoints, yPoints, xPoints.length);
    }

    public void drawCircle(int x, int y, int radius){
        g.drawOval(x, y, radius, radius);
    }

    public void drawCircle(int x, int y, int radius, Color color){
        g.setColor(color);
        g.drawOval(x, y, radius, radius);
    }

    public void drawCircle(int x, int y, int radius, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        g.drawOval(x, y, radius, radius);
    }

    public void drawCircle(int x, int y, int radius, Color color, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        g.setColor(color);
        g.drawOval(x, y, radius, radius);
    }

    public void drawOval(int x, int y, int width, int height){
        g.drawOval(x, y, width, height);
    }

    public void drawOval(int x, int y, int width, int height, Color color){
        g.setColor(color);
        g.drawOval(x, y, width, height);
    }

    public void drawOval(int x, int y, int width, int height, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        g.drawOval(x, y, width, height);
    }

    public void drawOval(int x, int y, int width, int height, Color color, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        g.setColor(color);
        g.drawOval(x, y, width, height);
    }

    public void drawRect(int x, int y, int width, int height){
        g.drawRect(x, y, width, height);
    }

    public void drawRect(int x, int y, int width, int height, Color color){
        g.setColor(color);
        g.drawRect(x, y, width, height);
    }

    public void drawRect(int x, int y, int width, int height, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        g.drawRect(x, y, width, height);
    }

    public void drawRect(int x, int y, int width, int height, Color color, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        g.setColor(color);
        g.drawRect(x, y, width, height);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcSize, Color color){
        g.setColor(color);
        g.drawRoundRect(x, y, width, height, arcSize, arcSize);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight, Color color){
        g.setColor(color);
        g.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcSize, Color color, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        g.setColor(color);
        g.drawRoundRect(x, y, width, height, arcSize, arcSize);
    }

    public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight, Color color, int stroke_width){
        g.setStroke(new BasicStroke(stroke_width));
        g.setColor(color);
        g.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }

    public void drawImage(String img, int x, int y, int width, int height) throws IOException {
        g.drawImage(loadImage(img), x, y, width, height, null);
    }

    public void drawImage(BufferedImage img, int x, int y, int width, int height) throws IOException {
        g.drawImage(img, x, y, width, height, null);
    }

    //<editor-fold desc="Draw text">
    public void drawText(String text, int x, int y) throws IOException {
        g.drawString(text, x, y);
    }

    public void drawText(String text, int x, int y, Color color) throws IOException {
        g.setColor(color);
        g.drawString(text, x, y);
    }

    public void drawText(String text, int x, int y, int font_size) throws IOException {
        g.setFont(new Font(g.getFont().getFontName(), g.getFont().getStyle(), font_size));
        g.drawString(text, x, y);
    }

    public void drawText(String text, int x, int y, int font_size, Color color) throws IOException {
        g.setColor(color);
        g.setFont(new Font(g.getFont().getFontName(), g.getFont().getStyle(), font_size));
        g.drawString(text, x, y);
    }

    public void drawText(String text, int x, int y, String font_name) throws IOException {
        g.setFont(new Font(font_name, g.getFont().getStyle(), g.getFont().getSize()));
        g.drawString(text, x, y);
    }

    public void drawText(String text, int x, int y, String font_name, Color color) throws IOException {
        g.setColor(color);
        g.setFont(new Font(font_name, g.getFont().getStyle(), g.getFont().getSize()));
        g.drawString(text, x, y);
    }

    public void drawText(String text, int x, int y, int font_size, String font_name) throws IOException {
        g.setFont(new Font(font_name, g.getFont().getStyle(), font_size));
        g.drawString(text, x, y);
    }

    public void drawText(String text, int x, int y, int font_size, String font_name, Color color) throws IOException {
        g.setColor(color);
        g.setFont(new Font(font_name, g.getFont().getStyle(), font_size));
        g.drawString(text, x, y);
    }

    public void drawTextWithStyle(String text, int x, int y, int font_style) throws IOException {
        g.setFont(new Font(g.getFont().getFontName(), font_style, g.getFont().getSize()));
        g.drawString(text, x, y);
    }

    public void drawTextWithStyle(String text, int x, int y, int font_style, Color color) throws IOException {
        g.setColor(color);
        g.setFont(new Font(g.getFont().getFontName(), font_style, g.getFont().getSize()));
        g.drawString(text, x, y);
    }

    public void drawTextWithStyle(String text, int x, int y, String font_style) throws IOException {
        int style = Font.PLAIN;
        if (Objects.equals(font_style, "bold") || Objects.equals(font_style, "Bold") || Objects.equals(font_style, "BOLD")){
            style = Font.BOLD;
        }
        if (Objects.equals(font_style, "italic") || Objects.equals(font_style, "Italic") || Objects.equals(font_style, "ITALIC")){
            style = Font.ITALIC;
        }
        g.setFont(new Font(g.getFont().getFontName(), style, g.getFont().getSize()));
        g.drawString(text, x, y);
    }

    public void drawTextWithStyle(String text, int x, int y, String font_style, Color color) throws IOException {
        int style = Font.PLAIN;
        if (Objects.equals(font_style, "bold") || Objects.equals(font_style, "Bold") || Objects.equals(font_style, "BOLD")){
            style = Font.BOLD;
        }
        if (Objects.equals(font_style, "italic") || Objects.equals(font_style, "Italic") || Objects.equals(font_style, "ITALIC")){
            style = Font.ITALIC;
        }
        g.setColor(color);
        g.setFont(new Font(g.getFont().getFontName(), style, g.getFont().getSize()));
        g.drawString(text, x, y);
    }

    public void drawText(String text, int x, int y, int font_size, String font_name, int font_style) throws IOException {
        g.setFont(new Font(font_name, font_style, font_size));
        g.drawString(text, x, y);
    }

    public void drawText(String text, int x, int y, int font_size, String font_name, int font_style, Color color) throws IOException {
        g.setFont(new Font(font_name, font_style, font_size));
        g.setColor(color);
        g.drawString(text, x, y);
    }

    public void drawText(String text, int x, int y, int font_size, String font_name, String font_style) throws IOException {
        int style = Font.PLAIN;
        if (Objects.equals(font_style, "bold") || Objects.equals(font_style, "Bold") || Objects.equals(font_style, "BOLD")){
            style = Font.BOLD;
        }
        if (Objects.equals(font_style, "italic") || Objects.equals(font_style, "Italic") || Objects.equals(font_style, "ITALIC")){
            style = Font.ITALIC;
        }
        g.setFont(new Font(font_name, style, font_size));
        g.drawString(text, x, y);
    }

    public void drawText(String text, int x, int y, int font_size, String font_name, String font_style, Color color) throws IOException {
        g.setColor(color);
        int style = Font.PLAIN;
        if (Objects.equals(font_style, "bold") || Objects.equals(font_style, "Bold") || Objects.equals(font_style, "BOLD")){
            style = Font.BOLD;
        }
        if (Objects.equals(font_style, "italic") || Objects.equals(font_style, "Italic") || Objects.equals(font_style, "ITALIC")){
            style = Font.ITALIC;
        }
        g.setFont(new Font(font_name, style, font_size));
        g.drawString(text, x, y);
    }
    //</editor-fold>
    //</editor-fold>

    //<editor-fold desc="Fill graphics">
    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3){
        g.fillPolygon(new int[] {x1, x2, x3}, new int[] {y1, y2, y3}, 3);
    }

    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3, Color color){
        g.setColor(color);
        g.fillPolygon(new int[] {x1, x2, x3}, new int[] {y1, y2, y3}, 3);
    }

    public void fillPolygon(int @NotNull [] xPoints, int @NotNull [] yPoints){
        if (xPoints.length != yPoints.length)
            throw new IllegalArgumentException("xPoints and yPoints must have the same length");

        g.fillPolygon(xPoints, yPoints, xPoints.length);
    }

    public void fillPolygon(int @NotNull [] xPoints, int @NotNull [] yPoints, Color color){
        g.setColor(color);
        if (xPoints.length != yPoints.length)
            throw new IllegalArgumentException("xPoints and yPoints must have the same length");

        g.fillPolygon(xPoints, yPoints, xPoints.length);
    }

    public void fillCircle(int x, int y, int radius){
        g.fillOval(x, y, radius, radius);
    }

    public void fillCircle(int x, int y, int radius, Color color){
        g.setColor(color);
        g.fillOval(x, y, radius, radius);
    }

    public void fillOval(int x, int y, int width, int height){
        g.fillOval(x, y, width, height);
    }

    public void fillOval(int x, int y, int width, int height, Color color){
        g.setColor(color);
        g.fillOval(x, y, width, height);
    }

    public void fillRect(int x, int y, int width, int height){
        g.fillRect(x, y, width, height);
    }

    public void fillRect(int x, int y, int width, int height, Color color){
        g.setColor(color);
        g.fillRect(x, y, width, height);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcSize, Color color){
        g.setColor(color);
        g.fillRoundRect(x, y, width, height, arcSize, arcSize);
    }

    public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight, Color color){
        g.setColor(color);
        g.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }
    //</editor-fold>
    //</editor-fold>

    //<editor-fold desc="Static utils">
    public static BufferedImage loadImage(String path) throws IOException {
        try {
            return ImageIO.read(Objects.requireNonNull(Display.class.getResource(path)));
        } catch (IOException e) {
            throw new IOException("Failed to load image file: " + path);
        }
    }
    //</editor-fold>

    //<editor-fold desc="3D Resources">
    //<editor-fold desc="Draw Mesh">
    @Contract(pure = true)
    private @NotNull List<Triangle> sortMeshByZ(Triangle @NotNull [] tris, double xRad, double yRad, double zRad, int offsetX, int offsetY, int offsetZ) {
        LinkedList<Triangle> result = new LinkedList<>();

        HashMap<Triangle, Integer> averages = new HashMap<>();

        for (Triangle triangle : tris) {
            int Az3D = triangle.verticesA.getLast() + offsetZ;
            int Bz3D = triangle.verticesB.getLast() + offsetZ;
            int Cz3D = triangle.verticesC.getLast() + offsetZ;
            int Ay3D = triangle.verticesA.get(1) + offsetY;
            int By3D = triangle.verticesB.get(1) + offsetY;
            int Cy3D = triangle.verticesC.get(1) + offsetY;
            int Ax3D = triangle.verticesA.getFirst() + offsetX;
            int Bx3D = triangle.verticesB.getFirst() + offsetX;
            int Cx3D = triangle.verticesC.getFirst() + offsetX;

            int avg = Math.abs(Az3D) + Math.abs(Bz3D) + Math.abs(Cz3D) + Math.abs(Ay3D) + Math.abs(By3D) + Math.abs(Cy3D) + Math.abs(Ax3D) + Math.abs(Bx3D) + Math.abs(Cx3D);

            averages.put(triangle, avg);

            int indx = 0;
            boolean successful = false;
            for (Triangle other : result) {
                int otherAvg = averages.get(other);
                if (avg > otherAvg) {
                    result.add(indx, triangle);
                    successful = true;
                    break;
                }

                indx++;
            }

            if (!successful)
                result.add(triangle);
        }

        return result;
    }

    public void drawMeshObject(@NotNull Mesh3D mesh, int fov, int xAxis, int yAxis, int zAxis, int offsetX, int offsetY, int offsetZ) {
        double xRad = Math.toRadians(xAxis);
        double yRad = Math.toRadians(yAxis);
        double zRad = Math.toRadians(zAxis);

        double Ax3D, Ay3D, Az3D, Bx3D, By3D, Bz3D, Cx3D, Cy3D, Cz3D;

        List<Triangle> sorted = sortMeshByZ(mesh.tris, xRad, yRad, zRad, offsetX, offsetY, offsetZ);

        for (Triangle triangle : sorted){
            Az3D = Math.cos(yRad) * (triangle.verticesA.getLast() + offsetZ) - Math.sin(yRad) * (triangle.verticesA.getFirst() + offsetX);
            Bz3D = Math.cos(yRad) * (triangle.verticesB.getLast() + offsetZ) - Math.sin(yRad) * (triangle.verticesB.getFirst() + offsetX);
            Cz3D = Math.cos(yRad) * (triangle.verticesC.getLast() + offsetZ) - Math.sin(yRad) * (triangle.verticesC.getFirst() + offsetX);

            if (Az3D > 0 && Az3D < 5000 && Bz3D > 0 && Cz3D > 0) {
                Ax3D = Math.cos(yRad) * (triangle.verticesA.getFirst() + offsetX) + Math.sin(yRad) * (triangle.verticesA.getLast() + offsetZ);
                Ay3D = triangle.verticesA.get(1) + offsetY;
                Bx3D = Math.cos(yRad) * (triangle.verticesB.getFirst() + offsetX) + Math.sin(yRad) * (triangle.verticesB.getLast() + offsetZ);
                By3D = triangle.verticesB.get(1) + offsetY;
                Cx3D = Math.cos(yRad) * (triangle.verticesC.getFirst() + offsetX) + Math.sin(yRad) * (triangle.verticesC.getLast() + offsetZ);
                Cy3D = triangle.verticesC.get(1) + offsetY;

                int avg = (int) (((Math.abs(Az3D) + Math.abs(Bz3D) + Math.abs(Cz3D)) + ((Math.abs(Ay3D) + Math.abs(By3D) + Math.abs(Cy3D))) + ((Math.abs(Ax3D) + Math.abs(Bx3D) + Math.abs(Cx3D)))) / focus_range);

                g.setColor(new Color(Math.max(Math.min(triangle.color.getRed() - avg, 255), 0), Math.max(Math.min(triangle.color.getGreen() - avg, 255), 0), Math.max(Math.min(triangle.color.getBlue() - avg, 255), 0), triangle.color.getAlpha()));

                fillTriangle(
                        (int) ((Ax3D / Az3D) * fov) + getFrameWidth() / 2,
                        (int) ((Ay3D / Az3D) * fov) + getFrameHeight() / 2,
                        (int) ((Bx3D / Bz3D) * fov) + getFrameWidth() / 2,
                        (int) ((By3D / Bz3D) * fov) + getFrameHeight() / 2,
                        (int) ((Cx3D / Cz3D) * fov) + getFrameWidth() / 2,
                        (int) ((Cy3D / Cz3D) * fov) + getFrameHeight() / 2
                );
            }
        }
    }
    //</editor-fold>

    //<editor-fold desc="Camera creation">
    public void createFirstPersonCamera(){
        camera = new FirstPersonCamera();
    }

    public void createStationairyCamera(){
        camera = new StationairyCamera();
    }

    public void createRotatingCamera(){
        camera = new RotatableCamera();
    }

    public void changeCameraSpeed(int newSpeed){camera.speed = newSpeed;}
    //</editor-fold>

    //<editor-fold desc="Roster Editors">
    public void addMeshToRoster(Mesh3D mesh, String tag){
        tags.put(tag, meshes.size());
        meshes.add(mesh);
        tagQuerier.put(mesh, tag);
    }

    public void replaceMesh(Mesh3D mesh, String tag){
        int indx = tags.get(tag);
        tagQuerier.remove(meshes.get(indx));
        tagQuerier.put(mesh, tag);
        meshes.remove(indx);
        meshes.add(indx, mesh);
    }

    private @NotNull LinkedList<Mesh3D> sortByMesh(){
        HashMap<Mesh3D, Integer> sorted = new LinkedHashMap<>(100, 85, true);
        HashMap<Mesh3D, Integer> xMap = new LinkedHashMap<>(100, 85, true);
        LinkedList<Mesh3D> result = new LinkedList<>();

        for (Mesh3D mesh : meshes){
            int trisMid = (int) Math.ceil((double) mesh.tris.length / 2);

            int meshX = mesh.tris[trisMid].verticesA.getFirst();
            int meshY = mesh.tris[trisMid].verticesA.get(1);
            int meshZ = mesh.tris[trisMid].verticesA.getLast();

            if (meshY / (Math.abs(meshZ)) == -1)
                continue;

            int avg = meshX + meshY + meshZ;

            int indx = 0;
            boolean successful = false;
            for (Mesh3D other : result) {
                int otherAvg = sorted.get(other);

                if (camera.x > meshX || camera.x > xMap.get(other)){
                    avg = -avg;
                    otherAvg = -otherAvg;
                }

                if (avg < otherAvg) {
                    result.add(indx, mesh);
                    sorted.put(mesh, avg);
                    xMap.put(mesh, meshX);
                    successful = true;
                    break;
                }

                indx++;
            }

            if (!successful) {
                result.add(mesh);
                sorted.put(mesh, avg);
                xMap.put(mesh, meshX);
            }
        }

        tags.clear();

        int indx = 0;
        for (Mesh3D mesh : result) {
            tags.put(tagQuerier.get(mesh), indx);
            indx++;
        }

        return result;
    }

    public void clearRoster(){
        meshes.clear();
        tags.clear();
    }
    //</editor-fold>

    public void updateCameraByMesh() throws NoninvertibleTransformException {
        sortTick--;
        if (sortTick <= 0) {
            meshes = sortByMesh();
            sortTick = 10;
        }

        camera.update(getKeys());

        for (Mesh3D mesh : meshes)
            drawMeshObject(mesh, fov, (int) camera.xAxis, (int) camera.yAxis, (int) camera.zAxis, (int) camera.x, (int) camera.y, (int) camera.z);
    }
    //</editor-fold>

    public void killProgram(){
        frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
    }

    public Object getVal(String value){
        if (Objects.equals(value, "keys")){
            return getKeys();
        }

        if (value.equals("mousePressed")){
            return getMousePressed();
        }

        if (value.equals("width")){
            return getFrameWidth();
        }

        if (value.equals("height")){
            return getFrameHeight();
        }

        if (value.equals("mouseX")){
            return getMouseX();
        }

        if (value.equals("mouseY")){
            return getMouseY();
        }

        return null;
    }
}
