package org.example.Zadanie2;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class Cook implements Runnable{
    private final long timeToLive;

    private static final int MULTIPLIER = 500;

    private final Account account;

    private final ArrayList<Account> remoteAccounts;

    public Cook(int timeToLive, Account account, ArrayList<Account>remoteAccounts){
        this.timeToLive = timeToLive;
        this.account=account;
        this.remoteAccounts = remoteAccounts;

    }


    public void run(){
        Random rand = new Random();
        long startTime;
        Date date = new Date();
        startTime= date.getTime();

        while(date.getTime()<(startTime+timeToLive)){
            date = new Date();
           // System.out.println(date.getTime() + ", "+(startTime+timeToLive));

            account.transfer(remoteAccounts.get(rand.nextInt(remoteAccounts.size())),rand.nextDouble()*MULTIPLIER);

        }
       // System.out.println("Out of loop!");

    }

}
