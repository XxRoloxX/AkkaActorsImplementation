package org.example.Zadanie3;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.Utils;

public class Warehouse extends AbstractBehavior<Warehouse.WarehouseCommands> {
    private double amountOfGrapes;
    private double amountOfWater;
    private double amountOfSugar;
    private int amountOfBottles;

    public interface WarehouseCommands extends Production.Commands {};

    private Warehouse(ActorContext<WarehouseCommands> context) {
        super(context);
    }
    private Warehouse(ActorContext<WarehouseCommands> context, double amountOfGrapes, double amountOfWater
    ,double amountOfSugar, int amountOfBottles) {
        super(context);
        this.amountOfGrapes = amountOfGrapes;
        this.amountOfWater = amountOfWater;
        this.amountOfSugar = amountOfSugar;
        this.amountOfBottles = amountOfBottles;

    }

    public static Behavior<WarehouseCommands> create(double amountOfGrapes, double amountOfWater
            ,double amountOfSugar, int amountOfBottles){
        return Behaviors.setup(context->new Warehouse(context,amountOfGrapes,amountOfWater,amountOfSugar,amountOfBottles));
    }

    @Override
    public Receive<WarehouseCommands> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(ReportState.INSTANCE, this::onReportState)
                .onMessage(ResourcesTransferRequest.class,this::onResourceTransferRequest)
                .build();
    }
    public enum ReportState implements WarehouseCommands {
        INSTANCE
    }

    private Behavior<WarehouseCommands>onReportState(){
        System.out.println("Resources: ");
        System.out.println("Grapes: "+amountOfGrapes);
        System.out.println("Water: "+amountOfWater);
        System.out.println("Sugar: "+amountOfSugar);
        System.out.println("Bottles: "+amountOfBottles);
        return this;

    }



    private Behavior<WarehouseCommands> onResourceTransferRequest(ResourcesTransferRequest commands){

        commands.from.tell(new ResourcesTransferResponse(getContext().getSelf(),
                Math.min(commands.grapes, amountOfGrapes),
                Math.min(commands.water, amountOfWater),
                Math.min(commands.sugar, amountOfSugar),
                Math.min(commands.bottles, amountOfBottles)));

        amountOfGrapes -= Math.min(commands.grapes, amountOfGrapes);
        amountOfWater -= Math.min(commands.water, amountOfWater);
        amountOfSugar -=Math.min(commands.sugar, amountOfSugar);
        amountOfBottles -= Math.min(commands.bottles, amountOfBottles);


        return this;

    }

    public static class ResourcesTransferRequest implements WarehouseCommands {
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


    }
    public static class ResourcesTransferResponse implements WinePress.WinePressCommands, WarehouseCommands, Fermentation.FermentationCommands {
        public final double grapes;
        public final double water;
        public final double sugar;
        public final int bottles;

        public final ActorRef<WarehouseCommands> from;


        public ResourcesTransferResponse(ActorRef<WarehouseCommands> from, double grapes, double water, double sugar, int bottles){
            this.grapes=grapes;
            this.water = water;
            this.sugar = sugar;
            this.bottles=bottles;
            this.from = from;
        }


    }

}
