/**
 * Authors: Antoine Mandin and Nada Lahlou
 */


public class Main {
    public static void printHelp() {
        System.out.println("    Usage:");
        System.out.println("        java Main [-h]");
        System.out.println("            -h - print this help message");
    }

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.equals("-h")) {
                printHelp();
            }
        }
    }
}