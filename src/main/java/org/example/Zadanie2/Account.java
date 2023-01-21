package org.example.Zadanie2;

import java.util.Objects;

public class Account {
    private final int clientID;
    private double balance;

    public Account(int clientID){
        this.clientID=clientID;
        balance=0;
    }

    public int  getClientID() {
        return clientID;
    }

    public synchronized double getBalance() {
        return balance;
    }

    public synchronized boolean payout(double amount){
        if(amount>balance || amount<0){
            return false;
        }else{
            balance-=amount;
            return true;
        }
    }
    public synchronized boolean receiveFunds(double amount){
        if(amount<0){
            return false;
        }else{
            balance+=amount;
            return true;
        }
    }

    public boolean transfer(Account destinationAccount, double amount){
        synchronized (this){
            if(amount<0 || destinationAccount==null || amount>balance || destinationAccount.equals(this)){
                return false;
            }
            balance-=amount;
        }
            return destinationAccount.receiveFunds(amount);
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Account account = (Account) o;
        return clientID == account.clientID;
    }

    @Override
    public String toString() {
        return "Account{" +
                "clientID=" + clientID +
                ", balance=" + balance +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientID, balance);
    }
}
