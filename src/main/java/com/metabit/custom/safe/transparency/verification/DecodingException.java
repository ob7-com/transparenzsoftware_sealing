package com.metabit.custom.safe.transparency.verification;

import com.metabit.custom.safe.transparency.LocalizedException;

public class DecodingException extends LocalizedException {

    public DecodingException(String s, String localizedMessageKey) {
        super(s, localizedMessageKey);
    }

    public DecodingException(String s, String localizedMessageKey, Throwable e) {
        super(s, localizedMessageKey, e);
    }
}
