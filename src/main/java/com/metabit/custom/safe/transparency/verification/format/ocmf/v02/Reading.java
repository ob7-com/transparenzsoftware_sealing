package com.metabit.custom.safe.transparency.verification.format.ocmf.v02;

public class Reading extends com.metabit.custom.safe.transparency.verification.format.ocmf.Reading {

    /**
     * Event Index
     */
    private Double EI;

    public Double getEI() {
        return EI;
    }

    public void setEI(Double EI) {
        this.EI = EI;
    }

	public void setRVasString(String RV) {
		this.RV = RV;	
	}
}
