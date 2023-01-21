package org.example.Zadanie2;

import java.util.ArrayList;
import java.util.Optional;

public class Bank {

    private ArrayList<Account> accounts;

    public Bank(){
        accounts=new ArrayList<>();
    }

    public void addAccount(Account account){
        accounts.add(account);
    }

    public boolean makeTransfer(Account sourceAccount, Account destinationAccount, double amount){

        if(amount<0 || !accounts.contains(sourceAccount)){
            return false;
        }

        return sourceAccount.transfer(destinationAccount,amount);
    }

}
