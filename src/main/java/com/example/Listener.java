package com.example;

import java.util.Collection;

import spread.*;

public class Listener implements AdvancedMessageListener {
    private final Client client;

    public Listener(Client client) {
        this.client = client;
    }
    
    public void regularMessageReceived(SpreadMessage message) {
        Collection<Transaction> transactions = null;
        try {
            client.processReceivedTransactions((Collection<Transaction>) message.getObject());
        } catch (SpreadException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        // Send updated member list and notification to client
        SpreadGroup[] members = spreadMessage.getMembershipInfo().getMembers();
        System.out.println("New membership change: " + members);
        client.handleMemberShipChange(members);
    }

}