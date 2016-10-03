package com.lcjian.wechatsimulation.exception;

public class JobException extends RuntimeException {

    public static final String MESSAGE_SYSTEM_ERROR = "system error";
    public static final String MESSAGE_HAVE_BEEN_FRIEND_ERROR = "This user is your friend already";
    public static final String MESSAGE_NO_THIS_USER_ERROR = "There is no such a user";

    public JobException(String detailMessage) {
        super(detailMessage);
    }
}
