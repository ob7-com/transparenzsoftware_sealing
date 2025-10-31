package com.metabit.custom.safe.transparency.verification;

import com.metabit.custom.safe.transparency.LocalizedException;

public class RegulationLawException extends LocalizedException {
    public RegulationLawException(String s, String localizedMessageKey) {
        super(s, localizedMessageKey);
    }

    public RegulationLawException(String s, String localizedMessageKey, Throwable throwable) {
        super(s, localizedMessageKey, throwable);
    }
}
