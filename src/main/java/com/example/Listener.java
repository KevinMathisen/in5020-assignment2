package com.example;

import java.util.Collection;

import spread.*;

public class Listener implements AdvancedMessageListener {
    private final Client client;

    public Listener(Client client) {
        this.client = client;
    }
    
    /**
     * Handles reception of outstanding_collections, sending it to the client for execution
     * @param message - The incomming message
     */
    public void regularMessageReceived(SpreadMessage message) {
        try {
            // Send the received transactions to the client for execution
            client.processReceivedTransactions((Collection<Transaction>) message.getObject());
        } catch (SpreadException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Handles reception of membership updates, sending the updated membership to the client
     */
    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        // Send updated member list to client
        SpreadGroup[] members = spreadMessage.getMembershipInfo().getMembers();
        client.handleMemberShipChange(members);

        // For debugging 
        System.out.println("New membership change: " + members);
    }

}