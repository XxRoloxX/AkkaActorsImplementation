package org.example.Zadanie3;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

public class Bottling extends AbstractBehavior<Production.Commands> {
    private static final double REQUIRED_FILTERED_WINE=0.75;

    private static final double REQUIRED_BOTTLES=1;

    private static final double WINE_OUTPUT=10;

    private static final double FAILURE_PROBABILITY = 0.03;

    private static final double TIME_TO_PRODUCE = 12;
    private Bottling(ActorContext<Production.Commands> context, double timeModifier) {
        super(context);
        occupied=false;
        amountOfWine=0;
        amountOfFilteredWine=0;
        amountOfBottles=0;
        this.timeModifier=timeModifier;
        reservedResources = new HashMap<>();

    }

    private double amountOfFilteredWine;
    private int amountOfBottles;
    private double amountOfWine;

    private boolean occupied;

    private double timeModifier;

    private HashMap<ActorRef<Production.Commands>,WineTransferResponse> reservedResources;

    private ArrayList<ActorRef<Production.Commands>> warehouses;
    private ArrayList<ActorRef<Production.Commands>>filtrationStations;
    public boolean isOccupied() {
        return occupied;
    }

    public static Behavior<Production.Commands> create(double timeModifierArg){
        return Behaviors.setup(context-> new Bottling(context,timeModifierArg));
    }

    @Override
    public Receive<Production.Commands> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(Production.ReportState.INSTANCE,this::onReportState)
                .onMessageEquals(Production.triggerProduction.INSTANCE,this::onTriggerProduction)
                .onMessage(Production.InitializeProduction.class,this::onInitializeProduction)
                .onMessage(Filtration.FilteredWineTransferResponse.class,this::onFilteredWineTransferResponse)
                .onMessage(WineTransferAcknowledgement.class,this::onWineTransferAcknowledgement)
                .onMessage(WineTransferRequest.class,this::onWineTransferRequest)
                .onMessage(Warehouse.ResourcesTransferResponse.class,this::onBottleTransferResponse)
                .build();
    }
    public boolean produce(){
        Random rand = new Random();
        //onReportState();
        if(amountOfFilteredWine>=REQUIRED_FILTERED_WINE && amountOfBottles>=REQUIRED_BOTTLES && !occupied){
            occupied=true;


            Utils.waitFor((int)(TIME_TO_PRODUCE*1000*timeModifier));


            amountOfBottles-=REQUIRED_BOTTLES;
            amountOfFilteredWine-=REQUIRED_FILTERED_WINE;

            if(rand.nextDouble()>FAILURE_PROBABILITY){
                amountOfWine+=WINE_OUTPUT;
            }

            occupied=false;

            return true;

        }else{
            return false;
        }
    }

    private Behavior<Production.Commands>onReportState(){
        System.out.println("Bottling Station Resources: ");
        System.out.println("Filtered Wine: "+amountOfFilteredWine);
        System.out.println("Wine: "+amountOfWine);
        System.out.println("Bottles: "+amountOfBottles);
        return this;

    }
    private Behavior<Production.Commands>onBottleTransferResponse(Warehouse.ResourcesTransferResponse commands){
        amountOfBottles+=commands.bottles;
        commands.from.tell(new Warehouse.ResourceTransferAcknowledgement(getContext().getSelf(),0,0,0, commands.bottles));
        getContext().getLog().info("Received Bottle Transfer Response: {}", commands);
        onReportState();
        return this;
    }
    private Behavior<Production.Commands>onFilteredWineTransferResponse(Filtration.FilteredWineTransferResponse commands){
        amountOfFilteredWine+= commands.filteredWine;
        commands.from.tell(new Filtration.FilteredWineTransferAcknowledgement(getContext().getSelf(), commands.filteredWine));
        getContext().getLog().info("Received Filtered Wine Response: {}", commands);
        onReportState();

        return this;
    }

    private Behavior<Production.Commands> onTriggerProduction(){


        for(ActorRef<Production.Commands> warehouse: warehouses){
            warehouse.tell(Production.triggerProduction.INSTANCE);
        }
        for(ActorRef<Production.Commands> filtration: filtrationStations){
            filtration.tell(Production.triggerProduction.INSTANCE);
        }

        if(!produce()){
            for(ActorRef<Production.Commands> warehouse: warehouses){
                warehouse.tell(new Warehouse.ResourcesTransferRequest(getContext().getSelf(),  0,0,0,(int)Math.max(REQUIRED_BOTTLES-amountOfBottles,0)));
            }
            for(ActorRef<Production.Commands>filtration: filtrationStations){
                filtration.tell(new Filtration.FilteredWineTransferRequest(getContext().getSelf(),Math.max(REQUIRED_FILTERED_WINE-amountOfFilteredWine,0)));
            }
        }

        return this;
    }




    private Behavior<Production.Commands> onInitializeProduction(Production.InitializeProduction commands){
        warehouses= commands.warehouses;
        filtrationStations= commands.filtrationStations;
        return this;
    }
    private WineTransferResponse getAllReservedResources(){
        //ResourcesTransferRequest result = new ResourcesTransferRequest(getContext().getSelf(),0,0,0,0);
        WineTransferResponse element;
        double wine =0;

        Set<ActorRef<Production.Commands>> keys= reservedResources.keySet();
        for(ActorRef<Production.Commands> key: keys){
            element = reservedResources.get(key);
            wine += element.wine;
        }

        return new WineTransferResponse(getContext().getSelf(),wine);
    }

    private Behavior<Production.Commands>onWineTransferRequest(WineTransferRequest commands){
       // commands.from.tell(new WineTransferResponse(getContext().getSelf(),Math.min(amountOfWine,commands.wine)));
        reservedResources.put(commands.from, new WineTransferResponse(commands.from,0));

        WineTransferResponse loanedResources = new WineTransferResponse(
                getContext().getSelf(), Math.min(Math.max(amountOfWine- getAllReservedResources().wine,0), commands.wine));


        commands.from.tell(loanedResources);
        reservedResources.put(commands.from,loanedResources);
        getContext().getLog().info("Received Bottle Transfer Response: {}", commands);
        return this;
    }
    private Behavior<Production.Commands>onWineTransferAcknowledgement(WineTransferRequest commands){
        //amountOfWine-= commands.wine;
        amountOfWine -= reservedResources.get(commands.from).wine;
        reservedResources.put(commands.from, new WineTransferResponse(commands.from,0));
        getContext().getLog().info("Received Bottle Transfer Response: {}", commands);
        return this;
    }



    public static class WineTransferRequest implements Production.Commands {
        public final double wine;
        public final ActorRef<Production.Commands> from;


        public WineTransferRequest(ActorRef<Production.Commands> from, double wine){
            this.wine=wine;
            this.from = from;

        }

        @Override
        public String toString() {
            return "WineTransferRequest{" +
                    "wine=" + wine +
                    ", from=" + from +
                    '}';
        }
    }
    public static class WineTransferResponse extends WineTransferRequest {
        public WineTransferResponse(ActorRef<Production.Commands> from, double wine){
            super(from, wine);

        }

    }
    public static class WineTransferAcknowledgement extends WineTransferRequest {
        public WineTransferAcknowledgement(ActorRef<Production.Commands> from, double wine){
            super(from, wine);

        }

    }
}
