package com.atd.communication.data;

/**
 * Interface for different type of messages, used for communication between airplanes and traffic controllers.
 */
public interface CommunicationMessage {
    Message getMessage();
    Object getData();
}
