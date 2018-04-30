package com.atd.utils;

import com.atd.communication.data.Message;
import com.atd.simulation.RunwayState;

import java.util.regex.Matcher;

import static com.atd.communication.data.Message.RUNWAY_X;

/**
 * Util methods for message processing.
 */
public class MessageUtils {

    public static RunwayState.RunwayType getRunwayFromMessage(Message message) {
        Matcher m = RUNWAY_X.matcher(message.getText());
        if (!m.find()) {
            throw new RuntimeException(String.format("Incorrect message was sent: '%s'", message.toString()));
        }
        return Integer.valueOf(m.group("index")) == RunwayState.RunwayType.SHORT.getIndex() ?
                RunwayState.RunwayType.SHORT : RunwayState.RunwayType.LONG;
    }
}
