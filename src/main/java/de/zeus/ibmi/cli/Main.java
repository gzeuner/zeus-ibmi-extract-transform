package de.zeus.ibmi.cli;

public final class Main {

    public static final String VERSION = "0.1.0-SNAPSHOT";

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new CliApplication(System.getenv()).run(args, System.out, System.err);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
