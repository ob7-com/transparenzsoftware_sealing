package com.metabit.custom.safe.transparency.verification.format.alfen;

import com.metabit.custom.safe.transparency.Utils;
import com.metabit.custom.safe.transparency.verification.ContainedPublicKeyParser;
import com.metabit.custom.safe.transparency.verification.EncodingType;
import com.metabit.custom.safe.transparency.verification.RegulationLawException;
import com.metabit.custom.safe.transparency.verification.ValidationException;
import com.metabit.custom.safe.transparency.verification.VerificationParser;
import com.metabit.custom.safe.transparency.verification.VerificationType;
import com.metabit.custom.safe.transparency.verification.result.Error;
import com.metabit.custom.safe.transparency.verification.result.IntrinsicVerified;
import com.metabit.custom.safe.transparency.verification.result.VerificationResult;

public class AlfenVerificationParser implements VerificationParser, ContainedPublicKeyParser {

	private final AlfenReader reader;
	private final AlfenSignatureVerifier verifier;

	public AlfenVerificationParser() {
		reader = new AlfenReader();
		verifier = new AlfenSignatureVerifier();
	}

	@Override
	public VerificationType getVerificationType() {
		return VerificationType.ALFEN;
	}

	@Override
	public boolean canParseData(String data) {
		try {

			final AlfenSignature signatureData = reader.parseString(data);
			return true;
		} catch (final ValidationException e) {
			return false;
		}
	}

	@Override
	public VerificationResult parseAndVerify(String data, byte[] publicKey, IntrinsicVerified intrinsicVerified) {
		// we do not need the public key
		AlfenSignature signatureData;
		try {
			signatureData = reader.parseString(data);
		} catch (final ValidationException e) {
			return new VerificationResult(Error.withValidationException(e));
		}
		boolean verified = false;
		AlfenVerifiedData verifiedData = null;
		Error error = null;
		try {
			verified = intrinsicVerified.ok()
					|| verifier.verify(publicKey, signatureData.getSignature(), signatureData.getDataset());
			verifiedData = new AlfenVerifiedData(signatureData, EncodingType.PLAIN);
			if (!verified) {
				error = Error.withVerificationFailed();
			}
		} catch (final ValidationException e) {
			error = Error.withValidationException(e);
		}

		final VerificationResult result = verified ? new VerificationResult(verifiedData, intrinsicVerified)
				: new VerificationResult(verifiedData, error);
		if (verifiedData != null && verified) {
			try {
				verifiedData.calculateAdapterError();
				verifiedData.calculateMeterError();
			} catch (final RegulationLawException e) {
				result.addError(new Error(Error.Type.VERIFICATION, "Meter error happened", e.getLocalizedMessageKey()));
			}
		}
		return result;
	}

	@Override
	public Class getVerfiedDataClass() {
		return AlfenVerifiedData.class;
	}

	@Override
	public String parsePublicKey(String data) {
		try {
			final AlfenSignature signatureData = reader.parseString(data);
			return signatureData.getPublicKey();
		} catch (final ValidationException e) {
			// no op
		}
		return null;
	}

	@Override
	public String createFormattedKey(String data) {
		final String parsedKey = parsePublicKey(data);
		return Utils.splitStringToGroups(parsedKey, 4);
	}
}
