package com.mywallet.transaction.client;

public record RemoteOutcome(Kind kind, String reason) {

	public enum Kind {
		SUCCESS,
		REJECTED,
		UNKNOWN
	}

	public static RemoteOutcome success() {
		return new RemoteOutcome(Kind.SUCCESS, null);
	}

	public static RemoteOutcome rejected(String reason) {
		return new RemoteOutcome(Kind.REJECTED, safe(reason));
	}

	public static RemoteOutcome unknown() {
		return new RemoteOutcome(Kind.UNKNOWN, null);
	}

	public boolean succeeded() {
		return kind == Kind.SUCCESS;
	}

	public boolean wasRejected() {
		return kind == Kind.REJECTED;
	}

	public static String safe(String reason) {
		if (reason != null && reason.matches("[A-Z0-9_]{1,40}")) {
			return reason;
		}
		return "REJECTED";
	}

}
