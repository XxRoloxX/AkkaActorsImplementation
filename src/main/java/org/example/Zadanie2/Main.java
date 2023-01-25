package org.example.Zadanie2;

import java.util.ArrayList;

public class Main {

    public static void cooking(int accountsAmount, int timeToLive, int initialFunds){
        ArrayList<Account> accounts = new ArrayList<>();
        ArrayList<Cook> cooks = new ArrayList<>();
        Thread[] threads = new Thread[accountsAmount];

        for(int i=0;i<accountsAmount;i++){
            accounts.add(new Account(i));
            accounts.get(i).receiveFunds(initialFunds);
            cooks.add(new Cook(timeToLive,accounts.get(i),accounts));
            threads[i] = new Thread(cooks.get(i));
        }

        for(Thread t: threads){
            t.start();
        }

        try {
            for (Thread t : threads) {
                t.join();
            }
        }catch (InterruptedException e){
            System.out.println(e);
        }

        System.out.println("Transfers completed");

        for(Account a:accounts){
            System.out.println(a);
        }

    }

    public static void main(String[] args){
        cooking(10,1000,1000);
    }
}
