package com.metabit.custom.safe.transparency.verification.result;

public enum IntrinsicVerified {

	VERIFIED, NOT_VERIFIED,;

	public boolean ok() {
		return this == VERIFIED;
	}

}
