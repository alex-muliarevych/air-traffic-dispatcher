package com.atd.simulation;

import com.atd.communication.CommunicatorParticipant;
import com.atd.communication.TrafficControllerCommunicator;
import com.atd.communication.data.CommunicationMessage;
import com.atd.communication.data.Message;
import com.atd.config.AirplaneData;
import com.atd.simulation.data.LandingRequest;
import com.atd.simulation.data.OtherTrafficControllerProposals;
import com.atd.utils.MessageUtils;
import com.atd.utils.RequestUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;

import static com.atd.communication.data.Message.MessageType.EMERGENCY_CALL_TO_LAND;
import static com.atd.communication.data.Message.PLEASE_CIRCLE_AROUND_THE_AIRPORT;
import static com.atd.communication.data.Message.PLEASE_LAND_ON_A_RUNWAY_X;

/**
 */
@Slf4j
public class TrafficController implements Callable<Void>, CommunicatorParticipant {

    private Integer id;
    private final LandingRequestStorage landingRequestStorage;
    private final PriorityBlockingQueue<Message> messages;
    private TrafficControllerCommunicator communicator;
    private OtherTrafficControllerProposals otherControllerProposal;
    private Boolean[] runwayAvailabilityMonitors;

    public TrafficController(Integer id, TrafficControllerCommunicator communicator) {
        this.landingRequestStorage = new LandingRequestStorage();
        messages = new PriorityBlockingQueue<>();
        this.id = id;
        this.communicator = communicator;
        otherControllerProposal = new OtherTrafficControllerProposals();
        runwayAvailabilityMonitors = new Boolean[] {true , true};
        communicator.registerForCommunication(id, this);
    }

    @Override
    public Void call() throws Exception {
        log.debug("Traffic controller with id '{}' started", id);
        try {
            main();
        } catch (Exception ex) {
            log.error("Execution failed", ex.getCause());
        }
        return null;
    }

    private void main() throws InterruptedException {
        while (true) {
            while (!messages.isEmpty()) {
                Message message = messages.poll();
                if (message.getType() == Message.MessageType.TERMINATED) {
                    return;
                }
                switch (message.getType()) {
                    case READY_TO_LAND:
                    case EMERGENCY_CALL_TO_LAND:
                        AirplaneData airplaneData = ((Airplane) message.getSender()).getData();
                        processLandingRequest(message);
                        Map<RunwayState.RunwayType, LandingRequest> proposalsForProcessing = getSynchronisedProposalForExecution();

                        if (proposalsForProcessing.isEmpty() ||
                                proposalsForProcessing.values().stream()
                                        .noneMatch(e -> e.getAirplaneName().equals(airplaneData.getAirplaneName()))) {
                            communicator.sendResponseToAirplane(
                                    id, airplaneData.getAirplaneName(), Message.MessageType.WAITING_AROUND, PLEASE_CIRCLE_AROUND_THE_AIRPORT);
                        }
                        executeProposals(proposalsForProcessing);
                        break;
                    case LANDING_APPROVED:
                        RunwayState.RunwayType runwayType = MessageUtils.getRunwayFromMessage(message);
                        runwayAvailabilityMonitors[runwayType.getIndex()] = true;
                        break;
                }
            }
            if (messages.isEmpty()) {
                Thread.sleep(200);
                Map<RunwayState.RunwayType, LandingRequest> proposalsForProcessing = getSynchronisedProposalForExecution();
                executeProposals(proposalsForProcessing);
            }
        }
    }

    private void executeProposals(Map<RunwayState.RunwayType, LandingRequest> proposalsForProcessing) throws InterruptedException {
        for (Map.Entry<RunwayState.RunwayType, LandingRequest> entry : proposalsForProcessing.entrySet()) {
            LandingRequest landingRequest = entry.getValue();
            // If request already in progress, skip it.
            if (landingRequest.getAirplaneName().isEmpty()) {
                continue;
            }
            communicator.sendResponseToAirplane(id, landingRequest.getAirplaneName(),
                    Message.MessageType.LAND_ON_A_RUNWAY,
                    String.format(PLEASE_LAND_ON_A_RUNWAY_X, entry.getKey().getIndex()));
            runwayAvailabilityMonitors[entry.getKey().getIndex()] = false;
            landingRequestStorage.removeLandingRequestFromQueue(landingRequest);
        }
    }

