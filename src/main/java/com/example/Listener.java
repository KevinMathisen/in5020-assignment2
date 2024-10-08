package com.example;

import spread.*;

public class Listener implements AdvancedMessageListener {
    public void regularMessageReceived(SpreadMessage message) {
        String msg = null;
        try {
            msg = (String) message.getObject();
        } catch (SpreadException e) {
            throw new RuntimeException(e);
        }
        System.out.println("New incomming message: " + msg);
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        System.out.println("New membership change: " + spreadMessage.getMembershipInfo().getMembers());
    }

}