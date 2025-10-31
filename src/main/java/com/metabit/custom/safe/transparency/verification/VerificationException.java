package com.metabit.custom.safe.transparency.verification;

import com.metabit.custom.safe.transparency.verification.result.Error;

public class VerificationException extends Exception {

    private Error error;

    public VerificationException(Error error) {
        super(error.getMessage());
        this.error = error;
    }

    public Error getError() {
        return error;
    }
}
