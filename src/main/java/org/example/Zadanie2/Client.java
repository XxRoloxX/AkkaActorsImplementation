package org.example.Zadanie2;

public class Client {
    private final String name;
    private final String surname;
    private Account account;

    public Client(String name, String surname){
        this.name=name;
        this.surname=surname;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }
}
