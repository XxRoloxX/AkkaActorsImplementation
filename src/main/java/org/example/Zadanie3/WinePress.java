package org.example.Zadanie3;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;

import java.util.*;

import org.example.Utils;

public class WinePress extends AbstractBehavior<Production.Commands> {

    private static final double REQUIRED_GRAPES=15;
    private static final double GRAPE_JUICE_OUTPUT=10;

    private static final double FAILURE_PROPABILITY = 0;

    private static final double TIME_TO_PRODUCE = 12;
    private WinePress(ActorContext<Production.Commands> context, double timeModifier) {
        super(context);
        amountOfGrapeJuice=0;
        amountOfGrapes=0;
        occupied=false;
        this.timeModifier=timeModifier;
        reservedResources = new HashMap<>();

    }

    private double amountOfGrapes;

    private double amountOfGrapeJuice;

    private boolean occupied;

    private double timeModifier;

    private HashMap<ActorRef<Production.Commands>,GrapeJuiceTransferResponse> reservedResources;

    private ArrayList<ActorRef<Production.Commands>> warehouses;
    private ArrayList<ActorRef<Production.Commands>>fermentationStations;
    public boolean isOccupied() {
        return occupied;
    }


    public static Behavior<Production.Commands> create(double timeModifierArg){
        return Behaviors.setup(context-> new WinePress(context,timeModifierArg));
    }

    @Override
    public Receive<Production.Commands> createReceive() {
        return newReceiveBuilder()
                .onMessageEquals(Production.ReportState.INSTANCE,this::onReportState)
                .onMessageEquals(Production.triggerProduction.INSTANCE,this::onTriggerProduction)
                .onMessage(GrapeJuiceTransferAcknowledgement.class,this::onGrapeJuiceTransferAcknowledgement)
                .onMessage(Production.InitializeProduction.class,this::onInitializeProduction)
                .onMessage(GrapeJuiceTransferRequest.class,this::onGrapeJuiceTransportRequest)
                .onMessage(Warehouse.ResourcesTransferResponse.class,this::onGrapeTransportResponse)
                .build();
    }
    public boolean produce(){
        Random rand = new Random();
        //onReportState();
        if(amountOfGrapes>=REQUIRED_GRAPES && !occupied){
            occupied=true;


            Utils.waitFor((int)(TIME_TO_PRODUCE*1000*timeModifier));


            amountOfGrapes-=REQUIRED_GRAPES;

            if(rand.nextDouble()>FAILURE_PROPABILITY){
                amountOfGrapeJuice+=GRAPE_JUICE_OUTPUT;
            }

            occupied=false;

            return true;

        }else{
            return false;
        }
    }

    private Behavior<Production.Commands>onReportState(){
        System.out.println("Wine Press Resources: ");
        System.out.println("Grapes: "+amountOfGrapes);
        System.out.println("Grape Juice: "+amountOfGrapeJuice);
        return this;

    }

    private Behavior<Production.Commands> onTriggerProduction(){


            for(ActorRef<Production.Commands> warehouse: warehouses){
                warehouse.tell(Production.triggerProduction.INSTANCE);
            }


            for(ActorRef<Production.Commands> fermentation: fermentationStations){
                fermentation.tell(Production.triggerProduction.INSTANCE);
            }

            if(!produce()){
                for(ActorRef<Production.Commands> warehouse: warehouses){
                    warehouse.tell(new Warehouse.ResourcesTransferRequest(getContext().getSelf(), Math.max(REQUIRED_GRAPES-amountOfGrapes,0),0,0,0));
                }
            }

            return this;
    }

    private GrapeJuiceTransferResponse getAllReservedResources(){
        //ResourcesTransferRequest result = new ResourcesTransferRequest(getContext().getSelf(),0,0,0,0);
        GrapeJuiceTransferRequest element;
        double grapeJuice=0;


        Set<ActorRef<Production.Commands>> keys= reservedResources.keySet();
        for(ActorRef<Production.Commands> key: keys){
            element = reservedResources.get(key);
            grapeJuice+= element.grapeJuice;

        }
        return new GrapeJuiceTransferResponse(getContext().getSelf(),grapeJuice);
    }



    private Behavior<Production.Commands> onGrapeJuiceTransportRequest(GrapeJuiceTransferRequest commands){
        /*
        onReportState();
        getContext().getLog().info("Received Grape Juice Transfer Request: {}", commands);
        reservedResources.put(commands.from, new GrapeJuiceTransferResponse(commands.from, 0));

        GrapeJuiceTransferResponse loanedResources = new GrapeJuiceTransferResponse(getContext().getSelf(),
                Math.min(commands.grapeJuice, Math.max(amountOfGrapeJuice-getAllReservedResources().grapeJuice,0)));

        commands.from.tell(loanedResources);
        reservedResources.put(commands.from,loanedResources);
    */
        onReportState();
        getContext().getLog().info("Received Grape Juice Transfer Request: {}", commands);
        GrapeJuiceTransferResponse loanedResources = new GrapeJuiceTransferResponse(getContext().getSelf(),
                Math.min(commands.grapeJuice, amountOfGrapeJuice));

        if(loanedResources.grapeJuice>0){
            amountOfGrapeJuice-= loanedResources.grapeJuice;
            commands.from.tell(loanedResources);
        }



        return this;

    }

    private Behavior<Production.Commands> onGrapeTransportResponse(Warehouse.ResourcesTransferResponse commands){
        onReportState();
        getContext().getLog().info("Received Grape Juice Transfer Response: {}", commands);

        commands.from.tell(new Warehouse.ResourceTransferAcknowledgement(getContext().getSelf(), commands.grapes, commands.water, commands.sugar, commands.bottles));
        amountOfGrapes += commands.grapes;

        return this;
    }
    private Behavior<Production.Commands> onGrapeJuiceTransferAcknowledgement(GrapeJuiceTransferAcknowledgement commands){
        /*
        getContext().getLog().info("Received Grape Juice Transfer Acknowledgment: {}", commands);
        amountOfGrapeJuice -= reservedResources.get(commands.from).grapeJuice;
        reservedResources.put(commands.from, new GrapeJuiceTransferResponse(commands.from, 0));

         */
        getContext().getLog().info("Received Grape Juice Transfer Acknowledgment: {}", commands);


        return this;

    }

    private Behavior<Production.Commands> onInitializeProduction(Production.InitializeProduction commands){
        warehouses= commands.warehouses;
        fermentationStations= commands.fermentationStations;
        return this;
    }



    public static class GrapeJuiceTransferRequest implements Production.Commands {
        public final double grapeJuice;
        public final ActorRef<Production.Commands> from;


        public GrapeJuiceTransferRequest(ActorRef<Production.Commands> from, double grapeJuice){
            this.grapeJuice=grapeJuice;
            this.from = from;

        }

        @Override
        public String toString() {
            return "GrapeJuiceTransferRequest{" +
                    "grapeJuice=" + grapeJuice +
                    ", from=" + from +
                    '}';
        }
    }
    public static class GrapeJuiceTransferResponse extends  GrapeJuiceTransferRequest{
        public GrapeJuiceTransferResponse(ActorRef<Production.Commands> from, double grapeJuice){
            super(from, grapeJuice);

        }

    }
    public static class GrapeJuiceTransferAcknowledgement extends  GrapeJuiceTransferRequest{
        public GrapeJuiceTransferAcknowledgement(ActorRef<Production.Commands> from, double grapeJuice){
            super(from, grapeJuice);

        }

    }



}