    private Map<RunwayState.RunwayType, LandingRequest> getSynchronisedProposalForExecution() throws InterruptedException {
        Map<RunwayState.RunwayType, LandingRequest> proposalForProcessing = getProposalForProcessing();
        communicator.synchroniseDecisions(id, proposalForProcessing);
        // Wait for response of other traffic controller.
        while (otherControllerProposal.isEmpty()) {
            Thread.sleep(50);
        }
        proposalForProcessing = RequestUtils.getSynchronisedProposals(proposalForProcessing,
                otherControllerProposal.getOtherControllerProposal());
        while (!otherControllerProposal.reset()) {
            Thread.sleep(50);
        }
        return proposalForProcessing;
    }

    private Map<RunwayState.RunwayType, LandingRequest> getProposalForProcessing() {
        Map<RunwayState.RunwayType, LandingRequest> availableRunwayToRequest = new HashMap<>();
        Boolean[] runwayStates = Arrays.copyOf(runwayAvailabilityMonitors, 2);

        defineAvailableRunwayForRequests(availableRunwayToRequest, runwayStates, landingRequestStorage.getEmergencyRequests());
        defineAvailableRunwayForRequests(availableRunwayToRequest, runwayStates, landingRequestStorage.getNormalRequests());
        updateWithMonitorStateInfo(availableRunwayToRequest);
        return availableRunwayToRequest;
    }

    private void updateWithMonitorStateInfo(Map<RunwayState.RunwayType, LandingRequest> availableRunwayToRequest) {
        if (!runwayAvailabilityMonitors[RunwayState.RunwayType.SHORT.getIndex()]) {
            availableRunwayToRequest.put(RunwayState.RunwayType.SHORT, LandingRequest.ALREADY_IN_PROGRESS);
        }
        if (!runwayAvailabilityMonitors[RunwayState.RunwayType.LONG.getIndex()]) {
            availableRunwayToRequest.put(RunwayState.RunwayType.LONG, LandingRequest.ALREADY_IN_PROGRESS);
        }
    }

    private void defineAvailableRunwayForRequests(
            Map<RunwayState.RunwayType, LandingRequest> availableRunwayToRequest,
            Boolean[] runwayStates, LinkedList<LandingRequest> emergencyRequests) {
        if (RequestUtils.anyRunwayAvailable(runwayStates)) {
            for (LandingRequest request : emergencyRequests) {
                RunwayState.RunwayType matchRunway =
                        RequestUtils.defineMatchRunway(runwayStates, request.getAirplaneType());
                if (matchRunway != null) {
                    availableRunwayToRequest.put(matchRunway, request);
                    runwayStates[matchRunway.getIndex()] = false;
                    if (RequestUtils.anyRunwayAvailable(runwayStates)) {
                        break;
                    }
                }
            }
        }
    }

    private void processLandingRequest(Message message) throws InterruptedException {
        AirplaneData airplaneData = ((Airplane) message.getSender()).getData();
        LinkedList<LandingRequest> requestQueue =
                message.getType() == EMERGENCY_CALL_TO_LAND ?
                        landingRequestStorage.getEmergencyRequests() : landingRequestStorage.getNormalRequests();
        LandingRequest request = LandingRequest.builder()
                .airplaneName(airplaneData.getAirplaneName())
                .airplaneType(airplaneData.getAirplaneType())
                .landingType(airplaneData.getLandingType())
                .date(new Date())
                .build();
        requestQueue.add(request);
    }

    public void send(CommunicationMessage communicationMessage) throws InterruptedException {
        Message message = communicationMessage.getMessage();
        if (message.getType() == Message.MessageType.SYNCHRONISATION_BETWEEN_CONTROLLER) {
            Map<RunwayState.RunwayType, LandingRequest> requestMap =
                    (Map<RunwayState.RunwayType, LandingRequest>) communicationMessage.getData();
            while (!otherControllerProposal.updateProposals(requestMap)) {
                Thread.sleep(50);
            }
        }
        messages.put(message);
    }

    @Override
    public String getParticipantName() {
        return String.format("Traffic controller %d", id);
    }
}
