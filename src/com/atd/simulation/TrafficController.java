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
import lombok.Getter;
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
 * Thread task, which represent behavior of traffic controller.
 * Accept requests for normal or emergency landing from {@link Airplane}, decide on which runway to land airplane.
 * Track when airplane finish landing, and next one could be land, if no runway are available, ordered airplane to
 * wait.
 * Decision are made using synchronisation of decision proposals between 2 running {@link TrafficController}s.
 */
@Slf4j
public class TrafficController implements Callable<Void>, CommunicatorParticipant {

    @Getter
    private int id;
    private final LandingRequestStorage landingRequestStorage;
    /**
     * Sorted by priority message queue for processing. All messages are passed asynchronously.
     */
    private final PriorityBlockingQueue<Message> messages;
    private TrafficControllerCommunicator communicator;
    private OtherTrafficControllerProposals otherControllerProposal;
    private Boolean[] runwayAvailabilityMonitors;

    public TrafficController(int id, TrafficControllerCommunicator communicator) {
        this.landingRequestStorage = new LandingRequestStorage();
        messages = new PriorityBlockingQueue<>();
        this.id = id;
        this.communicator = communicator;
        otherControllerProposal = new OtherTrafficControllerProposals();
        runwayAvailabilityMonitors = new Boolean[] {true , true};
        communicator.registerForCommunication(this);
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
                        // Store landing request.
                        AirplaneData airplaneData = ((Airplane) message.getSender()).getData();
                        processLandingRequest(message);
                        // Collect proposals available for execution.
                        Map<RunwayState.RunwayType, LandingRequest> proposalsForProcessing = getSynchronisedProposalForExecution();

                        // Check if new incoming landing request could be executed now.
                        if (proposalsForProcessing.isEmpty() ||
                                proposalsForProcessing.values().stream()
                                        .noneMatch(e -> e.getAirplaneName().equals(airplaneData.getAirplaneName()))) {
                            // If accepted request couldn't be executed now, send order for awaiting to Airplane.
                            communicator.sendResponseToAirplane(
                                    id, airplaneData.getAirplaneName(), Message.MessageType.WAITING_AROUND, PLEASE_CIRCLE_AROUND_THE_AIRPORT);
                        }
                        // Execute found landing proposals.
                        executeProposals(proposalsForProcessing);
                        break;
                    case LANDING_APPROVED:
                        // Update local runway state.
                        RunwayState.RunwayType runwayType = MessageUtils.getRunwayFromMessage(message);
                        runwayAvailabilityMonitors[runwayType.getIndex()] = true;
                        break;
                }
            }
            // If there are no more messages, try to check if some proposals are ready for execution.
            if (messages.isEmpty()) {
                Thread.sleep(200);
                Map<RunwayState.RunwayType, LandingRequest> proposalsForProcessing = getSynchronisedProposalForExecution();
                // Execute found landing proposals.
                executeProposals(proposalsForProcessing);
            }
        }
    }

    /**
     * Execute passed-in {@link LandingRequest} per {@link RunwayState.RunwayType} via sending orders for landing to
     * related airplanes. In parallel update local runway state monitors ({@code runwayAvailabilityMonitors}).
     */
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

    /**
     * Returns map of possible landing proposals for execution, which are already synchronised with another
     * traffic controller.
     */
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

    /**
     * Returns map of possible landing proposals per runway for execution defined using local known information.
     * Per runway, which are already occupied by landing airplanes specific state
     * {@link LandingRequest#ALREADY_IN_PROGRESS} is used.
     */
    private Map<RunwayState.RunwayType, LandingRequest> getProposalForProcessing() {
        Map<RunwayState.RunwayType, LandingRequest> availableRunwayToRequest = new HashMap<>();
        Boolean[] runwayStates = Arrays.copyOf(runwayAvailabilityMonitors, 2);

        defineAvailableRunwayForRequests(availableRunwayToRequest, runwayStates, landingRequestStorage.getEmergencyRequests());
        defineAvailableRunwayForRequests(availableRunwayToRequest, runwayStates, landingRequestStorage.getNormalRequests());
        updateWithMonitorStateInfo(availableRunwayToRequest);
        return availableRunwayToRequest;
    }

    /**
     * Updates passed-in {@code availableRunwayToRequest} with specific state {@link LandingRequest#ALREADY_IN_PROGRESS}
     * based on information in {@code runwayAvailabilityMonitors}.
     */
    private void updateWithMonitorStateInfo(Map<RunwayState.RunwayType, LandingRequest> availableRunwayToRequest) {
        if (!runwayAvailabilityMonitors[RunwayState.RunwayType.SHORT.getIndex()]) {
            availableRunwayToRequest.put(RunwayState.RunwayType.SHORT, LandingRequest.ALREADY_IN_PROGRESS);
        }
        if (!runwayAvailabilityMonitors[RunwayState.RunwayType.LONG.getIndex()]) {
            availableRunwayToRequest.put(RunwayState.RunwayType.LONG, LandingRequest.ALREADY_IN_PROGRESS);
        }
    }

    /**
     * Updates passed-in {@code availableRunwayToRequest} with found available for execution landing request from
     * {@code requests}, based on information about {@code runwayStates}.
     */
    private void defineAvailableRunwayForRequests(
            Map<RunwayState.RunwayType, LandingRequest> availableRunwayToRequest,
            Boolean[] runwayStates, LinkedList<LandingRequest> requests) {
        if (RequestUtils.anyRunwayAvailable(runwayStates)) {
            for (LandingRequest request : requests) {
                RunwayState.RunwayType matchRunway =
                        RequestUtils.defineMatchRunway(runwayStates, request.getAirplaneType());
                if (matchRunway != null) {
                    availableRunwayToRequest.put(matchRunway, request);
                    runwayStates[matchRunway.getIndex()] = false;
                    if (!RequestUtils.anyRunwayAvailable(runwayStates)) {
                        return;
                    }
                }
            }
        }
    }

    /**
     * Process {@link Message} of request for landing from airplane, store required information for further execution.
     */
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

    /**
     * Asynchronous message receiving to queue for further processing.
     * Synchronisation between traffic controllers type messages also update proposals data container.
     */
    public void send(CommunicationMessage communicationMessage) throws InterruptedException {
        Message message = communicationMessage.getMessage();
        if (message.getType() == Message.MessageType.SYNCHRONISATION_BETWEEN_CONTROLLER) {
            Map<RunwayState.RunwayType, LandingRequest> requestMap =
                    (Map<RunwayState.RunwayType, LandingRequest>) communicationMessage.getData();
            while (!otherControllerProposal.updateProposals(requestMap)) {
                Thread.sleep(50);
            }
        } else {
            messages.put(message);
        }
    }

    @Override
    public String getParticipantName() {
        return String.format("Traffic controller %d", id);
    }
}
