package org.example.Zadanie3;
import akka.actor.Actor;
import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import org.example.Utils;

import java.util.*;

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

    private ArrayList<ActorRef<Production.Commands>> warehouses;
    private ArrayList<ActorRef<Production.Commands>>winePresses;

    private HashMap<ActorRef<Production.Commands>,UnfilteredWineTransferResponse> reservedResources;


    private boolean occupied;

    private double timeModifier;
    private Fermentation(ActorContext<Production.Commands> context, double timeModifier) {
        super(context);
        amountOfGrapeJuice=0;
        amountOfSugar=0;
        amountOfWater = 0;
        occupied=false;
        this.timeModifier=timeModifier;
        reservedResources = new HashMap<>();

    }


    public boolean isOccupied() {
        return occupied;
    }

    //public interface FermentationCommands {};




    public static Behavior<Production.Commands> create(double timeModifierArg){
        return Behaviors.setup(context-> new  Fermentation(context,timeModifierArg));
    }

    @Override
    public Receive<Production.Commands> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(Production.ReportState.INSTANCE,this::onReportState)
                .onMessageEquals(Production.triggerProduction.INSTANCE,this::onTriggerProduction)
                .onMessage(WinePress.GrapeJuiceTransferResponse.class,this::onGrapeJuiceTransportResponse)
                .onMessage(Production.InitializeProduction.class,this::onInitializeProduction)
                .onMessage(UnfilteredWineTransferAcknowledgement.class,this::onUnfilteredWineTransferAcknowledgement)
                .onMessage(UnfilteredWineTransferRequest.class,this::onUnfilteredWineTransferRequest)
                .onMessage(Warehouse.ResourcesTransferResponse.class,this::onWarehouseTransportResponse)
                .build();
    }

    private Behavior<Production.Commands> onTriggerProduction(){


        for(ActorRef<Production.Commands> warehouse: warehouses){
            warehouse.tell(Production.triggerProduction.INSTANCE);
        }
        for(ActorRef<Production.Commands> winePress: winePresses){
            winePress.tell(Production.triggerProduction.INSTANCE);
        }
       // onReportState();

            if(!produce()){

                for(ActorRef<Production.Commands> warehouse: warehouses){
                    warehouse.tell(new Warehouse.ResourcesTransferRequest(getContext().getSelf(), 0,
                            Math.max(REQUIRED_WATER-amountOfWater,0),Math.max(REQUIRED_SUGAR-amountOfSugar,0),0));
                }

                for(ActorRef<Production.Commands>winePress: winePresses){
                    winePress.tell(new WinePress.GrapeJuiceTransferRequest(getContext().getSelf(),Math.max(REQUIRED_GRAPE_JUICE-amountOfGrapeJuice,0)));
                }

            }
            return this;

    }
    public boolean produce(){
        Random rand = new Random();
        if(amountOfGrapeJuice>=REQUIRED_GRAPE_JUICE && amountOfSugar>=REQUIRED_SUGAR &&
               amountOfWater>=REQUIRED_WATER && !occupied){
            occupied=true;

            Utils.waitFor((int)(TIME_TO_PRODUCE*1000*timeModifier));


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
        warehouses= commands.warehouses;
        winePresses= commands.winePresses;
        return this;
    }

    private Behavior<Production.Commands>onReportState(){
        System.out.println("Fermentation Station Resources: ");
        System.out.println("Sugar: "+amountOfSugar);
        System.out.println("Water: "+amountOfWater);
        System.out.println("Grape Juice: "+amountOfGrapeJuice);
        System.out.println("Unfiltered Wine: "+amountOfUnfilteredWine);

        return this;

    }
    private UnfilteredWineTransferResponse getAllReservedResources(){
        //ResourcesTransferRequest result = new ResourcesTransferRequest(getContext().getSelf(),0,0,0,0);
        UnfilteredWineTransferResponse element;
        double unfilteredWine =0;

        Set<ActorRef<Production.Commands>> keys= reservedResources.keySet();
        for(ActorRef<Production.Commands> key: keys){
            element = reservedResources.get(key);
            unfilteredWine += element.unfilteredWine;
        }

        return new UnfilteredWineTransferResponse(getContext().getSelf(),unfilteredWine);
    }

    public Behavior<Production.Commands> onUnfilteredWineTransferRequest(UnfilteredWineTransferRequest commands ){
        reservedResources.put(commands.from, new UnfilteredWineTransferResponse(commands.from,0));

        UnfilteredWineTransferResponse loanedResources = new UnfilteredWineTransferResponse(
                getContext().getSelf(), Math.min(Math.max(amountOfUnfilteredWine- getAllReservedResources().unfilteredWine,0), commands.unfilteredWine));

        commands.from.tell(loanedResources);
        reservedResources.put(commands.from, loanedResources);
        getContext().getLog().info("Received Filtered Wine Transfer Request: {}", commands);
        return this;
    }
    public Behavior<Production.Commands> onUnfilteredWineTransferAcknowledgement(UnfilteredWineTransferAcknowledgement commands ){
        amountOfUnfilteredWine -= reservedResources.get(commands.from).unfilteredWine;
        reservedResources.put(commands.from, new UnfilteredWineTransferResponse(commands.from,0));

        getContext().getLog().info("Received Filtered Wine Transfer Acknowledgmenet: {}", commands);
        return this;
    }


    public static class UnfilteredWineTransferRequest implements Production.Commands {
        public final double unfilteredWine;
        public final ActorRef<Production.Commands> from;

        public UnfilteredWineTransferRequest(ActorRef<Production.Commands> from, double unfilteredWine){
            this.from=from;
            this.unfilteredWine=unfilteredWine;
        }

    }
    public static class UnfilteredWineTransferResponse extends UnfilteredWineTransferRequest{
        public UnfilteredWineTransferResponse(ActorRef<Production.Commands> from, double unfilteredWine){
            super(from,unfilteredWine);
        }
    }
    public static class UnfilteredWineTransferAcknowledgement extends UnfilteredWineTransferRequest{
        public UnfilteredWineTransferAcknowledgement(ActorRef<Production.Commands> from, double unfilteredWine){
            super(from,unfilteredWine);
        }
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
        onReportState();

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
