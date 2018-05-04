package com.atd.communication;

import com.atd.communication.data.ControllerSynchMessage;
import com.atd.communication.data.Message;
import com.atd.config.AirplaneData;
import com.atd.simulation.Airplane;
import com.atd.simulation.RunwayState;
import com.atd.simulation.TrafficController;
import com.atd.simulation.data.LandingRequest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.atd.communication.data.Message.MAYDAY;
import static com.atd.communication.data.Message.READY_TO_LAND;

/**
 * Communicator, used as message dispatcher and router.
 */
@Slf4j
public class Communicator implements AirplaneCommunicator, TrafficControllerCommunicator {

    private ConcurrentMap<String, Airplane> airplaneByNames = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, TrafficController> trafficControllersById = new ConcurrentHashMap<>();

    /**
     * {@link TrafficController}'s index counter used during round-robin selection strategy.
     */
    private int controllerCounter = 0;

    /**
     * Returns {@link Message.MessageBuilder} with populated receiver and sender based on passed-in
     * {@code controllerId} and {@code airplaneName}, direction is defined by option {@code toController}.
     */
    private Message.MessageBuilder prepareBaseMessageBuilder(int controllerId, String airplaneName,
                                                             boolean toController) {
        TrafficController trafficController = trafficControllersById.get(controllerId);
        Airplane airplane = airplaneByNames.get(airplaneName);
        return Message.builder()
                .receiver(toController ? trafficController : airplane)
                .sender(toController ? airplane : trafficController);
    }

    /**
     * Returns {@link Message.MessageBuilder} with populated receiver and sender based on passed-in
     * {@code targetControllerId} and {@code controllerId}.
     */
    private Message.MessageBuilder prepareBaseMessageBuilder(int targetControllerId, int controllerId) {
        return Message.builder()
                .receiver(trafficControllersById.get(targetControllerId))
                .sender(trafficControllersById.get(controllerId));
    }

    /**
     * Round-robin strategy of traffic controller selection for {@link LandingRequest} processing.
     */
    private int selectTrafficControllerForRequestProcessing() {
        controllerCounter = (controllerCounter + 1) % 2;
        return controllerCounter;
    }

    @Override
    public void registerForCommunication(Airplane airplane) {
        airplaneByNames.put(airplane.getData().getAirplaneName(), airplane);
    }

    @Override
    public void registerForCommunication(TrafficController controller) {
        trafficControllersById.put(controller.getId(), controller);
    }

    @Override
    public void synchroniseDecisions(int controllerId,
                                     Map<RunwayState.RunwayType, LandingRequest> preparedDecisions) throws InterruptedException {
        // Added checker if all airplanes were landed, then 'kill' traffic controller processes.
        if (allAirplanesLanded()) {
            trafficControllersById.get(0).send(Message.builder().type(Message.MessageType.TERMINATED).build());
            trafficControllersById.get(1).send(Message.builder().type(Message.MessageType.TERMINATED).build());
        }
        int targetControllerId = controllerId == 0 ? 1 : 0;
        Message message =
                prepareBaseMessageBuilder(targetControllerId, controllerId)
                        .text("Lets synchronise decisions")
                        .type(Message.MessageType.SYNCHRONISATION_BETWEEN_CONTROLLER)
                        .build();
        ControllerSynchMessage synchMessage =
                ControllerSynchMessage.builder().message(message).requestForProcessing(preparedDecisions).build();
        log.info(synchMessage.toString());
        trafficControllersById.get(targetControllerId).send(synchMessage);
    }

    @Override
    public int requestForLanding(String airplaneName) throws InterruptedException {
        int controllerId = selectTrafficControllerForRequestProcessing();
        Message.MessageBuilder messageBuilder = prepareBaseMessageBuilder(controllerId, airplaneName, true);
        Message message;
        if (airplaneByNames.get(airplaneName).getData().getLandingType() == AirplaneData.LandingType.EMERGENCY) {
            message = messageBuilder.type(Message.MessageType.EMERGENCY_CALL_TO_LAND).text(MAYDAY).build();
        } else {
            message = messageBuilder.type(Message.MessageType.READY_TO_LAND).text(READY_TO_LAND).build();
        }
        log.info(message.toString());
        trafficControllersById.get(controllerId).send(message);
        return controllerId;
    }

    @Override
    public void confirmOfSuccessLanding(int controllerId, String airplaneName,
                                        RunwayState.RunwayType runwayType) throws InterruptedException {
        Message.MessageBuilder messageBuilder = prepareBaseMessageBuilder(controllerId, airplaneName, true);
        Message message =
                messageBuilder
                        .type(Message.MessageType.LANDING_APPROVED)
                        .text(String.format(Message.LANDED_ON_RUNWAY_X, runwayType.getIndex()))
                        .build();
        log.info(message.toString());
        trafficControllersById.get(controllerId).send(message);
    }

    @Override
    public void sendResponseToAirplane(int controllerId, String airplaneName,
                                       Message.MessageType type, String text) throws InterruptedException {
        Message.MessageBuilder messageBuilder = prepareBaseMessageBuilder(controllerId, airplaneName, false);
        Message message = messageBuilder.type(type).text(text).build();
        log.info(message.toString());
        airplaneByNames.get(airplaneName).send(message);
    }

    /**
     * Returns 'true' if all {@link Airplane}s were landed and simulation could be finished, otherwise returns 'false'.
     */
    private boolean allAirplanesLanded() {
        return airplaneByNames.values().stream().allMatch(Airplane::isLanded);
    }
}
