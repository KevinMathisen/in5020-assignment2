package com.example;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

public class Client {
    private double balance;                         // Balance of account
    private int order_counter;                      // How many transactions executed
    private int outstanding_counter;                // How many local transactions added to outstanding collection
    private final List<Transaction> executed_list;  // Transactions exectuted
    private final Collection<Transaction> outstanding_collection;   // Transactions to be broadcast

    private final SpreadConnection spread_connection;   // Used to connect to spread server
    private final Listener spread_listener;             // Used to receive messages
    private final SpreadGroup spread_group;             // Group for account
    private final int id;

    private final String file_name;
    private final boolean naive_sync_balance;
    private SpreadGroup[] members;                  // TODO: implement updated members list

    private final ScheduledExecutorService schedulerBroadcast = Executors.newScheduledThreadPool(1);

    public Client(int id, String server_address, String account_name, String file_name, int num_of_replica, boolean naive_sync_balance) throws InterruptedException {
        // Initialize to 0 and empty lists
        this.balance = 0.0;
        this.order_counter = 0;
        this.outstanding_counter = 0;
        this.executed_list = new ArrayList<>();
        this.outstanding_collection = new ArrayList<>();

        // Initialize connection and spead listener
        this.spread_connection = new SpreadConnection();
        this.spread_listener = new Listener(this);
        this.id = id;

        this.file_name = file_name;
        this.naive_sync_balance = naive_sync_balance;

        // Connect to spread server, connect the listener, and join the spread group
        try {
            spread_connection.add(spread_listener);
            spread_connection.connect(InetAddress.getByName(server_address), 4803, String.valueOf(id), false, true);

            this.spread_group = new SpreadGroup();
            this.spread_group.join(spread_connection, account_name);
        } catch (SpreadException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Created client with ID " + id + ", waiting for " + num_of_replica + " other clients to join group");


        // Wait for all other clients to join before starting client
        // Check if members are the same as num_of_replica

        // Start broadcasting outstanding_collection every 10 seconds
        broadcastOutstanding();

        System.out.println("All members joined group, starting to synch and process commands");

        // TODO: Start processing commands (either from file_name, or from commandline)
    }

    public static void main(String[] args) throws InterruptedException {
        String server_address = "127.0.0.1";
        String account_name = "Group10";
        String file_name = "";
        int num_of_replica = 1;
        boolean naive_sync_balance = false;

        Random rand = new Random();
        Client client = new Client(rand.nextInt(), server_address, account_name, file_name, num_of_replica, naive_sync_balance);

        Thread.sleep(100000000);
    }

    private void processCommand(String command) throws Exception {
        command = command.trim();

        // Check if command should have an argument
        if (command.startsWith("deposit") || command.startsWith("addInterest") || command.startsWith("checkTxStatus") || command.startsWith("sleep")) {
            if (command.split(" ").length != 2 ) {
                System.out.println("Invalid argument for command, should follow syntax '<command_name> <value>'");
                return;
            }

            // Handle deposit and addInterest commands by creating transactions
            if (command.startsWith("deposit") || command.startsWith("addInterest")) {
                Transaction tx = new Transaction();
                tx.command = command;
                tx.uniqueId = id + " " + outstanding_counter++;
                
                // Add the transaction to outstanding collection to be broadcast
                synchronized (outstanding_collection) {
                    outstanding_collection.add(tx);
                }

            // Handle txstatus and sleep by calling their methods
            } else if (command.startsWith("checkTxStatus")) {
                printTxStatus(command.split(" ")[1]);

            } else if (command.startsWith("sleep")) {
                sleep(Integer.parseInt(command.split(" ")[1]));
            }

            return;
        }

        // Call naive or correct approach for getSyncBalance depending on which one set to use
        if (command.startsWith("getSynchedBalance")) {
            if (naive_sync_balance) getSyncedBalanceNaive();
            else                    getSyncedBalanceCorrect();
        }

        // Simply call the methods of the other commands which require no arguments
        switch (command) {
            case "getQuickBalance" -> getQuickBalance();
            case "getHistory" -> getHistory();
            case "cleanHistory" -> cleanHistory();
            case "memberInfo" -> memberInfo();
            case "exit" -> exit();
            default -> System.out.println("Invalid command");
        }

    }

    private void broadcastOutstanding() {
        // calls sendOutstanding every 10 seconds
        schedulerBroadcast.scheduleAtFixedRate(() -> {
            try {
                sendOutstanding(); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    private void sendOutstanding() throws InterruptedException {
        try {
            SpreadMessage message = new SpreadMessage();
            message.addGroup(spread_group);
            message.setFifo();
            message.setObject((Serializable) outstanding_collection);
            spread_connection.multicast(message);
            // Outstanding collection is broadcast, but not cleared, which could in some cases cause transactions to be broadcast twice
            //   However this should not a problem, as clients receiving transactions should check if they have already been executed

        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }

    public void processReceivedTransactions(Collection<Transaction> receivedTransactions) {
        synchronized (this) {
            // Iterate over all transactions received
            for (Transaction tx : receivedTransactions) {

                // Check if the transaction has already been executed 
                boolean tx_already_executed = false;
                for (Transaction executedTx : executed_list) {
                    if (executedTx.uniqueId.equals(tx.uniqueId)) {
                        tx_already_executed = false;
                    }
                }

                // If the transaction was local, remove it from the local list
                outstanding_collection.removeIf(localTx -> localTx.uniqueId.equals(tx.uniqueId));

                // Execute the transaction if it has not been already
                if (!tx_already_executed) {
                    // Execute transaction
                    // Need to handle getSyncedBalance and setBalance
                    // Count it
                    order_counter++;
                    executed_list.add(tx);
                }
            }
        }
    }

    private void handleMemberShipChange(SpreadGroup[] members) {

    }

    private void getQuickBalance() {
        System.out.println("Quick balance: " + balance);
    }

    private void getSyncedBalanceNaive() {
        // wait in another thread for outstanding_collection to be empty
        //   then print balance
        
        System.out.println("Naive Synced balance: " + balance);
    }

    private void getSyncedBalanceCorrect() {
        // create a transaction 
    }

    private void deposit(double amount) {
        balance += amount;
    }

    private void addInterest(double percent) {
        balance += balance * (percent / 100.0);
    }
    
    private void getHistory() {
        // TODO: should print out history
        //   transactions from executed list should be numbered from (order_counter-executed_list.length()) and up
        //   transactions from outstanding should just be printed in order of when added (from index 0 and up)
        System.out.println("History: ");
    }

    private void printTxStatus(String transactionId) {
        System.err.println("Transaction with ID " + transactionId + " has status: " + getTxStatus(transactionId));
    }
    
    private String getTxStatus(String transactionId) {
        for (Transaction transaction : executed_list) {
            if (transaction.uniqueId.equals(transactionId)) {
                return "Executed";
            }
        }
        for (Transaction transaction : outstanding_collection) {
            if (transaction.uniqueId.equals(transactionId)) {
                return "In queue";
            }
        }
        return "Not Found";
    }
    
    private void cleanHistory() {
        executed_list.clear();
    }
    
    private List<String> memberInfo() {
        // TODO: Should print out members in string format
        List<String> members = new ArrayList<>();
        return members;
    }
    
    private void sleep(int duration) {
        // If this is called while file_name is not defined (i.e user is inputing commands) no sleep should happen
        if (file_name.isEmpty()) {
            return;
        }

        try {
            Thread.sleep(duration * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    private void exit() {
        // Exit the client application
        try {
            spread_connection.disconnect();
        } catch (SpreadException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

}