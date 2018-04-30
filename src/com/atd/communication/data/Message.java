package com.atd.communication.data;

import com.atd.communication.CommunicatorParticipant;
import lombok.Builder;
import lombok.Getter;

import java.util.regex.Pattern;

/**
 * Common message structure data used for communication between traffic controllers and airplanes.
 */
@Getter
@Builder
public class Message implements CommunicationMessage, Comparable<Message> {

    /*
     * Constant messages used for sending via Communicator.
     */
    public static final String PLEASE_CIRCLE_AROUND_THE_AIRPORT = "Please circle around the airport.";
    public static final String READY_TO_LAND = "Ready to land.";
    public static final String MAYDAY = "Mayday.";
    public static final String LANDED_ON_RUNWAY_X = "Landed on runway %d.";
    public static final String PLEASE_LAND_ON_A_RUNWAY_X = "Please land on a runway %d";
    public static final String EMERGENCY_LANDING_NOTIFICATION_OF_SIZE_X = "I have an emergency landing for airplane size %s";
    public static final String NORMAL_LANDING_NOTIFICATION_OF_SIZE_X = "I have an regular landing for airplane size %s";
    public static final String GOING_TO_OCCUPY_RUNWAY_X = "I'm going to start landing for airplane on a runway %d";

    /**
     * Pattern used to detect index of runway from text of the message. Example:
     * <pre>
     *     Please land on a runway 1
     * </pre>
     */
    public static final Pattern RUNWAY_X = Pattern.compile(".* (?<index>\\d+)");

    private MessageType type;
    private String text;
    private CommunicatorParticipant receiver;
    private CommunicatorParticipant sender;

    @Override
    public Message getMessage() {
        return this;
    }

    @Override
    public Object getData() {
        return null;
    }

    @Override
    public int compareTo(Message o) {
        return o.getType().getPriority() - this.type.getPriority();
    }

    public enum MessageType {
        // Types used for messages passed to traffic controller:
        LANDING_APPROVED(4),
        EMERGENCY_CALL_TO_LAND(3),
        READY_TO_LAND(2),
        TERMINATED(1),
        SYNCHRONISATION_BETWEEN_CONTROLLER(0),
        // Types used for messages passed to airplane:
        WAITING_AROUND(0),
        LAND_ON_A_RUNWAY(0),;

        /**
         * Priority of message type, used for ordering messages sent to traffic controller.
         */
        @Getter
        private int priority;

        MessageType(int priority) {
            this.priority = priority;
        }
    }

    @Override
    public String toString() {
        return "[" + sender.getParticipantName() + " -> " + receiver.getParticipantName() + ": " + text + "]";
    }
}
