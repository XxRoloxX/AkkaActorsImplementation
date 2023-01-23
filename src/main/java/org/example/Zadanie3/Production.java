package org.example.Zadanie3;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.actor.*;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.ArrayList;

public class Production extends AbstractBehavior<Production.Commands>  {


    private final int NUMBER_OF_WAREHOUSES=1;
    private final int NUMBER_OF_WINEPRESSES=1;

    private final int NUMBER_OF_FERMENTATION_STATIONS=3;

    private final int NUMBER_OF_FILTRATION_STATIONS=4;

    private final int NUMBER_OF_BOTTLING_STATIONS=3;



    private ArrayList<ActorRef<Commands>> warehouses;
    private ArrayList<ActorRef<Production.Commands>>winePresses;
    private ArrayList<ActorRef<Production.Commands>>fermentationStations;
    private ArrayList<ActorRef<Production.Commands>>filtrationStations;

    private ArrayList<ActorRef<Production.Commands>>bottlingStations;


    public interface Commands {};

    private Production(ActorContext<Commands> context) {
        super(context);
        warehouses = new ArrayList<>();
        winePresses = new ArrayList<>();
        fermentationStations = new ArrayList<>();
        filtrationStations = new ArrayList<>();
        bottlingStations = new ArrayList<>();
    }

    public static Behavior<Commands> create(){
        return Behaviors.setup(Production::new);
    }

    public enum triggerProduction implements Commands{
        INSTANCE
    } ;


    public static class InitializeProduction implements Commands {

        public final ArrayList<ActorRef<Production.Commands>>warehouses;
        public final ArrayList<ActorRef<Production.Commands>>winePresses;
        public final ArrayList<ActorRef<Production.Commands>>fermentationStations;

        public final ArrayList<ActorRef<Production.Commands>>filtrationStations;

        public final ArrayList<ActorRef<Production.Commands>>bottlingStations;


        public InitializeProduction(ArrayList<ActorRef<Production.Commands>>warehouses
        ,ArrayList<ActorRef<Production.Commands>>winePresses, ArrayList<ActorRef<Production.Commands>>fermentationStations, ArrayList<ActorRef<Production.Commands>>filtrationStations,
                                        ArrayList<ActorRef<Production.Commands>>bottlingStations){

            this.warehouses= warehouses;
            this.winePresses=winePresses;
            this.fermentationStations=fermentationStations;
            this.filtrationStations=filtrationStations;
            this.bottlingStations=bottlingStations;
        }
    }
    public enum ReportState implements Production.Commands {
        INSTANCE;
    }


    public static class StartProduction implements Commands{

        public final double grapes;
        public final double water;
        public final double sugar;
        public final int bottles;
        public final double timeModifier;


        public StartProduction(double grapes, double water, double sugar, int bottles, double timeModifier){
            this.grapes=grapes;
            this.water =water;
            this.sugar=sugar;
            this.bottles=bottles;
            this.timeModifier=timeModifier;

        }
    }

    private Behavior<Commands> onStartProduction(StartProduction productionParameters){

        for(int i=0;i<NUMBER_OF_WAREHOUSES;i++){
            warehouses.add(getContext().spawn(Warehouse.create(productionParameters.grapes,
                    productionParameters.water,productionParameters.sugar,productionParameters.bottles),"Warehouse"+i));
            warehouses.get(i).tell(new InitializeProduction(warehouses,winePresses,fermentationStations,filtrationStations,bottlingStations));
            warehouses.get(i).tell(triggerProduction.INSTANCE);
        }

        for(int i=0;i<NUMBER_OF_WINEPRESSES;i++){
            winePresses.add(getContext().spawn(WinePress.create(productionParameters.timeModifier), "WinePress"+i));
            winePresses.get(i).tell(new InitializeProduction(warehouses,winePresses,fermentationStations,filtrationStations,bottlingStations));
            winePresses.get(i).tell(triggerProduction.INSTANCE);

        }
        for(int i=0;i<NUMBER_OF_FERMENTATION_STATIONS;i++){
            fermentationStations.add(getContext().spawn(Fermentation.create(productionParameters.timeModifier), "Fermentation"+i));
            fermentationStations.get(i).tell(new InitializeProduction(warehouses,winePresses,fermentationStations,filtrationStations,bottlingStations));
            fermentationStations.get(i).tell(triggerProduction.INSTANCE);

        }

        for(int i=0;i<NUMBER_OF_FILTRATION_STATIONS;i++){
            filtrationStations.add(getContext().spawn(Filtration.create(productionParameters.timeModifier), "Filtration"+i));
            filtrationStations.get(i).tell(new InitializeProduction(warehouses,winePresses,fermentationStations,filtrationStations,bottlingStations));
            filtrationStations.get(i).tell(triggerProduction.INSTANCE);

        }
        for(int i=0;i<NUMBER_OF_BOTTLING_STATIONS;i++){
            bottlingStations.add(getContext().spawn(Bottling.create(productionParameters.timeModifier), "Bottling"+i));
            bottlingStations.get(i).tell(new InitializeProduction(warehouses,winePresses,fermentationStations,filtrationStations,bottlingStations));
            bottlingStations.get(i).tell(triggerProduction.INSTANCE);

        }
        return this;
    }


    @Override
    public Receive<Commands> createReceive() {
        return newReceiveBuilder().onMessage(StartProduction.class,this::onStartProduction).build();
    }



}
