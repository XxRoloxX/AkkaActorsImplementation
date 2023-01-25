package org.example.Zadanie3;

import akka.actor.typed.ActorSystem;

public class Main {
    public static void main(String[] args){

        ActorSystem<Production.Commands> mySystem = ActorSystem.create(Production.create(),"mySystemm");
        mySystem.tell(new Production.StartProduction(200,200,200,200,0.00001));

    }
}
