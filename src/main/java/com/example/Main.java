package com.example;

import spread.*;

import javax.swing.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.http.WebSocket;
import java.util.Random;


public class Main {

    public static void main(String[] args) throws InterruptedException {
        SpreadConnection connection = new SpreadConnection();
        Listener listener = new Listener();
        Random rand = new Random();
        int id = rand.nextInt();
        try {

            connection.add(listener);

            // if the ifi machine is used <use the ifi machine ip address>
            //connection.connect(InetAddress.getByName("129.240.65.59"), 4803, "test connection", false, true);

            // for the local machine (172.18.0.1 is the loopback address in this machine)
            connection.connect(InetAddress.getByName("127.0.0.1"), 4803, String.valueOf(id), false, true);

            SpreadGroup group = new SpreadGroup();
            group.join(connection, "group");
            SpreadMessage message = new SpreadMessage();
            message.addGroup(group);
            message.setFifo();
            message.setObject("client name : "+id);
            connection.multicast(message);

        } catch (SpreadException e) {
            throw new RuntimeException(e);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }


        //System.out.println("Hello world!");
        Thread.sleep(100000000);
    }

}