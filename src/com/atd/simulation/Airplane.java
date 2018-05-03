package com.atd.simulation;

import com.atd.communication.AirplaneCommunicator;
import com.atd.communication.CommunicatorParticipant;
import com.atd.communication.data.Message;
import com.atd.config.AirplaneData;
import com.atd.utils.MessageUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Thread task, which represent behavior of airplane.
 * Could request for normal or emergency landing to {@link TrafficController}, execute landing on a runway or waiting
 * for it depends on response message from traffic controller.
 */
@Slf4j
public class Airplane implements Callable<Void>, CommunicatorParticipant {

    @Getter
    private AirplaneData data;
    private AirplaneCommunicator communicator;
    private final LinkedBlockingQueue<Message> messages;
    private RunwayState runawayState;
    @Getter
    private boolean landed;
    @Getter
    private Long executionTime;
    @Getter
    private Integer chosenRunwayIndex;

    public Airplane(AirplaneData data, AirplaneCommunicator communicator, RunwayState runawayState) {
        messages = new LinkedBlockingQueue<>();
        landed = false;
        this.data = data;
        this.communicator = communicator;
        this.runawayState = runawayState;
        communicator.registerForCommunication(data.getAirplaneName(), this);
    }

    @Override
    public Void call() throws Exception {
        log.debug("Thread for airplane '{}' started", data.getAirplaneName());
        // Sleep until defined arrival time of airplane.
        Thread.sleep(data.getNoOfSeconds() * 1000);
        log.debug("{} is going to land.", data.getAirplaneName());
        try {
            executionTime = TimeUnit.NANOSECONDS.toSeconds(main());
        } catch (Exception ex) {
            log.error("Execution failed", ex.getCause());
        }
        return null;
    }

    private long main() throws InterruptedException {
        long startTime = System.nanoTime();
        Integer controllerId = communicator.requestForLanding(data.getAirplaneName());
        while (true) {
            if (messages.isEmpty()) {
                Thread.sleep(100);
            }
            while (!messages.isEmpty()) {
                Message message = messages.poll();
                if (message.getType() == Message.MessageType.TERMINATED) {
                    return System.nanoTime() - startTime;
                }
                switch (message.getType()) {
                    case WAITING_AROUND:
                        // Continue to sleep and wait for next messages.
                        break;
                    case LAND_ON_A_RUNWAY:
                        RunwayState.RunwayType runwayType = MessageUtils.getRunwayFromMessage(message);
                        chosenRunwayIndex = runwayType.getIndex();
                        runawayState.landOnRunaway(data.getAirplaneName(), runwayType);
                        // Execute landing.
                        Thread.sleep(data.getAirplaneType().getLandingTime() * 1000);
                        runawayState.finishLanding(runwayType);

                        communicator.confirmOfSuccessLanding(controllerId, data.getAirplaneName(), runwayType);
                        landed = true;
                        return System.nanoTime() - startTime;
                }
            }
        }
    }

    /**
     * Asynchronous message receiving to queue for further processing.
     */
    public void send(Message message) throws InterruptedException {
        messages.put(message);
    }

    @Override
    public String getParticipantName() {
        return data.getAirplaneName();
    }
}
