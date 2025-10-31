package com.metabit.custom.safe.transparency.verification.result;

import com.metabit.custom.safe.transparency.LocalizedException;
import com.metabit.custom.safe.transparency.verification.RegulationLawException;

public class Warning {

    private String message;
    private String localizedKey;

    public Warning(String message, String localizedKey) {
        this.message = message;
        this.localizedKey = localizedKey;
    }

    public Warning(RegulationLawException e) {
        this(e.getMessage(), e.getLocalizedMessageKey());
    }

    public String getMessage() {
        return message;
    }

    public String getLocalizedKey() {
        return localizedKey;
    }
}
