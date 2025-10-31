package com.metabit.custom.safe.transparency.verification.format.sml.EDLMennekes.embedded;

import com.metabit.custom.safe.transparency.LocalizedException;

public class BillingAdapterException extends LocalizedException {

    public BillingAdapterException(String s, String localizedMessageKey) {
        super(s, localizedMessageKey);
    }

    public BillingAdapterException(String s, String localizedMessageKey, Throwable throwable) {
        super(s, localizedMessageKey, throwable);
    }
}
