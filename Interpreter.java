import Display.Display;
import Display.MeshObjects.RectangleMesh;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.yaml.snakeyaml.Yaml;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.io.IOException;
import java.util.Objects;
import static com.jn.langx.util.ClassLoaders.getResourceAsStream;
import static java.lang.Math.ceil;
import static java.lang.Math.pow;

public class Interpreter {
    HashMap<String, Object> data = new HashMap<>();

    final String fileData;

    final String filePath;

    HashMap<String, LinkedList<Object>> functions = new HashMap<>();

    HashMap<String, LinkedHashMap<String, Object>> redefinedFunctionData = new HashMap<>();

    Random rand = new Random();

    HashMap<Display, Graphics2D> graphicsMap = new HashMap<>();

    private long oTime;

    public Interpreter(String filePath) throws Exception {
        System.out.println("Compiling: --------");
        System.out.println("Compilation request from stack          " + LocalDateTime.now());
        oTime = System.nanoTime();
        this.filePath = filePath;

        try{
            fileData = parseTxt(filePath);
        }catch(IOException io){
            throw new IOException("Failed to get file data at \"" + filePath + "\"");
        }

        System.out.println("Compiling: ##------");
        System.out.println("Successfully parsed file data           " + LocalDateTime.now() + "     " + (System.nanoTime() - oTime));
        oTime = System.nanoTime();

        parseFunctions();

        System.out.println("Compiling: ####----");
        System.out.println("Successfully extracted function data    " + LocalDateTime.now() + "     " + (System.nanoTime() - oTime));
        oTime = System.nanoTime();

        getStartData();

        System.out.println("Compiling: ######--");
        System.out.println("Successfully extracted global data      " + LocalDateTime.now() + "     " + (System.nanoTime() - oTime));
        oTime = System.nanoTime();

        buildFunctionModels();

        System.out.println("Compiling: ########");
        System.out.println("Successfully compiled function data     " + LocalDateTime.now() + "     " + (System.nanoTime() - oTime));
        oTime = System.nanoTime();

        System.out.println();
        System.out.println();
    }

    public void run() throws Exception {
        runFunc("update", new HashMap<>());
        System.out.println("Successfully completed system tick      " + LocalDateTime.now() + "     " + (System.nanoTime() - oTime));
        oTime = System.nanoTime();
    }

    public void runUntilCompletion() throws Exception {
        int repeatLimit = 0;
        do {
            runFunc("update", new HashMap<>());
            repeatLimit++;
            System.out.println("Successfully completed system tick      " + LocalDateTime.now() + "     " + (System.nanoTime() - oTime));
            oTime = System.nanoTime();
        } while (repeatLimit <= 100000);
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private void runFunc(String functionName, @NotNull HashMap<String, Object> localData) throws Exception {
        LinkedHashMap<String, Object> functionData = redefinedFunctionData.get(functionName);

         localData.putAll((HashMap<String, Object>) functionData.get("localData"));

        for (String opperation : functionData.keySet()){
            if (opperation.equals("localData")) continue;

            if (opperation.startsWith("call")){
                String callOn = (String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).get(0);
                LinkedList<Object> params = ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(0);

                if (redefinedFunctionData.containsKey(callOn)){
                    HashMap<String, Object> funcParams = new HashMap<>();
                    int paramIndx = 0;
                    LinkedList<String> requestedParams = (LinkedList<String>) redefinedFunctionData.get((String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).getFirst()).get("parameteres");
                    for (Object param : ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(0)){
                        paramIndx++;
                        if (param instanceof String){
                            funcParams.put(requestedParams.get(paramIndx), redefineValueForRuntime((String) param, localData));
                        }
                        else{
                            funcParams.put(requestedParams.get(paramIndx), param);
                        }
                    }
                    runFunc((String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).getFirst(), funcParams);
                }
                else if (data.containsKey(callOn))
                    if (data.get(callOn) instanceof Display){
                        String funcName = (String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).get(1);

                        if (Objects.equals(funcName, "init")){
                            Graphics2D g = (Graphics2D) ((Display) data.get(callOn)).graphicsInit();

                            graphicsMap.put(((Display) data.get(callOn)), g);
                        }

                        else if (Objects.equals(funcName, "render")) {
                            ((Display) data.get(callOn)).render();

                            graphicsMap.remove(((Display) data.get(callOn)));

                            continue;
                        }

                        Graphics2D g = graphicsMap.get(data.get(callOn));
                        if (g == null)
                            throw new IOException("Attempted call to " + funcName + " without initializing graphics");

                        if (Objects.equals(funcName, "fillRect")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(4), localData));

                            g.fillRect((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData));
                        }

                        else if (Objects.equals(funcName, "drawRect")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(4), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(5), localData)));

                            g.drawRect((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData));
                        }

