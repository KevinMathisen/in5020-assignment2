package com.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
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

    private final int id;                           // Unique ID of the client
    public boolean start_mode;                  // If the client is starting up
    public boolean sync_mode;                      // If the client is currently syncronising
    private int received_sync;                      // Amount of received sync messages since last sync
    private final CountDownLatch startClientLatch = new CountDownLatch(1);  

    private final String file_name;                 // File name to get commands from, empty '' when we read from command line
    private final int num_of_replica;               // Number of replicas required to start processing
    private final boolean naive_sync_balance;       // If naive mode should be used when running the command getSyncedBalance
    public SpreadGroup[] members;                   // The current members in the group

                                                    // Scheduler for broadcasting every 10 seconds
    private final ScheduledExecutorService schedulerBroadcast = Executors.newScheduledThreadPool(1);

    /**
     * Constructor for Client, which intializes variables and tries to connect the client to the spread server
     * 
     * @param id                    - Unique id of client
     * @param server_address        - Ip address of spread server
     * @param account_name          - Group name in spread server
     * @param file_name             - File to get commands from, empty if from command line
     * @param naive_sync_balance    - If naive mode should be used when running the command getSyncedBalance 
     * @throws InterruptedException
     */
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
        this.start_mode = true;   // Client is starting
        this.sync_mode = false;      // Client is not syncing at start
        this.received_sync = 0;

        this.file_name = file_name;
        this.num_of_replica = num_of_replica;
        this.naive_sync_balance = naive_sync_balance;

        try {
            spread_connection.add(spread_listener);         // Add the listener
            spread_connection.connect(                      // Connect to the spread server
                InetAddress.getByName(server_address), 4803, String.valueOf(id), false, true
            );

            this.spread_group = new SpreadGroup();          // Join the spread group for our account
            this.spread_group.join(spread_connection, account_name);

        } catch (SpreadException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read arguments, creates a client, waits for all other clients to join, starts broadcasting the outstanding_colleciton, and then reads commands
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException {
        // Temporary command line arguments
        String server_address = "127.0.0.1";
        String account_name = "Group10";
        String file_name = "";
        int num_of_replica = 2;
        boolean naive_sync_balance = false;
        int client_id = -1;

        // Parse command-line arguments
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--server":
                    server_address = args[++i];  
                    break;
                case "--account":
                    account_name = args[++i];    
                    break;
                case "--file":
                    file_name = args[++i];      
                    break;
                case "--replicas":
                    num_of_replica = Integer.parseInt(args[++i]);  
                    break;
                case "--naive":
                    naive_sync_balance = true;   
                    break;
                default:
                    System.out.println("Unknown argument: " + args[i]);
                    break;
            }
        }

        // Could also read optional client id from command line
        if (client_id == -1) {
            Random rand = new Random();
            client_id = rand.nextInt(Integer.MAX_VALUE)+1;
        }

        // Initialize client
        Client client = new Client(client_id, server_address, account_name, file_name, num_of_replica, naive_sync_balance);

        System.out.println("Created client with ID " + client_id + ", waiting for " + num_of_replica + " client(s) to join group");
        
        // Wait for all other clients to join before starting to process commands
        client.startClientLatch.await();

        System.out.println("All members joined group, starting to sync and process commands");

        // Start broadcasting outstanding_collection every 10 seconds
        client.broadcastOutstanding();

        // Start processing commands (either from file_name, or from commandline)
        if (!file_name.isEmpty()) {
            // Process commands from the file
            try {
                client.processCommandsFromFile(file_name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // Process commands from the command line
            client.processCommandsFromCommandLine();
        }

        try {
            client.processCommand("deposit 100");
            client.processCommand("addInterest 10");
            client.processCommand("getQuickBalance");
            client.processCommand("getSyncedBalance");
        } catch (Exception e) {
            e.printStackTrace();
        }

        Thread.sleep(100000000);
    }

    /**
     * Takes in a command, checks if the command has valid syntax, and then runs code to perform the command
     * @param command - The command to execute
     * @throws Exception
     */
    public void processCommand(String command) throws Exception {
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
        if (command.startsWith("getSyncedBalance")) {
            if (naive_sync_balance) getSyncedBalanceNaive();
            else                    getSyncedBalanceCorrect();

            return;
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

    /**
     * Broadcasts outstanding_collection every 10 seconds if the client is not syncronising
     */
    private void broadcastOutstanding() {
        // calls sendOutstanding every 10 seconds
        schedulerBroadcast.scheduleAtFixedRate(() -> {
            try {
                // Only send outstanding if mode is not syncing and not start
                if (!start_mode && !sync_mode) sendOutstanding(outstanding_collection); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * Sends outstanding_collection to the spread group
     * @throws InterruptedException
     */
    private void sendOutstanding(Collection<Transaction> outstanding_collection) throws InterruptedException {
        try {
            // Create spread message, add it to our account, and make it follow FIFO to ensure consistent view
            SpreadMessage message = new SpreadMessage();
            message.addGroup(spread_group);
            message.setFifo();

            // Set the message content to be the outstanding_collection
            message.setObject((Serializable) outstanding_collection);

            // Send the message to the other members of the group
            spread_connection.multicast(message);

            // Note:
            //      Outstanding collection is broadcast, but not cleared, which could in some cases cause transactions to be broadcast twice
            //      However this should not a problem, as clients receiving transactions should check if they have already been executed

        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }

    /**
     * Takes in transactions received from the spread server, and executed them if no problems are detected
     * Handles getSyncedBalance, setBalance, deposit, and addInterest
     * When a deposit or addInterest transaction is executed the order_counter is incremented and the transaction is saved to executed_list 
     * Also notifies getSyncedBalance if outstanding_collection is empty when it is implemented as naive
     * 
     * @param receivedTransactions - A collection containing transactions reveived from spread, which is executed in order of collection (which should be the same for all replicas??)
     */
    public void processReceivedTransactions(Collection<Transaction> receivedTransactions) {
        synchronized (this) {
            // Return if there are no transactions to process 
            if (receivedTransactions.isEmpty()) return;

            // Retrieve if we received sync transaction
            boolean syncMessageReceived = receivedTransactions.iterator().next().command.startsWith("sync");                            

            // If we are syncing, (or in start mode) we want to not handle any incomming transaction, except if they are for syncronizing. 
            //   Because the transactions broadcasted from this client are not removed yet, we can feel safe knowing they will be broadcast again later:)
            if ((sync_mode || start_mode) && !syncMessageReceived) return;

            // Iterate over all transactions received
            for (Transaction tx : receivedTransactions) {

                // Check if the transaction has already been executed 
                boolean tx_already_executed = false;
                for (Transaction executedTx : executed_list) {
                    if (executedTx.uniqueId.equals(tx.uniqueId)) {
                        tx_already_executed = false;
                        break;
                    }
                }

                // If the transaction was local, remove it from the local list
                outstanding_collection.removeIf(localTx -> localTx.uniqueId.equals(tx.uniqueId));

                // If transaction has already been executed, skip it
                if (tx_already_executed) continue;

                // If command is getSyncedBalance, print out the balance if the command was meant for this client
                if (tx.command.equals("getSyncedBalance")) {
                    if (Integer.parseInt(tx.uniqueId.split(" ")[0]) == id) {
                        System.out.println("Correct Synced Balance: " + balance);
                    }
                    continue;
                }

                // Check if command has correct format (<command> <value>)
                String[] commandParts;
                try {
                   commandParts = tx.command.split(" "); 
                } catch (Exception e) {
                    continue;
                }

                // call handleSyncBalance for syncronization transactions
                if (commandParts[0].equals("sync")) {
                    handleSyncTx(tx);
                    continue;
                }

                // Execute deposit and addInterest commands by using their method, skip transaction if invalid transaction type
                switch (commandParts[0]) {
                    case "deposit" -> deposit(Double.parseDouble(commandParts[1]));
                    case "addInterest" -> addInterest(Double.parseDouble(commandParts[1]));
                    default -> {
                        continue;
                    }
                }

                // Count deposit and addInterest transaction and save it to history
                order_counter++;
                executed_list.add(tx);

                // For debugging
                System.out.println("Executed transaction '" + tx.command + "' with id '" + tx.uniqueId + "', New balance is: " + balance);
            }

            // If outstanding_collection empty and are using navie sync balance, notify potential getSyncedBalance waiting for outstanding_collection to be empty
            synchronized (outstanding_collection) {
                if (outstanding_collection.isEmpty() && naive_sync_balance) outstanding_collection.notifyAll();
            }
        }
    }

    /**
     * Handles membership changes for the account
     * Starts the client when enough clients have joined
     * Also sets the client to sync mode and broadcasts a sync transaction when a new member joins
     * @param members
     */
    public void handleMemberShipChange(SpreadGroup[] members) {
        // If a member leaves the exection, update member list but otherwise ignore it.
        if (this.members != null && this.members.length >= members.length) {
            this.members = members;
            return;
        }

        // Update members and check if we can start client
        this.members = members;
        if (start_mode) {
            updateStartMode();
            return;
        }
        
        // We have received a new member and are not in start mode; assume we need to sync
        sync_mode = true;

        // Send out sync transaction, containing current balance and order_counter
        Transaction syncTx = new Transaction();
        syncTx.command = "sync " + balance + " " + order_counter;   // sync tx format: sync <balance> <order_counter>
        syncTx.uniqueId = id + " " + outstanding_counter++;

        Collection<Transaction> syncTxCollection = new ArrayList<>();
        syncTxCollection.add(syncTx);

        try {
            sendOutstanding(syncTxCollection);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the reception of a sync transaction
     * If we are a new member (in start mode), we use the values to set balance and order_counter
     * If we are an existing member, we use the values to check if we are syncronized
     * Makes the client exit sync mode once enough sync transactions have been received
     * @param tx - The sync transaction
     */
    private void handleSyncTx(Transaction tx) {
        String[] commandParts = tx.command.split(" ");
        double syncBalance = Double.parseDouble(commandParts[1]);
        int syncOrderCounter = Integer.parseInt(commandParts[2]);

        // When in start mode we set the balance and order_counter, and change mode to sync_mode
        if (start_mode) {
            balance = syncBalance;
            order_counter = syncOrderCounter;

            sync_mode = true;
            start_mode = false;
            startClientLatch.countDown();   // Let main know we are no longer in start_mode

        // When not in start mode, we are in sync mode (ARE WE ???)
        } else {

            // Check if the replias differ in balance and/or order_counter. If so, let user know and exit program.
            if (balance != syncBalance || order_counter != syncOrderCounter) {
                System.err.println("Fatal error: Balance and/or order counter differ between replicas, view is not consistent");
                System.err.println("Expected balance '" + balance + "', but received '" + syncBalance);
                System.err.println("Expected order_counter '" + order_counter + "', but received '" + syncOrderCounter);
                exit();
            }
        }

        // Check how many sync received. When enough (membersize-1): go out of sync mode 
        if (++received_sync >= (members.length - 1)) {
            received_sync = 0;
            sync_mode = false;
        }
    }

    /**
     * Prints the balance immediately
     */
    private void getQuickBalance() {
        System.out.println("Quick balance: " + balance);
    }

    /**
     * Print the balance when outstanding collection is empty
     */
    private void getSyncedBalanceNaive() {
        // wait in a thread for outstanding_collection to be empty
        //   then print balance
        new Thread(() -> {
            synchronized (outstanding_collection) {
                while (!outstanding_collection.isEmpty()) {
                    try {
                        outstanding_collection.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Naive Synced balance: " + balance);
            }
        }).start();
    }

    /**
     * Print the balance by adding a transaction which will print the balance when it is later broadcasted and executed
     */
    private void getSyncedBalanceCorrect() {
        // create a transaction 
        Transaction tx = new Transaction();
        tx.command = "getSyncedBalance";
        tx.uniqueId = id + " " + outstanding_counter++;
        
        // Add the transaction to outstanding collection to be broadcast
        synchronized (outstanding_collection) {
            outstanding_collection.add(tx);
        }
}

    /**
     * Add given amount to the balance
     * @param amount - How much to add to the balance. Can be negative
     */
    private void deposit(double amount) {
        balance += amount;
    }

    /**
     * Apply interest to the balance
     * @param percent - How much to interest (in percent) to add to the balance. Can be negative
     */
    private void addInterest(double percent) {
        balance += balance * (percent / 100.0);
    }
    
    /**
     * Print transactions from the executed list and the outstanding_collection
     * The transactions in executed_list are numbered based on order_counter
     */
    private void getHistory() {
        // TODO: should print out history
        //   transactions from executed list should be numbered from (order_counter-executed_list.length()) and up
        //   transactions from outstanding should just be printed in order of when added (from index 0 and up)
        System.out.println("History: ");
    }

    /**
     * Print the status of the transaction with the ID given
     * @param transactionId - The id of the transaction to find status of
     */
    private void printTxStatus(String transactionId) {
        System.err.println("Transaction with ID " + transactionId + " has status: " + getTxStatus(transactionId));
    }

    /**
     * Return the status of the transaction with the ID given
     * @param transactionId - The id of the transaction to find status of
     * @return the status of the transaction
     */
    private String getTxStatus(String transactionId) {
        // Check if transaction is in the executed list
        for (Transaction transaction : executed_list) {
            if (transaction.uniqueId.equals(transactionId)) {
                return "Executed";
            }
        }

        // Check if the transaction is in outstanding_collection 
        for (Transaction transaction : outstanding_collection) {
            if (transaction.uniqueId.equals(transactionId)) {
                return "In queue";
            }
        }

        return "Not Found";
    }
    
    /**
     * Empty executed_list
     */
    private void cleanHistory() {
        executed_list.clear();
    }
    
    /**
     * Prints the members to the user
     */
    private void memberInfo() {
        // TODO: Should print out members in string format
        System.out.println("Current members are: ");
    }
    
    /**
     * Pause execution of thread for specified seconds
     * Only pauses when commands are retrieved from a batch file
     * @param duration - How many seconds to sleep for
     */
    private void sleep(int duration) {
        // If this is called while file_name is not defined (i.e user is inputing commands) no sleep will happen
        if (file_name.isEmpty()) {
            return;
        }

        try {
            Thread.sleep(duration * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Disconnects from spread and exits the program
     */
    private void exit() {
        try {
            spread_connection.disconnect();
        } catch (SpreadException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * Checks if enough members have joined to exit start mode, and if so signals this to main
     */
    public void updateStartMode() {
        // Only exit start mode if same amount of replicas and expected
        //      if too few: we need to wait for more to join, 
        //      if too many: means we need to sync and therefore wait for sync message
        if (start_mode && members.length == num_of_replica ) {
            this.start_mode = false;
            startClientLatch.countDown();
        }
    }

    public void processCommandsFromFile(String fileName) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String command;
            while ((command = reader.readLine()) != null) {
                processCommand(command);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
    public void processCommandsFromCommandLine() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter commands (type 'exit' to quit):");
    
        while (true) {
            String command = scanner.nextLine();
            if (command.equalsIgnoreCase("exit")) {
                exit();
                break;
            }
            try {
                processCommand(command);
            } catch (Exception e) {
                System.err.println("Error processing command: " + e.getMessage());
            }
        }
    }

}