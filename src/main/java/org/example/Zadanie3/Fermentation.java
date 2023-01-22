package org.example.Zadanie3;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.Date;
import java.util.Random;

public class Fermentation extends AbstractBehavior<Fermentation.FermentationCommands> {

    private static final double REQUIRED_GRAPE_JUICE=15;

    private static final double REQUIRED_WATER=8;

    private static final double REQUIRED_SUGAR=2;


    private static final double UNFILTERED_WINE_OUTPUT=25;

    private static final double FAILURE_PROPABILITY = 5;

    private static final double TIME_TO_PRODUCE = 12*14;
    private double amountOfSugar;

    private double amountOfGrapeJuice;

    private double amountOfWater;

    private double amountOfUnfilteredWine;


    private boolean occupied;

    private double timeModifier;
    private Fermentation(ActorContext<FermentationCommands> context, double timeModifier) {
        super(context);
        amountOfGrapeJuice=0;
        amountOfSugar=0;
        amountOfWater = 0;
        occupied=false;
        this.timeModifier=timeModifier;
    }


    public boolean isOccupied() {
        return occupied;
    }

    public interface FermentationCommands {};

    public enum ReportState implements FermentationCommands {
        INSTANCE;
    }


    public static Behavior< FermentationCommands> create(double timeModifierArg){
        return Behaviors.setup(context-> new  Fermentation(context,timeModifierArg));
    }

    @Override
    public Receive<FermentationCommands> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(ReportState.INSTANCE,this::onReportState)
                .onMessage(WinePress.GrapeJuiceTransferRequest.class,this::onGrapeJuiceTransportResponse)
                .onMessage(Warehouse.ResourcesTransferResponse.class,this::onWarehouseTransportResponse)
                .build();
    }
    public boolean produce(){
        Random rand = new Random();
        if(amountOfGrapeJuice>=REQUIRED_GRAPE_JUICE && amountOfSugar>=REQUIRED_SUGAR &&
               amountOfWater>=REQUIRED_WATER && !occupied){
            occupied=true;

            try{
                Thread.sleep((long) (TIME_TO_PRODUCE*1000));
            }catch(InterruptedException e){
                System.out.println(e);
            }
            occupied=false;
            amountOfWater-=REQUIRED_WATER;
            amountOfSugar-=REQUIRED_SUGAR;
            amountOfGrapeJuice-=REQUIRED_GRAPE_JUICE;



            if(rand.nextDouble()>FAILURE_PROPABILITY){
                amountOfUnfilteredWine+=UNFILTERED_WINE_OUTPUT;
            }

            return true;

        }else{
            return false;
        }
    }

    private Behavior< FermentationCommands>onReportState(){
        System.out.println("Resources: ");
        System.out.println("Sugar: "+amountOfSugar);
        System.out.println("Water: "+amountOfWater);
        System.out.println("Grape Juice: "+amountOfGrapeJuice);
        System.out.println("Unfiltered Wine: "+amountOfUnfilteredWine);

        return this;

    }



    private Behavior<FermentationCommands> onGrapeJuiceTransportResponse(WinePress.GrapeJuiceTransferRequest commands){

        /*
        commands.from.tell(new GrapeJuiceTransferRequest(getContext().getSelf(),
                Math.min(commands.grapeJuice, amountOfGrapeJuice)));
        amountOfGrapes -= Math.min(commands.grapeJuice, amountOfGrapeJuice);
        */
        amountOfGrapeJuice+= commands.grapeJuice;

        return this;

    }
    private Behavior<FermentationCommands> onWarehouseTransportResponse(Warehouse.ResourcesTransferResponse commands){

        /*
        commands.from.tell(new GrapeJuiceTransferRequest(getContext().getSelf(),
                Math.min(commands.grapeJuice, amountOfGrapeJuice)));
        amountOfGrapes -= Math.min(commands.grapeJuice, amountOfGrapeJuice);
        */
        amountOfSugar+= commands.sugar;
        amountOfWater-= commands.water;

        return this;

    }




}
