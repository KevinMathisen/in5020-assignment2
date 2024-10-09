package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class BatchProcessor {
    private final Client[] clients;

    public BatchProcessor(Client[] clients) {
        this.clients = clients;
    }

    /**
     * Processes a batch file, each line represents a command that will be
     * executed by all clients in the array. The method waits for a random period between 0.5 to 1.5 seconds
     * before processing the next command.
     * 
     * @param fileName 
     */
    public void processBatchFile(String fileName) throws IOException, InterruptedException {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String command;
            // Random generator for delays
            Random rand = new Random();

             // Read each command from the file and process it
            while ((command = reader.readLine()) != null) {
                executeCommand(command.trim());

                // Sleep for a random time between 0.5 to 1.5 seconds using rand generator
                Thread.sleep((long) (500 + rand.nextDouble() * 1000));
            }
        }
    }
    /**
     * Executes a given command on each client. 
     * @param command The command to be executed on all clients.
     */
    private void executeCommand(String command) {
        for (Client client : clients) {
            if (command.equals("getQuickBalance")) {
                System.out.println("Client ID: " + client.getId() + " Quick Balance: " + client.getQuickBalance());
            } else if (command.startsWith("deposit")) {
                String[] parts = command.split(" ");
                if (parts.length == 2) {
                    // Extract the amount from the command
                    double amount = Double.parseDouble(parts[1]);
                    // Perform the deposit operation
                    client.deposit(amount);
                } else {
                    // ... resterende commands
                }
            } else if (command.equals("exit")) {
                client.exit();
            } else {
                System.out.println("Unknown command: " + command);
            }
        }
    }
}
