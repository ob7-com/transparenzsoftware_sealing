package com.metabit.custom.safe.transparency.verification.format.sml.SignatureOnly;


import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.metabit.custom.safe.transparency.Utils;
import com.metabit.custom.safe.transparency.verification.EncodingType;
import com.metabit.custom.safe.transparency.verification.VerificationType;
import com.metabit.custom.safe.transparency.verification.format.sml.SMLSignature;

@XmlRootElement(name = "verifiedData")
@XmlAccessorType(XmlAccessType.FIELD)
public class SMLVerifiedData extends com.metabit.custom.safe.transparency.verification.format.sml.SMLVerifiedData {

    public SMLVerifiedData(SMLSignature smlSignature, VerificationType edl40Sig, EncodingType plain, String toFormattedHex) {
        super(smlSignature, edl40Sig, plain, toFormattedHex);
    }

    @Override
    public String getCustomerId() {
        return hexRepresentation(super.getCustomerId());
    }

    @Override
    public String getServerId() {
        return hexRepresentation(super.getServerId());
    }

    private String hexRepresentation(String format){
        if(format != null && Utils.hexToAscii(format).matches("[A-Za-z0-9!#/ ]*")){
            return String.format("%s (%s)", format, Utils.hexToAscii(format));
        } else {
            return format;
        }

    }
}
