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
import java.util.Set;
import java.util.TreeSet;

public class Warehouse extends AbstractBehavior<Production.Commands> {
    private double amountOfGrapes;
    private double amountOfWater;
    private double amountOfSugar;
    private int amountOfBottles;

    //public interface WarehouseCommands extends Production.Commands {};

    private HashMap<ActorRef<Production.Commands>,ResourcesTransferResponse> reservedResources;

    private Warehouse(ActorContext<Production.Commands> context) {
        super(context);
    }
    private Warehouse(ActorContext<Production.Commands> context, double amountOfGrapes, double amountOfWater
    ,double amountOfSugar, int amountOfBottles) {
        super(context);
        this.amountOfGrapes = amountOfGrapes;
        this.amountOfWater = amountOfWater;
        this.amountOfSugar = amountOfSugar;
        this.amountOfBottles = amountOfBottles;
        reservedResources = new HashMap<>();

    }

    public static Behavior<Production.Commands> create(double amountOfGrapes, double amountOfWater
            ,double amountOfSugar, int amountOfBottles){
        return Behaviors.setup(context->new Warehouse(context,amountOfGrapes,amountOfWater,amountOfSugar,amountOfBottles));
    }

    @Override
    public Receive<Production.Commands> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(Production.ReportState.INSTANCE, this::onReportState)
                .onMessage(ResourceTransferAcknowledgement.class,this::onResourceTransferAcknowledgement)
                .onMessage(ResourcesTransferRequest.class,this::onResourceTransferRequest)
                .build();
    }


    private Behavior<Production.Commands>onReportState(){
        System.out.println("Warehouse Resources: ");
        System.out.println("Grapes: "+amountOfGrapes);
        System.out.println("Water: "+amountOfWater);
        System.out.println("Sugar: "+amountOfSugar);
        System.out.println("Bottles: "+amountOfBottles);
        return this;

    }

    private Behavior<Production.Commands> onResourceTransferAcknowledgement(ResourceTransferAcknowledgement commands){
        amountOfGrapes -= commands.grapes;
        amountOfWater -= commands.water;
        amountOfSugar -=commands.sugar;
        amountOfBottles -= commands.bottles;
        reservedResources.put(commands.from,new ResourcesTransferResponse(commands.from,0,0,0,0));
        getContext().getLog().info("Received Resource Transfer Acknowledgement: {}", commands);
        onReportState();
        return this;
    }

    private ResourcesTransferResponse getAllReservedResources(){
        //ResourcesTransferRequest result = new ResourcesTransferRequest(getContext().getSelf(),0,0,0,0);
        ResourcesTransferRequest element;
        double grapes=0;
        double sugar=0;
        double water=0;
        int bottles=0;

        Set<ActorRef<Production.Commands>> keys= reservedResources.keySet();
        for(ActorRef<Production.Commands> key: keys){
            element = reservedResources.get(key);
            grapes += element.grapes;
            sugar += element.sugar;
            water +=element.water;
            bottles +=element.bottles;

        }
        return new ResourcesTransferResponse(getContext().getSelf(),grapes,water,sugar,bottles);
    }



    private Behavior<Production.Commands> onResourceTransferRequest(ResourcesTransferRequest commands){

        reservedResources.put(commands.from,new ResourcesTransferResponse(commands.from, 0,0,0,0));
        ResourcesTransferRequest reserved = getAllReservedResources();
        onReportState();
        ResourcesTransferResponse loanedResources = new ResourcesTransferResponse(getContext().getSelf(),
                Math.min(commands.grapes, Math.max(amountOfGrapes-reserved.grapes,0)),
                Math.min(commands.water, Math.max(amountOfWater-reserved.water,0)),
                Math.min(commands.sugar, Math.max(amountOfSugar-reserved.sugar,0)),
                Math.min(commands.bottles, Math.max(amountOfBottles-reserved.bottles,0)));

        commands.from.tell(loanedResources);

        reservedResources.put(commands.from,(loanedResources));

        getContext().getLog().info("Received Resource Transfer Request: {}", commands);

        //onReportState();
        return this;

    }

    public static class ResourcesTransferRequest implements Production.Commands {
        public final double grapes;
        public final double water;

        public final double sugar;

        public final int bottles;

        public final ActorRef<Production.Commands> from;


        public ResourcesTransferRequest(ActorRef<Production.Commands> from, double grapes, double water, double sugar, int bottles){
            this.grapes=grapes;
            this.water = water;
            this.sugar = sugar;
            this.bottles=bottles;
            this.from = from;
        }

        @Override
        public String toString() {
            return "ResourcesTransferRequest{" +
                    "grapes=" + grapes +
                    ", water=" + water +
                    ", sugar=" + sugar +
                    ", bottles=" + bottles +
                    ", from=" + from +
                    '}';
        }
    }
    public static class ResourcesTransferResponse extends ResourcesTransferRequest {

        public ResourcesTransferResponse(ActorRef<Production.Commands> from, double grapes, double water, double sugar, int bottles){
            super(from, grapes,water,sugar,bottles);
        }
    }

    public static class ResourceTransferAcknowledgement extends ResourcesTransferRequest{
        public ResourceTransferAcknowledgement(ActorRef<Production.Commands> from, double grapes, double water, double sugar, int bottles){
            super(from, grapes,water,sugar,bottles);
        }
    }

}
