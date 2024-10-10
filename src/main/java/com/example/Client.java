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

    private final int id;                           // Unique ID of the client
    private boolean client_syncronising;            // If the client is currently syncronising

    private final String file_name;                 // File name to get commands from, empty '' when we read from command line
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
    public Client(int id, String server_address, String account_name, String file_name, boolean naive_sync_balance) throws InterruptedException {
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
        this.client_syncronising = false;   // Start all clients in normal mode

        this.file_name = file_name;
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
        // Temporary command line argument
        // TODO: read arguments from command line
        String server_address = "127.0.0.1";
        String account_name = "Group10";
        String file_name = "";
        int num_of_replica = 1;
        boolean naive_sync_balance = false;

        // Initialize client
        Random rand = new Random();
        int client_id = rand.nextInt(Integer.MAX_VALUE)+1;
        Client client = new Client(client_id, server_address, account_name, file_name, naive_sync_balance);

        System.out.println("Created client with ID " + client_id + ", waiting for " + num_of_replica + " other clients to join group");
        
        // Wait for all other clients to join before starting to process commands
        synchronized (client.members) {
            while (client.members.length != num_of_replica) {
                try {
                    client.members.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("All members joined group, starting to synch and process commands");

        // Start broadcasting outstanding_collection every 10 seconds
        client.broadcastOutstanding();

        // TODO: Start processing commands (either from file_name, or from commandline)

        Thread.sleep(100000000);
    }

    /**
     * Takes in a command, checks if the command has valid syntax, and then runs code to perform the command
     * @param command - The command to execute
     * @throws Exception
     */
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
                // Only send outstanding if mode is not syncing
                if (!client_syncronising) sendOutstanding(); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    /**
     * Sends outstanding_collection to the spread group
     * @throws InterruptedException
     */
    private void sendOutstanding() throws InterruptedException {
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
     * Also notifies getSynchedBalance if outstanding_collection is empty when it is implemented as naive
     * 
     * @param receivedTransactions - A collection containing transactions reveived from spread, which is executed in order of collection (which should be the same for all replicas??)
     */
    public void processReceivedTransactions(Collection<Transaction> receivedTransactions) {
        synchronized (this) {
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

                // If setbalance command for syncronisation, send it to handleSetBalance(...)
                if (commandParts[0].equals("setBalance")) {
                    handleSetBalance(Double.parseDouble(commandParts[1]));
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
            }

            // If outstanding_collection empty and are using navie sync balance, notify potential getSyncedBalance waiting for outstanding_collection to be empty
            synchronized (outstanding_collection) {
                if (outstanding_collection.isEmpty() && naive_sync_balance) outstanding_collection.notifyAll();
            }
        }
    }

    /**
     * 
     * @param members
     */
    public void handleMemberShipChange(SpreadGroup[] members) {
        // If a member leaves the exection, update member list but otherwise ignore it.

        // If a member joins, update member list, 
        //   and if members same as num_of_replicas, signal to main that the program can start to execute the commands

        // If we get a new member after starting the execution, we need to handle the new member
        // 
        // This should be done by changing mode to syncing, pausing outstanding messages (and the incomming commands)
        //          the program should let the user know that it is syncing the new member
        //      in sync mode, the program should send a setBalance transaction to all members
        //      and wait for receiving all setBalance from all members-1 (not the new member)
        //          if any of these differ, we print out a message to the user that the servers are not synced, and exit the program
        //      If everything ok,  we can resume execution and broadcasting
    }

    /**
     * 
     * @param balance
     */
    private void handleSetBalance(double balance) {
        // TODO: handle what happens if setbalance

        // If the balance is 0.0, we know that we are the new member. Then, we can also set our status to syncing, to prevent us from running.
        //  we also will simply set our balance to be the incomming balance then.

        // Else, this client is either an existing member, or the new member in sync mode. In both cases we should now be in sync mode. 
        //      If the client is not in sync mode, e.g. has not recieved new member message, there is inconsistency, and the program should let user know and exit

        // if we are in sync mode, we can check if incomming and local balance is the same
        //    if not, there is inconsistency, and the program should let user know and exit

        // If after handling the setBalance transaction, either if balance was set to a new value, or it was checked if correct,
        //   we should increment how many times we have received a setBalance
        //      if this is equal to members size - 1, we have received all setBalance, and should continue executing by setting sync mode to false
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

}