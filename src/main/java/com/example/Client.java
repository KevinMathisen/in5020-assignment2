package com.example;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadGroup;
import spread.SpreadMessage;

public class Client {
    private double balance;
    private int order_counter;
    private int outstanding_counter;
    private final List<Transaction> executed_list;
    private final Collection<Transaction> outstanding_collection;

    private final String server_address;
    private final String account_name;
    private final String file_name;

    private final SpreadConnection spread_connection;
    private final Listener spread_listener;
    private final SpreadGroup spread_group;
    private final int id;

    public Client(int id, String server_address, String account_name, String file_name) throws InterruptedException {
        // Initialize to 0 and empty lists
        this.balance = 0.0;
        this.order_counter = 0;
        this.outstanding_counter = 0;
        this.executed_list = new ArrayList<>();
        this.outstanding_collection = new ArrayList<>();

        // Set arguments from command line
        this.server_address = server_address;
        this.account_name = account_name;
        this.file_name = file_name;

        // Initialize connection and spead listener
        this.spread_connection = new SpreadConnection();
        this.spread_listener = new Listener();
        this.id = id;

        // Connect to spread server, connect the listener, and join the spread group
        try {
            spread_connection.add(spread_listener);
            spread_connection.connect(InetAddress.getByName(server_address), 4803, String.valueOf(id), false, true);

            this.spread_group = new SpreadGroup();
            this.spread_group.join(spread_connection, "group");
        } catch (SpreadException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        sendMessage("Hello world! I am client : "+id);
    }

    public static void main(String[] args) throws InterruptedException {
        String server_address = "127.0.0.1";
        String account_name = "Group10";
        String file_name = "";
        int num_of_replica = 1;

        Client[] clients = new Client[num_of_replica];

        Random rand = new Random();

        for (int i = 0; i < num_of_replica; i++) {
            clients[i] = new Client(rand.nextInt(), server_address, account_name, file_name);
        }

        System.out.println("Hello world!");
        Thread.sleep(100000000);
    }

    private void sendMessage(String message_content) throws InterruptedException {
        try {
            SpreadMessage message = new SpreadMessage();
            message.addGroup(spread_group);
            message.setFifo();
            message.setObject(message_content);
            spread_connection.multicast(message);
        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }

    private double getQuickBalance() {
        return balance;
    }

    private double getSyncedBalance() {
        return balance;
    }

    private void deposit(double amount) {
        balance += amount;
    }

    private void addInterest(double percent) {
        balance += balance * (percent / 100.0);
    }
    
    private List<Transaction> getHistory() {
        // Return the list of executed transactions
        return new ArrayList<>(executed_list);
    }
    
    private String checkTxStatus(String transactionId) {
        for (Transaction transaction : executed_list) {
            if (transaction.uniqueId.equals(transactionId)) {
                return "Not applied";
            } else {
                return "Applied";
            }
        }
        return "Not Found";
    }
    
    private void cleanHistory() {
        executed_list.clear();
    }
    
    private List<String> memberInfo() {
        List<String> members = new ArrayList<>();
        return members;
    }
    
    private void sleep(int duration) {
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