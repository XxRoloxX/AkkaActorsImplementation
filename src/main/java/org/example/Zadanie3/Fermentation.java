package org.example.Zadanie3;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.Utils;

import java.util.Date;
import java.util.Random;

public class Fermentation extends AbstractBehavior<Production.Commands> {

    private static final double REQUIRED_GRAPE_JUICE=15;

    private static final double REQUIRED_WATER=8;

    private static final double REQUIRED_SUGAR=2;


    private static final double UNFILTERED_WINE_OUTPUT=25;

    private static final double FAILURE_PROPABILITY = 0.05;

    private static final double TIME_TO_PRODUCE = 12*14;
    private double amountOfSugar;

    private double amountOfGrapeJuice;

    private double amountOfWater;

    private double amountOfUnfilteredWine;

    private ActorRef<Production.Commands> warehouse;

    private ActorRef<Production.Commands> winePress;


    private boolean occupied;

    private double timeModifier;
    private Fermentation(ActorContext<Production.Commands> context, double timeModifier) {
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

    //public interface FermentationCommands {};

    public enum ReportState implements Production.Commands {
        INSTANCE;
    }


    public static Behavior<Production.Commands> create(double timeModifierArg){
        return Behaviors.setup(context-> new  Fermentation(context,timeModifierArg));
    }

    @Override
    public Receive<Production.Commands> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(ReportState.INSTANCE,this::onReportState)
                .onMessageEquals(Production.triggerProduction.INSTANCE,this::onTriggerProduction)
                .onMessage(WinePress.GrapeJuiceTransferResponse.class,this::onGrapeJuiceTransportResponse)
                .onMessage(Production.InitializeProduction.class,this::onInitializeProduction)
                .onMessage(Warehouse.ResourcesTransferResponse.class,this::onWarehouseTransportResponse)
                .build();
    }

    private Behavior<Production.Commands> onTriggerProduction(){

        winePress.tell(Production.triggerProduction.INSTANCE);
        warehouse.tell(Production.triggerProduction.INSTANCE);
        onReportState();

            if(!produce()){
                warehouse.tell(new Warehouse.ResourcesTransferRequest(getContext().getSelf(), 0,
                        Math.max(REQUIRED_WATER-amountOfWater,0),Math.max(REQUIRED_SUGAR-amountOfSugar,0),0));
                winePress.tell(new WinePress.GrapeJuiceTransferRequest(getContext().getSelf(),Math.max(REQUIRED_GRAPE_JUICE-amountOfGrapeJuice,0)));
            }
            return this;

    }
    public boolean produce(){
        Random rand = new Random();
        if(amountOfGrapeJuice>=REQUIRED_GRAPE_JUICE && amountOfSugar>=REQUIRED_SUGAR &&
               amountOfWater>=REQUIRED_WATER && !occupied){
            occupied=true;

            Utils.waitFor((int)(TIME_TO_PRODUCE*1000*timeModifier));

            occupied=false;
            amountOfWater-=REQUIRED_WATER;
            amountOfSugar-=REQUIRED_SUGAR;
            amountOfGrapeJuice-=REQUIRED_GRAPE_JUICE;



            if(rand.nextDouble()>FAILURE_PROPABILITY){
                amountOfUnfilteredWine+=UNFILTERED_WINE_OUTPUT;
            }
            //onReportState();
            occupied=false;

            return true;

        }else{
            return false;
        }
    }

    private Behavior<Production.Commands>onInitializeProduction(Production.InitializeProduction commands){
        this.warehouse=commands.warehouse;
        this.winePress=commands.winePress;
        return this;
    }

    private Behavior<Production.Commands>onReportState(){
        System.out.println("Resources: ");
        System.out.println("Sugar: "+amountOfSugar);
        System.out.println("Water: "+amountOfWater);
        System.out.println("Grape Juice: "+amountOfGrapeJuice);
        System.out.println("Unfiltered Wine: "+amountOfUnfilteredWine);

        return this;

    }



    private Behavior<Production.Commands> onGrapeJuiceTransportResponse(WinePress.GrapeJuiceTransferResponse commands){

        /*
        commands.from.tell(new GrapeJuiceTransferRequest(getContext().getSelf(),
                Math.min(commands.grapeJuice, amountOfGrapeJuice)));
        amountOfGrapes -= Math.min(commands.grapeJuice, amountOfGrapeJuice);
        */
        //onReportState();
        commands.from.tell(new WinePress.GrapeJuiceTransferAcknowledgement(getContext().getSelf(), commands.grapeJuice));
        amountOfGrapeJuice+= commands.grapeJuice;

        return this;

    }
    private Behavior<Production.Commands> onWarehouseTransportResponse(Warehouse.ResourcesTransferResponse commands){

        /*
        commands.from.tell(new GrapeJuiceTransferRequest(getContext().getSelf(),
                Math.min(commands.grapeJuice, amountOfGrapeJuice)));
        amountOfGrapes -= Math.min(commands.grapeJuice, amountOfGrapeJuice);
        */
        commands.from.tell(new Warehouse.ResourceTransferAcknowledgement(getContext().getSelf(), commands.grapes,commands.water,commands.sugar,commands.bottles));
        amountOfSugar+= commands.sugar;
        amountOfWater+= commands.water;

        return this;

    }




}
