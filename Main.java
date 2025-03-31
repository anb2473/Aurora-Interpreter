/*
    TODO:
        - Increase versatility and remove buggy implementations
*/

public class Main {
    public static void main(String[] args) throws Exception {
        Interpreter interpreter = new Interpreter("src\\test.aura");
        while (true)
            interpreter.run();
    }
}