                        else if (Objects.equals(funcName, "fillTriangle")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(6), localData));

                            g.fillPolygon(new int[] {(int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(4), localData)}, new int[] {(int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(3), localData), (int) redefineValueForRuntime((String) params.get(5), localData)}, 3);
                        }

                        else if (Objects.equals(funcName, "drawTriangle")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(6), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(7), localData)));

                            g.drawPolygon(new int[] {(int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(4), localData)}, new int[] {(int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(3), localData), (int) redefineValueForRuntime((String) params.get(5), localData)}, 3);
                        }

                        else if (Objects.equals(funcName, "drawLine")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(5), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(4), localData)));

                            g.drawLine((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData));
                        }

                        else if (Objects.equals(funcName, "fillPolygon")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(6), localData));

                            int[] xArray = new int[] {};
                            for (int x = 0; x < params.size() / 2; x++)
                                xArray[x] = (int) redefineValueForRuntime((String) params.get(x * 2), localData);

                            int[] yArray = new int[] {};
                            for (int y = 0; y < params.size() / 2; y++)
                                yArray[y] = (int) redefineValueForRuntime((String) params.get(y * 2), localData);

                            g.fillPolygon(xArray, yArray, params.size() / 2);
                        }

                        else if (Objects.equals(funcName, "drawPolygon")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(params.size() - 1), localData));

                            int[] xArray = new int[] {};
                            for (int x = 0; x < params.size() / 2; x++)
                                xArray[x] = (int) redefineValueForRuntime((String) params.get(x * 2), localData);

                            int[] yArray = new int[] {};
                            for (int y = 0; y < params.size() / 2; y++)
                                yArray[y] = (int) redefineValueForRuntime((String) params.get(y * 2), localData);

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.getLast(), localData)));

                            g.drawPolygon(xArray, yArray, params.size() / 2);
                        }

                        else if (Objects.equals(funcName, "fillRoundRect")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(6), localData));

                            g.fillRoundRect((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData), (int) redefineValueForRuntime((String) params.get(4), localData), (int) redefineValueForRuntime((String) params.get(5), localData));
                        }

                        else if (Objects.equals(funcName, "drawRoundRect")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(6), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(7), localData)));

                            g.drawRoundRect((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData), (int) redefineValueForRuntime((String) params.get(4), localData), (int) redefineValueForRuntime((String) params.get(5), localData));
                        }

                        else if (Objects.equals(funcName, "fillOval")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(4), localData));

                            g.fillOval((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData));
                        }

                        else if (Objects.equals(funcName, "drawOval")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(4), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(5), localData)));

                            g.drawOval((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData));
                        }

                        else if (Objects.equals(funcName, "fillCircle")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(3), localData));

                            g.fillOval((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(2), localData));
                        }

                        else if (Objects.equals(funcName, "drawCircle")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(3), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(4), localData)));

                            g.drawOval((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(2), localData));
                        }

                        else if (Objects.equals(funcName, "drawImage"))
                            g.drawImage((BufferedImage) redefineValueForRuntime((String) (params.get(0)), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData), (int) redefineValueForRuntime((String) params.get(4), localData), null);

                        else if (Objects.equals(funcName, "drawText")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(5), localData));
                            g.setFont(new Font((String) redefineValueForRuntime((String) (params.get(3)), localData), Font.PLAIN, (int) redefineValueForRuntime((String) (params.get(4)), localData)));
                            g.drawString((String) redefineValueForRuntime((String) (params.get(0)), localData), (int) redefineValueForRuntime((String) (params.get(1)), localData), (int) redefineValueForRuntime((String) (params.get(2)), localData));
                        }
                        
                        else if (Objects.equals(funcName, "addRectangularPrism")){
                            if (params.size() == 6)
                                ((Display) data.get(callOn)).addMeshToRoster(new RectangleMesh((int) redefineValueForRuntime((String) (params.get(0)), localData), (int) redefineValueForRuntime((String) (params.get(1)), localData), (int) redefineValueForRuntime((String) (params.get(2)), localData), (int) redefineValueForRuntime((String) (params.get(3)), localData), (int) redefineValueForRuntime((String) (params.get(4)), localData), (int) redefineValueForRuntime((String) (params.get(5)), localData), (int) redefineValueForRuntime((String) (params.get(6)), localData), new Color(255, 255, 255)).mesh, "Default-Tag");
                            if (params.size() == 6)
                                ((Display) data.get(callOn)).addMeshToRoster(new RectangleMesh((int) redefineValueForRuntime((String) (params.get(0)), localData), (int) redefineValueForRuntime((String) (params.get(1)), localData), (int) redefineValueForRuntime((String) (params.get(2)), localData), (int) redefineValueForRuntime((String) (params.get(3)), localData), (int) redefineValueForRuntime((String) (params.get(4)), localData), (int) redefineValueForRuntime((String) (params.get(5)), localData), (int) redefineValueForRuntime((String) (params.get(6)), localData), (Color) redefineValueForRuntime((String) (params.get(7)), localData)).mesh, "Default-Tag");
                        }

                        else if (Objects.equals(funcName, "clearRoster")){
                            ((Display) data.get(callOn)).clearRoster();
                        }
                    }
                    else if (data.get(callOn) instanceof HashMap){
                        String funcName = (String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).get(1);

                        if (Objects.equals(funcName, "put"))
                            ((HashMap<Object, Object>) data.get(callOn)).put(params.getFirst(), params.get(1));
                    }
                    else if (data.get(callOn) instanceof LinkedList){
                        String funcName = (String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).get(1);

                        if (Objects.equals(funcName, "put"))
                            ((LinkedList<Object>) data.get(callOn)).add(params.getFirst());
                    }
            }

            if (opperation.startsWith("if")){
                LinkedList<Object> conditional = (LinkedList<Object>) ((LinkedList<Object>) functionData.get(opperation)).getFirst();
                if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof Integer) {
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof String) {
                        if (conditional.getFirst() == ("<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof HashMap) {
                        if (conditional.getFirst() == ("<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) == ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof LinkedList) {
                        if (conditional.getFirst() == ("<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) == ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) != ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof String){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof String) {
                        if (conditional.getFirst() == ("<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).contains(((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            if (!((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).contains(((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() >= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof LinkedList){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof LinkedList) {
                        if (conditional.getFirst() == ("<")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).containsAll(((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            if (!((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).containsAll(((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) != ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof HashMap){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof HashMap) {
                        if (conditional.getFirst() == ("<")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).keySet().containsAll(((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).keySet())) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            if (!((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).keySet().containsAll(((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).keySet())) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                continue;
            }

            if (opperation.startsWith("+")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):"))
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        else if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                    }
                    if (first instanceof String)
                        if (first.startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            data.put(first, (Integer) data.get(((String) data.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) + (Integer) value);
                        else data.put(first, (Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) + (Integer) value);
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) + (Integer) value);
                        }
                    }
                    else {
                        localData.put(first, (Integer) localData.get(first) + (Integer) value);
                    }
                }
                continue;
            }

            if (opperation.startsWith("-")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):"))
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        else if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                    }
                    if (first instanceof String)
                        if (first.startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            data.put(first, (Integer) data.get(((String) data.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) - (Integer) value);
                        else data.put(first, (Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) - (Integer) value);
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) - (Integer) value);
                        }
                    }
                    else {
                        localData.put(first, (Integer) localData.get(first) - (Integer) value);
                    }
                }
                continue;
            }

            if (opperation.startsWith("for")){
                LinkedList<Object> conditional = (LinkedList<Object>) ((LinkedList<Object>) functionData.get(opperation)).getFirst();
                if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof Integer){
                    if (conditional.getFirst() == ("<") || conditional.getFirst() == ":") {
                        for (int i = 0; i < (int) redefineValueForRuntime(((LinkedList<String>) (conditional.getLast())).getLast(), localData); i++){
                            HashMap<String, Object> local = (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1);
                            local.put(((LinkedList<String>) (conditional.getLast())).getFirst(), i);
                            localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), local);
                        }
                    }
                    if (conditional.getFirst() == ("<=")) {
                        for (int i = 0; i <= (int) redefineValueForRuntime(((LinkedList<String>) (conditional.getLast())).getLast(), localData); i++){
                            HashMap<String, Object> local = (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1);
                            local.put(((LinkedList<String>) (conditional.getLast())).getFirst(), i);
                            localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), local);
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof LinkedList){
                    if (conditional.getFirst() == ":") {
                        for (Object obj : (LinkedList<Object>) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)){
                            HashMap<String, Object> local = (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1);
                            local.put(((LinkedList<String>) (conditional.getLast())).getFirst(), obj);
                            localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), local);
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof HashMap)
                    if (conditional.getFirst() == ":")
                        for (Object obj : ((HashMap<Object, Object>) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).values()){
                            HashMap<String, Object> local = (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1);
                            local.put(((LinkedList<String>) (conditional.getLast())).getFirst(), obj);
                            localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), local);
                        }
                continue;
            }

            if (opperation.startsWith("/")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):"))
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        else if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                    }
                    if (first instanceof String)
                        if (first.startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            data.put(first, (Integer) data.get(((String) data.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) / (Integer) value);
                    else data.put(first, (Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) / (Integer) value);
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) / (Integer) value);
                        }
                    }
                    else {
                        localData.put(first, (Integer) localData.get(first) / (Integer) value);
                    }
                }
                continue;
            }

            if (opperation.startsWith("*")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (data.get(first) instanceof String){
                        if (((String) data.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            data.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) * (Integer) value);
                        }
                    }
                    else {
                        data.put(first, (Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) * (Integer) value);
                    }
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) * (Integer) value);
                        }
                    }
                    else {
                        localData.put(first, (Integer) localData.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) * (Integer) value);
                    }
                }
                continue;
            }

            if (opperation.startsWith("while")){
                LinkedList<Object> conditional = (LinkedList<Object>) ((LinkedList<Object>) functionData.get(opperation)).getFirst();
                if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof Integer) {
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof String) {
                        if (conditional.getFirst() == ("<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof HashMap) {
                        if (conditional.getFirst() == ("<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) == ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof LinkedList) {
                        if (conditional.getFirst() == ("<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) == ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) != ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof String){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof String) {
                        if (conditional.getFirst() == ("<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).contains(((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            while (!((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).contains(((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() >= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof LinkedList){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof LinkedList) {
                        if (conditional.getFirst() == ("<")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).containsAll(((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            while (!((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).containsAll(((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">=")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() >= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) != ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof HashMap){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof HashMap) {
                        if (conditional.getFirst() == ("<")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).keySet().containsAll(((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).keySet())) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            while (!((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).keySet().containsAll(((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).keySet())) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() >= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                continue;
            }

            if (opperation.startsWith("%")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):"))
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        else if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                    }
                    if (first instanceof String)
                        if (first.startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            data.put(first, (Integer) data.get(((String) data.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) % (Integer) value);
                        else data.put(first, (Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) % (Integer) value);
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) % (Integer) value);
                        }
                    }
                    else {
                        localData.put(first, (Integer) localData.get(first) % (Integer) value);
                    }
                }
                continue;
            }

            if (opperation.startsWith("^")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):"))
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        else if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                    }
                    if (first instanceof String)
                        if (first.startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            data.put(first, Math.pow((Integer) data.get(((String) data.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")), (Integer) value));
                        else data.put(first, Math.pow((Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()), (Integer) value));
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, Math.pow((Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")), (Integer) value));
                        }
                    }
                    else {
                        localData.put(first, Math.pow((Integer) localData.get(first), (Integer) value));
                    }
                }
            }
        }
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private HashMap<String, Object> runBlock(@NotNull LinkedHashMap<String, Object> functionData, HashMap<String, Object> localData) throws Exception {
        for (String opperation : functionData.keySet()) {
            if (opperation.equals("localData")) continue;

            if (opperation.startsWith("call")){
                String callOn = (String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).get(0);
                LinkedList<Object> params = ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(0);

                if (redefinedFunctionData.containsKey(callOn)){
                    HashMap<String, Object> funcParams = new HashMap<>();
                    int paramIndx = 0;
                    LinkedList<String> requestedParams = (LinkedList<String>) redefinedFunctionData.get((String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).getFirst()).get("parameteres");
                    for (Object param : ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(0)){
                        paramIndx++;
                        if (param instanceof String){
                            funcParams.put(requestedParams.get(paramIndx), redefineValueForRuntime((String) param, localData));
                        }
                        else{
                            funcParams.put(requestedParams.get(paramIndx), param);
                        }
                    }
                    runFunc((String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).getFirst(), funcParams);
                }
                else if (data.containsKey(callOn))
                    if (data.get(callOn) instanceof Display){
                        String funcName = (String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).get(1);

                        if (Objects.equals(funcName, "init")){
                            Graphics2D g = (Graphics2D) ((Display) data.get(callOn)).graphicsInit();

                            graphicsMap.put(((Display) data.get(callOn)), g);
                        }

                        else if (Objects.equals(funcName, "render")) {
                            ((Display) data.get(callOn)).render();

                            graphicsMap.remove(((Display) data.get(callOn)));

                            continue;
                        }

                        Graphics2D g = graphicsMap.get(data.get(callOn));
                        if (g == null)
                            throw new IOException("Attempted call to " + funcName + " without initializing graphics");

                        if (Objects.equals(funcName, "fillRect")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(4), localData));

                            g.fillRect((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData));
                        }

                        else if (Objects.equals(funcName, "drawRect")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(4), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(5), localData)));

                            g.drawRect((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData));
                        }

                        else if (Objects.equals(funcName, "fillTriangle")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(6), localData));

                            g.fillPolygon(new int[] {(int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(4), localData)}, new int[] {(int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(3), localData), (int) redefineValueForRuntime((String) params.get(5), localData)}, 3);
                        }

                        else if (Objects.equals(funcName, "drawTriangle")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(6), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(7), localData)));

                            g.drawPolygon(new int[] {(int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(4), localData)}, new int[] {(int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(3), localData), (int) redefineValueForRuntime((String) params.get(5), localData)}, 3);
                        }

                        else if (Objects.equals(funcName, "drawLine")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(5), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(4), localData)));

                            g.drawLine((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData));
                        }

                        else if (Objects.equals(funcName, "fillPolygon")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(6), localData));

                            int[] xArray = new int[] {};
                            for (int x = 0; x < params.size() / 2; x++)
                                xArray[x] = (int) redefineValueForRuntime((String) params.get(x * 2), localData);

                            int[] yArray = new int[] {};
                            for (int y = 0; y < params.size() / 2; y++)
                                yArray[y] = (int) redefineValueForRuntime((String) params.get(y * 2), localData);

                            g.fillPolygon(xArray, yArray, params.size() / 2);
                        }

                        else if (Objects.equals(funcName, "drawPolygon")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(params.size() - 1), localData));

                            int[] xArray = new int[] {};
                            for (int x = 0; x < params.size() / 2; x++)
                                xArray[x] = (int) redefineValueForRuntime((String) params.get(x * 2), localData);

                            int[] yArray = new int[] {};
                            for (int y = 0; y < params.size() / 2; y++)
                                yArray[y] = (int) redefineValueForRuntime((String) params.get(y * 2), localData);

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.getLast(), localData)));

                            g.drawPolygon(xArray, yArray, params.size() / 2);
                        }

                        else if (Objects.equals(funcName, "fillRoundRect")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(6), localData));

                            g.fillRoundRect((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData), (int) redefineValueForRuntime((String) params.get(4), localData), (int) redefineValueForRuntime((String) params.get(5), localData));
                        }

                        else if (Objects.equals(funcName, "drawRoundRect")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(6), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(7), localData)));

                            g.drawRoundRect((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData), (int) redefineValueForRuntime((String) params.get(4), localData), (int) redefineValueForRuntime((String) params.get(5), localData));
                        }

                        else if (Objects.equals(funcName, "fillOval")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(4), localData));

                            g.fillOval((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData));
                        }

                        else if (Objects.equals(funcName, "drawOval")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(4), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(5), localData)));

                            g.drawOval((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData));
                        }

                        else if (Objects.equals(funcName, "fillCircle")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(3), localData));

                            g.fillOval((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(2), localData));
                        }

                        else if (Objects.equals(funcName, "drawCircle")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(3), localData));

                            g.setStroke(new BasicStroke((int) redefineValueForRuntime((String) params.get(4), localData)));

                            g.drawOval((int) redefineValueForRuntime((String) params.get(0), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(2), localData));
                        }

                        else if (Objects.equals(funcName, "drawImage"))
                            g.drawImage((BufferedImage) redefineValueForRuntime((String) (params.get(0)), localData), (int) redefineValueForRuntime((String) params.get(1), localData), (int) redefineValueForRuntime((String) params.get(2), localData), (int) redefineValueForRuntime((String) params.get(3), localData), (int) redefineValueForRuntime((String) params.get(4), localData), null);

                        else if (Objects.equals(funcName, "drawText")){
                            g.setColor((Color) redefineValueForRuntime((String) params.get(5), localData));
                            g.setFont(new Font((String) redefineValueForRuntime((String) (params.get(3)), localData), Font.PLAIN, (int) redefineValueForRuntime((String) (params.get(4)), localData)));
                            g.drawString((String) redefineValueForRuntime((String) (params.get(0)), localData), (int) redefineValueForRuntime((String) (params.get(1)), localData), (int) redefineValueForRuntime((String) (params.get(2)), localData));
                        }
                    }
                    else if (data.get(callOn) instanceof HashMap){
                        String funcName = (String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).get(1);

                        if (Objects.equals(funcName, "put"))
                            ((HashMap<Object, Object>) data.get(callOn)).put(params.getFirst(), params.get(1));
                    }
                    else if (data.get(callOn) instanceof LinkedList){
                        String funcName = (String) ((LinkedList<LinkedList<Object>>) (functionData.get(opperation))).get(1).get(1);

                        if (Objects.equals(funcName, "put"))
                            ((LinkedList<Object>) data.get(callOn)).add(params.getFirst());
                    }
            }

            if (opperation.startsWith("if")){
                LinkedList<Object> conditional = (LinkedList<Object>) ((LinkedList<Object>) functionData.get(opperation)).getFirst();
                if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof Integer) {
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof String) {
                        if (conditional.getFirst() == ("<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof HashMap) {
                        if (conditional.getFirst() == ("<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) == ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof LinkedList) {
                        if (conditional.getFirst() == ("<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) == ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) != ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof String){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof String) {
                        if (conditional.getFirst() == ("<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).contains(((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            if (!((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).contains(((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() >= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof LinkedList){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof LinkedList) {
                        if (conditional.getFirst() == ("<")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).containsAll(((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            if (!((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).containsAll(((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if (((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) != ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof HashMap){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof HashMap) {
                        if (conditional.getFirst() == ("<")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).keySet().containsAll(((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).keySet())) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            if (!((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).keySet().containsAll(((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).keySet())) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if (((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            if ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                continue;
            }

            if (opperation.startsWith("+")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):"))
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        else if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                    }
                    if (first instanceof String)
                        if (first.startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            data.put(first, (Integer) data.get(((String) data.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) + (Integer) value);
                        else data.put(first, (Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) + (Integer) value);
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) + (Integer) value);
                        }
                    }
                    else {
                        localData.put(first, (Integer) localData.get(first) + (Integer) value);
                    }
                }
                continue;
            }

            if (opperation.startsWith("-")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):"))
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        else if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                    }
                    if (first instanceof String)
                        if (first.startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            data.put(first, (Integer) data.get(((String) data.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) - (Integer) value);
                        else data.put(first, (Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) - (Integer) value);
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) - (Integer) value);
                        }
                    }
                    else {
                        localData.put(first, (Integer) localData.get(first) - (Integer) value);
                    }
                }
                continue;
            }

            if (opperation.startsWith("for")){
                LinkedList<Object> conditional = (LinkedList<Object>) ((LinkedList<Object>) functionData.get(opperation)).getFirst();
                if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof Integer){
                    if (conditional.getFirst() == ("<") || conditional.getFirst() == ":") {
                        for (int i = 0; i < (int) redefineValueForRuntime(((LinkedList<String>) (conditional.getLast())).getLast(), localData); i++){
                            HashMap<String, Object> local = (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1);
                            local.put(((LinkedList<String>) (conditional.getLast())).getFirst(), i);
                            localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), local);
                        }
                    }
                    if (conditional.getFirst() == ("<=")) {
                        for (int i = 0; i <= (int) redefineValueForRuntime(((LinkedList<String>) (conditional.getLast())).getLast(), localData); i++){
                            HashMap<String, Object> local = (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1);
                            local.put(((LinkedList<String>) (conditional.getLast())).getFirst(), i);
                            localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), local);
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof LinkedList){
                    if (conditional.getFirst() == ":") {
                        for (Object obj : (LinkedList<Object>) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)){
                            HashMap<String, Object> local = (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1);
                            local.put(((LinkedList<String>) (conditional.getLast())).getFirst(), obj);
                            localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), local);
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof HashMap)
                    if (conditional.getFirst() == ":")
                        for (Object obj : ((HashMap<Object, Object>) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).values()){
                            HashMap<String, Object> local = (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1);
                            local.put(((LinkedList<String>) (conditional.getLast())).getFirst(), obj);
                            localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), local);
                        }
                continue;
            }

            if (opperation.startsWith("/")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):"))
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        else if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                    }
                    if (first instanceof String)
                        if (first.startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            data.put(first, (Integer) data.get(((String) data.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) / (Integer) value);
                        else data.put(first, (Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) / (Integer) value);
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) / (Integer) value);
                        }
                    }
                    else {
                        localData.put(first, (Integer) localData.get(first) / (Integer) value);
                    }
                }
                continue;
            }

            if (opperation.startsWith("*")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (data.get(first) instanceof String){
                        if (((String) data.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            data.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) * (Integer) value);
                        }
                    }
                    else {
                        data.put(first, (Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) * (Integer) value);
                    }
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) * (Integer) value);
                        }
                    }
                    else {
                        localData.put(first, (Integer) localData.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) * (Integer) value);
                    }
                }
                continue;
            }

            if (opperation.startsWith("while")){
                LinkedList<Object> conditional = (LinkedList<Object>) ((LinkedList<Object>) functionData.get(opperation)).getFirst();
                if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof Integer) {
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof String) {
                        if (conditional.getFirst() == ("<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof HashMap) {
                        if (conditional.getFirst() == ("<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getFirst()).getFirst(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) == ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) <= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((HashMap<?, ?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof LinkedList) {
                        if (conditional.getFirst() == ("<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) == ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) <= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) >= ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) < ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) > ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) != ((LinkedList<?>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof String){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof String) {
                        if (conditional.getFirst() == ("<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).contains(((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            while (!((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).contains(((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() >= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() <= ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() > ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).length() < ((String) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)).length()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() == (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() <= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() >= (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() < (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() > (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while (((String) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).length() != (int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof LinkedList){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof LinkedList) {
                        if (conditional.getFirst() == ("<")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).containsAll(((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            while (!((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).containsAll(((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))))) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">=")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() >= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while (((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) != ((LinkedList<Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData) instanceof HashMap){
                    if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof HashMap) {
                        if (conditional.getFirst() == ("<")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (":")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).keySet().containsAll(((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).keySet())) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!:")) {
                            while (!((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).keySet().containsAll(((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).keySet())) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) == redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() >= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() <= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() > ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while (((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData))).size() < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while (Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData)) != redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData)) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                    else if (redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) instanceof Integer) {
                        if (conditional.getFirst() == ("<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == (">")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                        if (conditional.getFirst() == (">=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) == ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) >= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) <= ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!<=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) > ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!>=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        } else if (conditional.getFirst() == ("!=")) {
                            while ((int) redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getFirst(), localData) < ((HashMap<Object, Object>) Objects.requireNonNull(redefineValueForRuntime((String) ((LinkedList<Object>) conditional.getLast()).getLast(), localData))).size()) {
                                localData = runBlock((LinkedHashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1), (HashMap<String, Object>) ((LinkedList<Object>) functionData.get(opperation)).get(1));
                            }
                        }
                    }
                }
                continue;
            }

            if (opperation.startsWith("%")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):"))
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        else if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                    }
                    if (first instanceof String)
                        if (first.startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            data.put(first, (Integer) data.get(((String) data.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) % (Integer) value);
                        else data.put(first, (Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()) % (Integer) value);
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, (Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")) % (Integer) value);
                        }
                    }
                    else {
                        localData.put(first, (Integer) localData.get(first) % (Integer) value);
                    }
                }
                continue;
            }

            if (opperation.startsWith("^")){
                String first = (String) ((LinkedList<Object>)functionData.get(opperation)).getFirst();
                if (data.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):"))
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        else if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                    }
                    if (first instanceof String)
                        if (first.startsWith("\\global-query(EnsuranceID=980102.10AAimd019):"))
                            data.put(first, Math.pow((Integer) data.get(((String) data.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")), (Integer) value));
                        else data.put(first, Math.pow((Integer) data.get((String) ((LinkedList<Object>) functionData.get(opperation)).getFirst()), (Integer) value));
                    continue;
                }

                if (localData.containsKey(first)){
                    Object value = ((LinkedList<Object>)functionData.get(opperation)).get(1);
                    if (value instanceof String) {
                        if (((String) value).startsWith("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):")) {
                            value = localData.get(((String) value).replace("\\local-query(EnsuranceID=18Z2n9QA1948.1921aE7819):", ""));
                        }
                        if (((String) value).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            value = data.get(((String)value).replace("\\global-query(EnsuranceID=980102.10AAimd019):", ""));
                        }
                    }
                    if (localData.get(first) instanceof String){
                        if (((String) localData.get(first)).startsWith("\\global-query(EnsuranceID=980102.10AAimd019):")){
                            localData.put(first, Math.pow((Integer) data.get(((String) localData.get(first)).replace("\\global-query(EnsuranceID=980102.10AAimd019):", "")), (Integer) value));
                        }
                    }
                    else {
                        localData.put(first, Math.pow((Integer) localData.get(first), (Integer) value));
                    }
                }
            }
        }

        return localData;
    }

    //<editor-fold desc="Initializer">
    private void buildFunctionModels() throws Exception {
        for (String function : functions.keySet()){
            if (Objects.equals(function, "start"))
                continue;

            LinkedHashMap<String, Object> functionModel = buildMultiLinearOpperation((String) functions.get(function).getFirst());

            if (functions.get(function).size() == 2)
                functionModel.put("parameters", functions.get(function).getLast());

            redefinedFunctionData.put(function, functionModel);
        }
    }

    private @NotNull String getNewCallID(@NotNull HashMap<String, Object> functionModel, String functionName){
        float functionID = rand.nextFloat(-1000000000, 1000000000);
        if (functionModel.containsKey(functionName + ".function_call_id:" + functionID)){
            return getNewCallID(functionModel, functionName);
        }

        return functionName + ".function_call_id:" + functionID;
    }

    private @NotNull LinkedHashMap<String, Object> buildMultiLinearOpperation(@NotNull String data) throws Exception {
        LinkedHashMap<String, Object> functionModel = new LinkedHashMap<>();

        HashMap<String, Object> localData = new HashMap<>();

        LinkedList<String> listData = new LinkedList<>(Arrays.asList((data.split("\\s{2,}"))));
        for (int i = 0; i < listData.size(); i++) {
            var current = listData.get(i);

            if (current.contains("(")){
                LinkedList<String> parameters = new LinkedList<>(Arrays.asList(current.split("\\(")[1].replace(")", "").split(",")));
                if (parameters.getFirst().isEmpty()){
                    parameters.clear();
                }

                LinkedList<Object> callData = new LinkedList<>();
                callData.add(parameters);
                callData.add(new LinkedList<>(Arrays.asList(current.split("\\(")[0].split("\\."))));


                functionModel.put(getNewCallID(functionModel, "call"), callData);
                continue;
            }

            if (current.contains("=")) {
                localData.put(current.split("=")[0].strip(), redefineValue(current.split("=")[1].strip(), localData));
                continue;
            }

            if (current.contains("+")){
                LinkedList<Object> oppData = new LinkedList<>();
                if (current.split("\\+").length == 2) {
                    oppData.add(current.split("\\+")[0].strip());
                    oppData.add(redefineValue(current.split("\\+")[1].strip(), localData));
                }
                else oppData.add(current.replace("+", "").strip());

                functionModel.put(getNewCallID(functionModel, "+"), oppData);

                continue;
            }

            if (current.contains("-")){
                LinkedList<Object> oppData = new LinkedList<>();
                if (current.split("-").length == 2){
                    oppData.add(current.split("-")[0].strip());
                    oppData.add(redefineValue(current.split("-")[1].strip(), localData));
                }
                else oppData.add(current.replace("-", "").strip());

                functionModel.put(getNewCallID(functionModel, "-"), oppData);

                continue;
            }

            if (current.contains("*")){
                LinkedList<Object> oppData = new LinkedList<>();
                oppData.add(current.split("\\*")[0].strip());
                oppData.add(redefineValue(current.split("\\*")[1].strip(), localData));

                functionModel.put(getNewCallID(functionModel, "*"), oppData);

                continue;
            }

            if (current.contains("/")){
                LinkedList<Object> oppData = new LinkedList<>();
                oppData.add(current.split("/")[0].strip());
                oppData.add(redefineValue(current.split("/")[1].strip(), localData));

                functionModel.put(getNewCallID(functionModel, "/"), oppData);

                continue;
            }

            if (current.contains("%")){
                LinkedList<Object> oppData = new LinkedList<>();
                oppData.add(current.split("%")[0].strip());
                oppData.add(redefineValue(current.split("%")[1].strip(), localData));

                functionModel.put(getNewCallID(functionModel, "%"), oppData);

                continue;
            }

            if (current.contains("^")){
                LinkedList<Object> oppData = new LinkedList<>();
                oppData.add(current.split("\\^")[0].strip());
                oppData.add(redefineValue(current.split("\\^")[1].strip(), localData));

                functionModel.put(getNewCallID(functionModel, "^"), oppData);

                continue;
            }

            if (current.contains("if")){
                StringBuilder innerData = new StringBuilder();
                for (int g = i + 1; g < listData.size(); g++){
                    var inner = listData.get(g);
                    if (inner.contains("]")){
                        i = g + 1;
                        break;
                    }

                    innerData.append(inner);
                }

                LinkedHashMap<String, Object> statementData = buildMultiLinearOpperation(innerData.toString());
                statementData.remove("localData");

                LinkedList<Object> fullStatementData = new LinkedList<>();

                String opperation = current.replace("if", "").replace("[", "").strip();
                LinkedList<Object> opperationData = new LinkedList<>();
                if (opperation.contains("<")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("<")));
                    opperationData.add("<");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("<=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("<=")));
                    opperationData.add("<=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("=")));
                    opperationData.add("=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains(">")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split(">")));
                    opperationData.add(">");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains(">=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split(">=")));
                    opperationData.add(">=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains(":")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split(":")));
                    opperationData.add(":");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!<")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!<")));
                    opperationData.add("!<");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!<=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!<=")));
                    opperationData.add("!<=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!=")));
                    opperationData.add("!=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!>")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!>")));
                    opperationData.add("!>");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!>=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!>=")));
                    opperationData.add("!>=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!:")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!:")));
                    opperationData.add("!:");
                    opperationData.add(oppParts);
                }

                fullStatementData.add(opperationData);

                fullStatementData.add(statementData);

                functionModel.put(getNewCallID(functionModel, "if"), fullStatementData);

                continue;
            }

            if (current.contains("while")){
                StringBuilder innerData = new StringBuilder();
                for (int g = i + 1; g < listData.size(); g++){
                    var inner = listData.get(i);
                    if (inner.contains("]")){
                        i = g - 1;
                        break;
                    }

                    innerData.append(inner);
                }

                LinkedHashMap<String, Object> statementData = buildMultiLinearOpperation(innerData.toString());

                LinkedList<Object> fullStatementData = new LinkedList<>();

                String opperation = current.replace("while", "").replace("[", "").strip();
                LinkedList<Object> opperationData = new LinkedList<>();
                if (opperation.contains("<")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("<")));
                    opperationData.add("<");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("<=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("<=")));
                    opperationData.add("<=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("=")));
                    opperationData.add("=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains(">")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split(">")));
                    opperationData.add(">");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains(">=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split(">=")));
                    opperationData.add(">=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains(":")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split(":")));
                    opperationData.add(":");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!<")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!<")));
                    opperationData.add("!<");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!<=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!<=")));
                    opperationData.add("!<=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!=")));
                    opperationData.add("!=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!>")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!>")));
                    opperationData.add("!>");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!>=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!>=")));
                    opperationData.add("!>=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("!:")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("!:")));
                    opperationData.add("!:");
                    opperationData.add(oppParts);
                }

                fullStatementData.add(opperationData);

                fullStatementData.add(statementData);

                functionModel.put(getNewCallID(functionModel, "while"), fullStatementData);

                continue;
            }

            if (current.contains("for")){
                StringBuilder innerData = new StringBuilder();
                for (int g = i + 1; g < listData.size(); g++){
                    var inner = listData.get(i);
                    if (inner.contains("]")){
                        i = g - 1;
                        break;
                    }

                    innerData.append(inner);
                }

                LinkedHashMap<String, Object> statementData = buildMultiLinearOpperation(innerData.toString());

                LinkedList<Object> fullStatementData = new LinkedList<>();

                String opperation = current.replace("for", "").replace("[", "").strip();
                LinkedList<Object> opperationData = new LinkedList<>();
                if (opperation.contains("<")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("<")));
                    opperationData.add("<");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains("<=")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split("<=")));
                    opperationData.add("<=");
                    opperationData.add(oppParts);
                }
                else if (opperation.contains(":")){
                    LinkedList<String> oppParts = new LinkedList<>(Arrays.asList(opperation.split(":")));
                    opperationData.add(":");
                    opperationData.add(oppParts);
                }

                fullStatementData.add(opperationData);

                fullStatementData.add(statementData);

                functionModel.put(getNewCallID(functionModel, "for"), fullStatementData);
            }
        }
        functionModel.put("localData", localData);

        return functionModel;
    }

    @NotNull
    private void parseFunctions(){
        HashMap<String, LinkedList<Object>> functions = new HashMap<>();

        String[] stringListInterpolation = fileData.split("}");

        HashMap<String, String> functionLiteralInterpolation = new HashMap<>();
        for (String function : stringListInterpolation)
            functionLiteralInterpolation.put(function.split("\\{")[0].strip(), function.split("\\{")[1].strip());

        for (String functionBase : functionLiteralInterpolation.keySet()){
            LinkedList<Object> finalFunctionData = new LinkedList<>();
            finalFunctionData.add(functionLiteralInterpolation.get(functionBase));

            String[] bases = functionBase.split("\\(", 2);

            LinkedList<String> parameterData = new LinkedList<>(Arrays.asList(bases[1].strip().split(",")));
            parameterData.add(parameterData.getLast().replace(")", "").strip());
            parameterData.remove(parameterData.size() - 2);

            if (!Objects.equals(parameterData.getFirst(), ""))
                finalFunctionData.add(parameterData);

            functions.put(bases[0].strip(), finalFunctionData);
        }

        this.functions = functions;
    }

    @Contract(pure = true)
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    private @Nullable @Unmodifiable Object redefineValue(String value, HashMap<String, Object> localData) throws Exception {
        value = value.strip();

        if (value.equals("true"))
            return true;

        if (value.equals("false"))
            return false;

        try{
            return Integer.parseInt(value);
        }catch (NumberFormatException _){}

        if (value.startsWith("\\")){
            if (value.endsWith(".yaml"))
                return parseYaml(value);
            if (value.endsWith(".json"))
                return parseJson(value);
            if (value.endsWith(".txt"))
                return parseTxt(value);
            if (value.endsWith(".png") || value.endsWith(".jpg"))
                return loadImage(value);
            if (value.endsWith(".btm"))
                return createBTMImage(value, 500, 500);
            if (value.endsWith(".wav"))
                return value.replace("\\", "");
            throw new Exception("Unrecognized file format: " + value);
        }

        String start = Character.toString(value.charAt(0));

        if (start.equals("#") || value.startsWith("0x"))
            return Color.decode(value);

        if (start.equals("\""))
            return value.replace("\"", "");

        String shortenedValue = value.substring(1).substring(0, value.length() - 1);
        switch (start) {
            case "(" -> {
                LinkedList<String> list = new LinkedList<>(Arrays.asList(shortenedValue.replace(")", "").split(",")));

                LinkedList<Object> redefinedList = new LinkedList<>();
                for (String listValue : list)
                    redefinedList.add(redefineValueForRuntime(listValue, localData));

                return redefinedList;
            }

            case "<" -> {
                String[] listedValues = shortenedValue.replace(">", "").split(",");

                HashMap<String, Object> hashMap = new HashMap<>();
                for (String listedValue : listedValues)
                    hashMap.put(listedValue.split("=")[0], redefineValue(listedValue.split("=")[1], localData));

                return hashMap;
            }
        }

        if (value.startsWith("Aurora")) {
            String[] callSections = value.split("\\.");

            String[] args = value.substring(0, value.length() - 1).split("\\(")[1].split(",");

            if (callSections.length < 2)
                throw new IllegalArgumentException("Invalid reference to Aurora library: " + value);
            callSections[2] = callSections[2].split("\\(")[0];

            if (Objects.equals(callSections[1], "display"))
                if (Objects.equals(callSections[2], "new"))
                    switch (args.length) {
                        case 3: return new Display((String) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData), loadImage("/NullIcon.png"), false, true);
                        case 4: return new Display((String) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[2], localData), (int) redefineValueForRuntime(args[3], localData), (BufferedImage) redefineValueForRuntime(args[1], localData), false, true);
                        case 5: return new Display((String) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData), (BufferedImage) redefineValueForRuntime(args[3], localData), false, (boolean) redefineValueForRuntime(args[4], localData));
                        case 6: return new Display((String) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData), (BufferedImage) redefineValueForRuntime(args[3], localData), (boolean) redefineValueForRuntime(args[5], localData), (boolean) redefineValueForRuntime(args[4], localData));
                    }

            if (Objects.equals(callSections[1], "audio"))
                if (Objects.equals(callSections[1], "playWav"))
                    return playWav((String) redefineValueForRuntime(args[0], localData));

            if (Objects.equals(callSections[1], "image"))
                switch (callSections[1]) {
                    case "blur":         return blur((BufferedImage) redefineValueForRuntime(args[0], localData));
                    case "heavyBlur":    return heavyBlur((BufferedImage) redefineValueForRuntime(args[0], localData));
                    case "detectEdges":  return detectEdges((BufferedImage) redefineValueForRuntime(args[0], localData));
                    case "smoothResize": return smoothResize((BufferedImage) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData));
                    case "resize":       return resizeImage((BufferedImage) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData));
                    case "rotate":       return rotate((BufferedImage) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData));
                    case "brighten":     return brighten((BufferedImage) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData));
                    case "pixelate":     return pixelate((BufferedImage) redefineValueForRuntime(args[0], localData));
                    case "toGray":       return convertToGrayScale((BufferedImage) redefineValueForRuntime(args[0], localData));
                    case "setAlpha":     return setAlpha((BufferedImage) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData));
                }

            if (Objects.equals(callSections[1], "math"))
                switch (callSections[2]) {
                    case "cos":         return Math.cos((double) redefineValueForRuntime(args[0], localData));
                    case "sin":         return Math.sin((double) redefineValueForRuntime(args[0], localData));
                    case "ln":          return Math.log((double) redefineValueForRuntime(args[0], localData));
                    case "floor":       return Math.floor((double) redefineValueForRuntime(args[0], localData));
                    case "ceil":        return Math.ceil((double) redefineValueForRuntime(args[0], localData));
                    case "log10":       return Math.log10((double) redefineValueForRuntime(args[0], localData));
                    case "log1p":       return Math.log1p((double) redefineValueForRuntime(args[0], localData));
                    case "abs":         return Math.abs((double) redefineValueForRuntime(args[0], localData));
                    case "log":         return Math.log((double) redefineValueForRuntime(args[1], localData)) / Math.log((int) redefineValueForRuntime(args[0], localData));
                    case "tan":         return Math.tan((double) redefineValueForRuntime(args[0], localData));
                    case "round":       return Math.round((double) redefineValueForRuntime(args[0], localData));
                    case "acos":        return Math.acos((double) redefineValueForRuntime(args[0], localData));
                    case "asin":        return Math.asin((double) redefineValueForRuntime(args[0], localData));
                    case "cosh":        return Math.cosh((double) redefineValueForRuntime(args[0], localData));
                    case "sinh":        return Math.sinh((double) redefineValueForRuntime(args[0], localData));
                    case "tanh":        return Math.tanh((double) redefineValueForRuntime(args[0], localData));
                    case "atan":        return Math.atan((double) redefineValueForRuntime(args[0], localData));
                    case "atan2":       return Math.atan2((double) redefineValueForRuntime(args[0], localData), (double) redefineValueForRuntime(args[0], localData));
                    case "randInt":     return rand.nextInt((int) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[0], localData));
                    case "randBool":    return rand.nextBoolean();
                    case "randDouble":  return rand.nextDouble((int) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[0], localData));
                    case "toDeg":       return Math.toDegrees((double) redefineValueForRuntime(args[0], localData));
                    case "toRad":       return Math.toRadians((double) redefineValueForRuntime(args[0], localData));
                    case "sqrt":        return Math.sqrt((double) redefineValueForRuntime(args[0], localData));
                    case "pi":          return Math.PI;
                    case "tau":         return Math.TAU;
                    case "e":           return Math.E;
                }

            if (Objects.equals(callSections[1], "color"))
                switch (callSections[2]) {
                    case "new":
                        if (args.length == 3)
                            return new Color((int) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData));
                        if (args.length == 4)
                            return new Color((int) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData), (int) redefineValueForRuntime(args[3], localData));
                    case "white":       return Color.WHITE;
                    case "black":       return Color.BLACK;
                    case "red":         return Color.RED;
                    case "yellow":      return Color.YELLOW;
                    case "orange":      return Color.ORANGE;
                    case "blue":        return Color.BLUE;
                    case "magenta":     return Color.MAGENTA;
                    case "cyan":        return Color.CYAN;
                    case "green":       return Color.GREEN;
                    case "gray":        return Color.GRAY;
                    case "light_gray":  return Color.LIGHT_GRAY;
                    case "dark_gray":   return Color.DARK_GRAY;
                    case "pink":        return Color.PINK;
                }
        }

        if ((value).contains(".")){;
            if (localData.containsKey(( value).split("\\.")[0])){
                if (localData.get((value).split("\\.")[0]) instanceof Display)
                    return ((Display) localData.get((value).split("\\.")[0])).getVal((value).split("\\.")[1]);
                if (localData.get((value).split("\\.")[0]) instanceof HashMap){
                    String val = (value).split("\\.")[1];
                    if (Objects.equals(val, "size"))
                        return ((HashMap<Object, Object>) localData.get((value).split("\\.")[0])).size();

                    return ((HashMap<Object, Object>) localData.get((value).split("\\.")[0])).get(val);
                }
                if (localData.get((value).split("\\.")[0]) instanceof LinkedList){
                    String val = (value).split("\\.")[1];
                    if (Objects.equals(val, "size"))
                        return ((LinkedList<Object>) localData.get((value).split("\\.")[0])).size();
                    try{
                        return ((LinkedList<Object>) localData.get((value).split("\\.")[0])).get(Integer.parseInt(val) - 1);
                    } catch (NumberFormatException _){}
                }
            }
            if (data.containsKey((value).split("\\.")[0])){
                if (data.get((value).split("\\.")[0]) instanceof Display)
                    return ((Display) data.get((value).split("\\.")[0])).getVal((value).split("\\.")[1]);
                if (data.get((value).split("\\.")[0]) instanceof HashMap){
                    String val = (value).split("\\.")[1];
                    if (Objects.equals(val, "size"))
                        return ((HashMap<Object, Object>) data.get((value).split("\\.")[0])).size();
                    if (Objects.equals(val, "keys"))
                        return ((HashMap<Object, Object>) data.get((value).split("\\.")[0])).keySet();
                    if (Objects.equals(val, "values"))
                        return ((HashMap<Object, Object>) data.get((value).split("\\.")[0])).values();

                    return ((HashMap<Object, Object>) data.get((value).split("\\.")[0])).get(val);
                }
                if (data.get((value).split("\\.")[0]) instanceof LinkedList){
                    String val = (value).split("\\.")[1];
                    if (Objects.equals(val, "size"))
                        return ((LinkedList<Object>) data.get((value).split("\\.")[0])).size();
                    try{
                        return ((LinkedList<Object>) data.get((value).split("\\.")[0])).get(Integer.parseInt(val) - 1);
                    } catch (NumberFormatException _){}
                }
            }
            try{
                return Double.parseDouble(value);
            }catch (NumberFormatException _){}
        }

        if (data.get(value) != null)
            return "\\global-query(EnsuranceID=980102.10AAimd019):" + value;

        if (localData.get(value) != null)
            return "\\local-query(EnsuranceID=10g19U02n091.2401h1098):" + value;

        throw new IOException("Failed to interpret value: " + value);
    }

    @Contract(pure = true)
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private @Nullable @Unmodifiable Object redefineValueForRuntime(String value, HashMap<String, Object> localData) throws Exception {
        value = value.strip();

        if (value.equals("true"))
            return true;

        if (value.equals("false"))
            return false;

        try{
            return Integer.parseInt(value);
        }catch (NumberFormatException _){}

        if (value.startsWith("\\")){
            if (value.endsWith(".yaml"))
                return parseYaml(value);
            if (value.endsWith(".json"))
                return parseJson(value);
            if (value.endsWith(".txt"))
                return parseTxt(value);
            if (value.endsWith(".png") || value.endsWith(".jpg"))
                return loadImage(value);
            if (value.endsWith(".btm"))
                return createBTMImage(value, 500, 500);
            if (value.endsWith(".wav"))
                return value.replace("\\", "");
            throw new Exception("Unrecognized file format: " + value);
        }

        String start = Character.toString(value.charAt(0));

        if (start.equals("#") || value.startsWith("0x"))
            return Color.decode(value);

        if (start.equals("\""))
            return value.replace("\"", "");

        String shortenedValue = value.substring(1).substring(0, value.length() - 1);
        switch (start) {
            case "(" -> {
                LinkedList<String> list = new LinkedList<>(Arrays.asList(shortenedValue.replace(")", "").split(",")));

                LinkedList<Object> redefinedList = new LinkedList<>();
                for (String listValue : list)
                    redefinedList.add(redefineValueForRuntime(listValue, localData));

                return redefinedList;
            }

            case "<" -> {
                String[] listedValues = shortenedValue.replace(">", "").split(",");

                HashMap<String, Object> hashMap = new HashMap<>();
                for (String listedValue : listedValues)
                    hashMap.put(listedValue.split("=")[0], redefineValue(listedValue.split("=")[1], localData));

                return hashMap;
            }
        }

        if (value.startsWith("Aurora")) {
            String[] callSections = value.split("\\.");

            String[] args = value.substring(0, value.length() - 1).split("\\(")[1].split(",");

            if (callSections.length < 2)
                throw new IllegalArgumentException("Invalid reference to Aurora library: " + value);
            callSections[2] = callSections[2].split("\\(")[0];

            if (Objects.equals(callSections[1], "display"))
                if (Objects.equals(callSections[2], "new"))
                    switch (args.length) {
                        case 3: return new Display((String) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData), loadImage("/NullIcon.png"), false, true);
                        case 4: return new Display((String) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[2], localData), (int) redefineValueForRuntime(args[3], localData), (BufferedImage) redefineValueForRuntime(args[1], localData), false, false);
                        case 5: return new Display((String) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData), (BufferedImage) redefineValueForRuntime(args[3], localData), (boolean) redefineValueForRuntime(args[4], localData), false);
                        case 6: return new Display((String) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData), (BufferedImage) redefineValueForRuntime(args[3], localData), (boolean) redefineValueForRuntime(args[4], localData), (boolean) redefineValueForRuntime(args[5], localData));
                    }

            if (Objects.equals(callSections[1], "audio"))
                if (Objects.equals(callSections[1], "playWav"))
                    return playWav((String) redefineValueForRuntime(args[0], localData));

            if (Objects.equals(callSections[1], "image"))
                switch (callSections[1]) {
                    case "blur":         return blur((BufferedImage) redefineValueForRuntime(args[0], localData));
                    case "heavyBlur":    return heavyBlur((BufferedImage) redefineValueForRuntime(args[0], localData));
                    case "detectEdges":  return detectEdges((BufferedImage) redefineValueForRuntime(args[0], localData));
                    case "smoothResize": return smoothResize((BufferedImage) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData));
                    case "resize":       return resizeImage((BufferedImage) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData));
                    case "rotate":       return rotate((BufferedImage) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData));
                    case "brighten":     return brighten((BufferedImage) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData));
                    case "pixelate":     return pixelate((BufferedImage) redefineValueForRuntime(args[0], localData));
                    case "toGray":       return convertToGrayScale((BufferedImage) redefineValueForRuntime(args[0], localData));
                    case "setAlpha":     return setAlpha((BufferedImage) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData));
                }

            if (Objects.equals(callSections[1], "math"))
                switch (callSections[2]) {
                    case "cos":         return Math.cos((double) redefineValueForRuntime(args[0], localData));
                    case "sin":         return Math.sin((double) redefineValueForRuntime(args[0], localData));
                    case "ln":          return Math.log((double) redefineValueForRuntime(args[0], localData));
                    case "floor":       return Math.floor((double) redefineValueForRuntime(args[0], localData));
                    case "ceil":        return Math.ceil((double) redefineValueForRuntime(args[0], localData));
                    case "log10":       return Math.log10((double) redefineValueForRuntime(args[0], localData));
                    case "log1p":       return Math.log1p((double) redefineValueForRuntime(args[0], localData));
                    case "abs":         return Math.abs((double) redefineValueForRuntime(args[0], localData));
                    case "log":         return Math.log((double) redefineValueForRuntime(args[1], localData)) / Math.log((int) redefineValueForRuntime(args[0], localData));
                    case "tan":         return Math.tan((double) redefineValueForRuntime(args[0], localData));
                    case "round":       return Math.round((double) redefineValueForRuntime(args[0], localData));
                    case "acos":        return Math.acos((double) redefineValueForRuntime(args[0], localData));
                    case "asin":        return Math.asin((double) redefineValueForRuntime(args[0], localData));
                    case "cosh":        return Math.cosh((double) redefineValueForRuntime(args[0], localData));
                    case "sinh":        return Math.sinh((double) redefineValueForRuntime(args[0], localData));
                    case "tanh":        return Math.tanh((double) redefineValueForRuntime(args[0], localData));
                    case "atan":        return Math.atan((double) redefineValueForRuntime(args[0], localData));
                    case "atan2":       return Math.atan2((double) redefineValueForRuntime(args[0], localData), (double) redefineValueForRuntime(args[0], localData));
                    case "randInt":     return rand.nextInt((int) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[0], localData));
                    case "randBool":    return rand.nextBoolean();
                    case "randDouble":  return rand.nextDouble((int) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[0], localData));
                    case "toDeg":       return Math.toDegrees((double) redefineValueForRuntime(args[0], localData));
                    case "toRad":       return Math.toRadians((double) redefineValueForRuntime(args[0], localData));
                    case "sqrt":        return Math.sqrt((double) redefineValueForRuntime(args[0], localData));
                    case "pi":          return Math.PI;
                    case "tau":         return Math.TAU;
                    case "e":           return Math.E;
                }

            if (Objects.equals(callSections[1], "color"))
                switch (callSections[2]) {
                    case "new":
                        if (args.length == 3)
                            return new Color((int) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData));
                        if (args.length == 4)
                            return new Color((int) redefineValueForRuntime(args[0], localData), (int) redefineValueForRuntime(args[1], localData), (int) redefineValueForRuntime(args[2], localData), (int) redefineValueForRuntime(args[3], localData));
                    case "white":       return Color.WHITE;
                    case "black":       return Color.BLACK;
                    case "red":         return Color.RED;
                    case "yellow":      return Color.YELLOW;
                    case "orange":      return Color.ORANGE;
                    case "blue":        return Color.BLUE;
                    case "magenta":     return Color.MAGENTA;
                    case "cyan":        return Color.CYAN;
                    case "green":       return Color.GREEN;
                    case "gray":        return Color.GRAY;
                    case "light_gray":  return Color.LIGHT_GRAY;
                    case "dark_gray":   return Color.DARK_GRAY;
                    case "pink":        return Color.PINK;
                }
        }

        if ((value).contains(".")){;
            if (localData.containsKey(( value).split("\\.")[0])){
                if (localData.get((value).split("\\.")[0]) instanceof Display)
                    return ((Display) localData.get((value).split("\\.")[0])).getVal((value).split("\\.")[1]);
                if (localData.get((value).split("\\.")[0]) instanceof HashMap){
                    String val = (value).split("\\.")[1];
                    if (Objects.equals(val, "size"))
                        return ((HashMap<Object, Object>) localData.get((value).split("\\.")[0])).size();

                    return ((HashMap<Object, Object>) localData.get((value).split("\\.")[0])).get(val);
                }
                if (localData.get((value).split("\\.")[0]) instanceof LinkedList){
                    String val = (value).split("\\.")[1];
                    if (Objects.equals(val, "size"))
                        return ((LinkedList<Object>) localData.get((value).split("\\.")[0])).size();
                    try{
                        return ((LinkedList<Object>) localData.get((value).split("\\.")[0])).get(Integer.parseInt(val) - 1);
                    } catch (NumberFormatException _){}
                }
            }
            if (data.containsKey((value).split("\\.")[0])){
                if (data.get((value).split("\\.")[0]) instanceof Display)
                    return ((Display) data.get((value).split("\\.")[0])).getVal((value).split("\\.")[1]);
                if (data.get((value).split("\\.")[0]) instanceof HashMap){
                    String val = (value).split("\\.")[1];
                    if (Objects.equals(val, "size"))
                        return ((HashMap<Object, Object>) data.get((value).split("\\.")[0])).size();
                    if (Objects.equals(val, "keys"))
                        return ((HashMap<Object, Object>) data.get((value).split("\\.")[0])).keySet();
                    if (Objects.equals(val, "values"))
                        return ((HashMap<Object, Object>) data.get((value).split("\\.")[0])).values();

                    return ((HashMap<Object, Object>) data.get((value).split("\\.")[0])).get(val);
                }
                if (data.get((value).split("\\.")[0]) instanceof LinkedList){
                    String val = (value).split("\\.")[1];
                    if (Objects.equals(val, "size"))
                        return ((LinkedList<Object>) data.get((value).split("\\.")[0])).size();
                    try{
                        return ((LinkedList<Object>) data.get((value).split("\\.")[0])).get(Integer.parseInt(val) - 1);
                    } catch (NumberFormatException _){}
                }
            }
            try{
                return Double.parseDouble(value);
            }catch (NumberFormatException _){}
        }

        if (data.get(value) != null)
            return data.get(value);

        if (localData.get(value) != null)
            return localData.get(value);

        throw new IOException("Failed to interpret value: " + value);
    }

    private void getStartData() throws Exception {
        String startFunctionData = (String) functions.get("start").getFirst();

        LinkedList<String> listData = new LinkedList<>(Arrays.asList((startFunctionData.split("\\s{2,}"))));
        for (String current : listData) {
            String[] expression = current.split("=");

            data.put(expression[0].strip(), redefineValueForRuntime(expression[1].strip(), new LinkedHashMap<>()));
        }
    }
    //</editor-fold>

    public static boolean playWav(String path) {
        try{
            File audioFile = new File(path);

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);

            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(-10.0f);

            clip.start();
        } catch (Exception e){return false;}

        return true;
    }

    //<editor-fold desc="Image resources">
    public static BufferedImage smoothResize(@NotNull BufferedImage img, int newHeight, int newWidth) {
        double heightFactor = (double) newHeight / img.getHeight();
        double widthFactor = (double) newWidth / img.getWidth();

        BufferedImage scaledImg = new BufferedImage((int)(heightFactor*img.getWidth()), newHeight, img.getType());
        AffineTransform at = new AffineTransform();
        at.scale(heightFactor, widthFactor);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);

        return scaleOp.filter(img, scaledImg);
    }

    public static BufferedImage setAlpha(@NotNull BufferedImage img, int alpha){
        float floatAlpha = (float) alpha /255;
        BufferedImage nImg = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        Graphics g = nImg.createGraphics();
        g.drawImage(img, 0, 0, null);

        float[] alp = new float[]{1f, 1f, 1f, floatAlpha};
        float[] def = new float[]{0, 0, 0, 0};
        RescaleOp r = new RescaleOp(alp, def, null);

        return r.filter(nImg, null);
    }

    public static @NotNull BufferedImage convertToGrayScale (@NotNull BufferedImage img) {
        BufferedImage grayImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = grayImage.getGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();

        return grayImage;
    }

    public static @NotNull BufferedImage pixelate (@NotNull BufferedImage img) {
        BufferedImage pixImg = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        int pix = 0, p=0;
        for (int y=0; y<img.getHeight()-2; y+=2) {
            for (int x=0; x<img.getWidth()-2; x+=2) {
                pix = (int)((img.getRGB(x, y)& 0xFF) + (img.getRGB(x+1, y)& 0xFF) + (img.getRGB(x, y+1)& 0xFF) + (img.getRGB(x+1, y+1)& 0xFF))/4;
                p = (255<<24) | (pix<<16) | (pix<<8) | pix;
                pixImg.setRGB(x,y,p);
                pixImg.setRGB(x+1,y,p);
                pixImg.setRGB(x,y+1,p);
                pixImg.setRGB(x+1,y+1,p);
            }
        }

        return pixImg;
    }

    public static @NotNull BufferedImage blur (@NotNull BufferedImage img) {
        BufferedImage blurImg = new BufferedImage(img.getWidth()-2, img.getHeight()-2, img.getType());
        int pix = 0;
        for (int y=0; y<blurImg.getHeight(); y++) {
            for (int x=0; x<blurImg.getWidth(); x++) {
                pix = (int)(4*(img.getRGB(x+1, y+1)& 0xFF) + 2*(img.getRGB(x+1, y)& 0xFF) + 2*(img.getRGB(x+1, y+2)& 0xFF) + 2*(img.getRGB(x, y+1)& 0xFF) + 2*(img.getRGB(x+2, y+1)& 0xFF) + (img.getRGB(x, y)& 0xFF) + (img.getRGB(x, y+2)& 0xFF) + (img.getRGB(x+2, y)& 0xFF) + (img.getRGB(x+2, y+2)& 0xFF))/16;
                int p = (255<<24) | (pix<<16) | (pix<<8) | pix;
                blurImg.setRGB(x,y,p);
            }
        }
        return blurImg;
    }

    public static @NotNull BufferedImage heavyBlur (@NotNull BufferedImage img) {
        BufferedImage blurImg = new BufferedImage(img.getWidth()-4, img.getHeight()-4, img.getType());

        int pix = 0;
        for (int y=0; y<blurImg.getHeight(); y++) {
            for (int x=0; x<blurImg.getWidth(); x++) {
                pix = (int)(10*(img.getRGB(x+3, y+3)& 0xFF) + 6*(img.getRGB(x+2, y+1)& 0xFF) + 6*(img.getRGB(x+1, y+2)& 0xFF) + 6*(img.getRGB(x+2, y+3)& 0xFF) + 6*(img.getRGB(x+3, y+2)& 0xFF) + 4*(img.getRGB(x+1, y+1)& 0xFF) + 4*(img.getRGB(x+1, y+3)& 0xFF) + 4*(img.getRGB(x+3, y+1)& 0xFF) + 4*(img.getRGB(x+3, y+3)& 0xFF) + 2*(img.getRGB(x, y+1)& 0xFF) + 2*(img.getRGB(x, y+2)& 0xFF) + 2*(img.getRGB(x, y+3)& 0xFF) + 2*(img.getRGB(x+4, y+1)& 0xFF) + 2*(img.getRGB(x+4, y+2)& 0xFF) + 2*(img.getRGB(x+4, y+3)& 0xFF) + 2*(img.getRGB(x+1, y)& 0xFF) + 2*(img.getRGB(x+2, y)& 0xFF) + 2*(img.getRGB(x+3, y)& 0xFF) + 2*(img.getRGB(x+1, y+4)& 0xFF) + 2*(img.getRGB(x+2, y+4)& 0xFF) + 2*(img.getRGB(x+3, y+4)& 0xFF) + (img.getRGB(x, y)& 0xFF) + (img.getRGB(x, y+2)& 0xFF) + (img.getRGB(x+2, y)& 0xFF) + (img.getRGB(x+2, y+2)& 0xFF))/74;
                int p = (255<<24) | (pix<<16) | (pix<<8) | pix;
                blurImg.setRGB(x,y,p);
            }
        }

        return blurImg;
    }

    public static @NotNull BufferedImage detectEdges (@NotNull BufferedImage img) {
        int h = img.getHeight(), w = img.getWidth(), threshold=30, p = 0;
        BufferedImage edgeImg = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        int[][] vert = new int[w][h];
        int[][] horiz = new int[w][h];
        int[][] edgeWeight = new int[w][h];
        for (int y=1; y<h-1; y++) {
            for (int x=1; x<w-1; x++) {
                vert[x][y] = (int)(img.getRGB(x+1, y-1)& 0xFF + 2*(img.getRGB(x+1, y)& 0xFF) + img.getRGB(x+1, y+1)& 0xFF - img.getRGB(x-1, y-1)& 0xFF - 2*(img.getRGB(x-1, y)& 0xFF) - img.getRGB(x-1, y+1)& 0xFF);
                horiz[x][y] = (int)(img.getRGB(x-1, y+1)& 0xFF + 2*(img.getRGB(x, y+1)& 0xFF) + img.getRGB(x+1, y+1)& 0xFF - img.getRGB(x-1, y-1)& 0xFF - 2*(img.getRGB(x, y-1)& 0xFF) - img.getRGB(x+1, y-1)& 0xFF);
                edgeWeight[x][y] = (int)(Math.sqrt(vert[x][y] * vert[x][y] + horiz[x][y] * horiz[x][y]));
                if (edgeWeight[x][y] > threshold) p = (255<<24) | (255<<16) | (255<<8) | 255;
                else p = (255 << 24) | (0);
                edgeImg.setRGB(x,y,p);
            }
        }
        return edgeImg;
    }

    public static @NotNull BufferedImage brighten (@NotNull BufferedImage img, int percentage) {
        int r, g, b, rgb, p;
        int amount = percentage * 255 / 100;
        BufferedImage newImage = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        for (int y=0; y<img.getHeight(); y+=1)
            for (int x=0; x<img.getWidth(); x+=1) {
                rgb = img.getRGB(x, y);
                r = ((rgb >> 16) & 0xFF) + amount;
                g = ((rgb >> 8) & 0xFF) + amount;
                b = (rgb & 0xFF) + amount;
                if (r>255) r=255;
                if (g>255) g=255;
                if (b>255) b=255;
                p = (255<<24) | (r<<16) | (g<<8) | b;
                newImage.setRGB(x,y,p);
            }

        return newImage;
    }

    public static @NotNull BufferedImage rotate(@NotNull BufferedImage img, int angle) {
        int width = img.getWidth();
        int height = img.getHeight();

        BufferedImage newImage = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());

        Graphics2D g2 = newImage.createGraphics();

        g2.rotate(Math.toRadians(angle), (double) width / 2, (double) height / 2);
        g2.drawImage(img, null, 0, 0);

        return newImage;
    }

    public static @NotNull BufferedImage resizeImage(@NotNull BufferedImage img, int width, int height){
        BufferedImage newImage = new BufferedImage(width, height, img.getType());

        Graphics g = img.createGraphics();

        g.drawImage(img, 0, 0, width, height, null);

        return newImage;
    }
    //</editor-fold>

    //<editor-fold desc="Parsers">
    public static @Nullable JSONObject parseJson(String jsonPath) throws IOException, ParseException, ParseException {
        JSONParser jsonParser = new JSONParser();
        return (JSONObject) jsonParser.parse(new FileReader(jsonPath));
    }

    public static @NotNull String parseTxt(String path) throws IOException {
        StringBuilder result = new StringBuilder();

        File file = new File(path);

        BufferedReader br = new BufferedReader(new FileReader(file));

        String st;
        while ((st = br.readLine()) != null)
            result.append(st);

        return result.toString();
    }

    public static Map<String, Object> parseYaml(String path){
        Yaml yaml = new Yaml();
        InputStream inputStream = getResourceAsStream(path);

        return yaml.load(inputStream);
    }

    public static @Nullable BufferedImage loadImage(String path) throws IOException {
        try {
            return ImageIO.read(Objects.requireNonNull(Interpreter.class.getResource(path)));
        } catch (IOException e) {
            throw new IOException("Failed to load image file: " + path);
        }
    }

    //<editor-fold desc="BTM Loader">
    public static @NotNull BufferedImage createBTMImage(String filePath, int width, int height) throws Exception {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics g = img.getGraphics();

        FileReader testReader;

        try{
            testReader = new FileReader(filePath);
        } catch(FileNotFoundException fnf){
            throw new FileNotFoundException("Could Not Find Specified File: " + filePath);
        }

        BufferedReader flbr = new BufferedReader(testReader);

        String firstLine = flbr.readLine();

        boolean bitMapFormat = firstLine.charAt(0) == '1';

        drawBTM(filePath, width, height, g, bitMapFormat, firstLine, flbr);

        return img;
    }

    private static void drawBTM(String path, int width, int height, Graphics g, boolean bitMapFormat, String firstLine, BufferedReader flbr) throws Exception {
        int lineNumber = 0;

        double pixelWidth;
        double pixelHeight;

        if (bitMapFormat){
            if (firstLine.length() < 13)
                throw new Exception("File Doesn't Have Valid Number Of Characters: " + firstLine.length());

            pixelWidth = ((double) width / ((double) (firstLine.length() - 1) / 6)) * 2;

            int lines = 1;
            while (flbr.readLine() != null) lines++;

            pixelHeight = (double) height / lines;
        }
        else{
            if (firstLine.length() < 7)
                throw new Exception("Invalid Number Of Characters In Image File: " + firstLine.length());

            pixelWidth = ((double) width / ((double) (firstLine.length() - 1) / 3)) * 2;

            int lines = 1;
            while (flbr.readLine() != null) lines++;

            pixelHeight = (double) height / lines;
        }

        FileReader reader;

        try{
            reader = new FileReader(path);
        } catch(FileNotFoundException fnf){
            throw new FileNotFoundException("Could Not Find Specified File: " + path);
        }

        BufferedReader br = new BufferedReader(reader);

        try{
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lineNumber++;

                int innerIndx = 0;
                LinkedList<Integer> RGBAValues = new LinkedList<Integer>();

                int start = 0;
                if (lineNumber == 1)
                    start = 1;

                int RGBAValue = 0;

                for (int i = start; i < line.length() + 1; i++) {
                    String letter;

                    try{
                        letter = Character.toString(line.charAt(i));
                    } catch(IndexOutOfBoundsException e){
                        letter = "";
                    }

                    innerIndx++;

                    if (bitMapFormat){
                        if (innerIndx > 3){
                            RGBAValues.add(RGBAValue);
                            RGBAValue = 0;
                            innerIndx = 1;

                            if (RGBAValues.size() == 4){
                                g.setColor(new Color(RGBAValues.get(0), RGBAValues.get(1), RGBAValues.get(2), RGBAValues.get(3)));
                                g.fillRect((int) ceil(((((double) i / 12) - 1) * pixelWidth)), (int) ceil(((lineNumber - 1) * pixelHeight)), (int) ceil(pixelWidth), (int) ceil(pixelHeight));

                                RGBAValues.clear();
                            }
                        }

                        RGBAValue += getSingleDigitNumber(letter) * (int) pow(10, 3 - innerIndx);
                    }
                    else{
                        if (innerIndx > 3){
                            RGBAValues.add(RGBAValue);
                            RGBAValue = 0;
                            innerIndx = 1;

                            if (RGBAValues.size() == 2){
                                g.setColor(new Color(RGBAValues.get(0), RGBAValues.get(0), RGBAValues.get(0), RGBAValues.get(1)));
                                g.fillRect((int) ceil(((((double) i / 6) - 1) * pixelWidth)), (int) ceil(((lineNumber - 1) * pixelHeight)), (int) ceil(pixelWidth), (int) ceil(pixelHeight));

                                RGBAValues.clear();
                            }
                        }

                        RGBAValue += getSingleDigitNumber(letter) * (int) pow(10, 3 - innerIndx);
                    }
                }
            }
        }
        catch(IOException io){
            throw new IOException("IO Exception: Check That The Specified File Exists and Application Has Permission");
        }

        reader.close();
    }

    @Contract(pure = true)
    private static int getSingleDigitNumber(@NotNull String number){
        int result = 0;

        switch (number){
            case "0" -> {}
            case "1" -> result = 1;
            case "2" -> result = 2;
            case "3" -> result = 3;
            case "4" -> result = 4;
            case "5" -> result = 5;
            case "6" -> result = 6;
            case "7" -> result = 7;
            case "8" -> result = 8;
            case "9" -> result = 9;
        }

        return result;
    }
    //</editor-fold>
    //</editor-fold>
}