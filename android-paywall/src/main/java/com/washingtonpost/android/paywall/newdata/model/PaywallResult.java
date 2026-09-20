package com.washingtonpost.android.paywall.newdata.model;

/**
 * Paywall result
 * 
 * @author Bkilari
 * 
 */
public class PaywallResult {

    public enum State {
        SUCCESS,
        FAIL,
        ERROR
    }

    State state;
	String message;

    public boolean isSuccess() {
        return state == State.SUCCESS;
    }

    public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
