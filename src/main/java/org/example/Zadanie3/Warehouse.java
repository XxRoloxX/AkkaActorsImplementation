package org.example.Zadanie3;
import akka.actor.AbstractActor;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

public class Warehouse extends AbstractBehavior<Warehouse.WarehouseCommands> {
    private double amountOfGrapes;
    private double amountOfWater;
    private double amountOfSugar;
    private int amountOfBottles;

    private Warehouse(ActorContext<WarehouseCommands> context) {
        super(context);
    }

    public static Behavior<WarehouseCommands> create(){
        return Behaviors.setup(context->new Warehouse(context));
    }

    @Override
    public Receive<WarehouseCommands> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(ReportState.INSTANCE, this::onReportState)
                .onMessage(resourcesTransfer.class, )
                .build();
    }

    private Behavior<WarehouseCommands>onReportState(){
        System.out.println("Resources: ");
        System.out.println("Grapes: "+amountOfGrapes);
        System.out.println("Water: "+amountOfWater);
        System.out.println("Sugar: "+amountOfSugar);
        System.out.println("Bottles: "+amountOfBottles);
        return this;

    }

    interface WarehouseCommands {};


    public enum ReportState implements WarehouseCommands {
        INSTANCE
    }

    private Behavior<WarehouseCommands> onResourceTransfer(resourcesTransfer commands){
        if(commands.grapes>amountOfGrapes || commands.water>amountOfWater || commands.bottles > amountOfBottles || commands.sugar>amountOfSugar){
           getSelf();
        }


    }

    public static class resourcesTransfer implements WarehouseCommands {
        public final double grapes;
        public final double water;

        public final double sugar;

        public final int bottles;


        public resourcesTransfer(double grapes,double water,double sugar,int bottles){
            this.grapes=grapes;
            this.water = water;
            this.sugar = sugar;
            this.bottles=bottles;
        }


    }
}
