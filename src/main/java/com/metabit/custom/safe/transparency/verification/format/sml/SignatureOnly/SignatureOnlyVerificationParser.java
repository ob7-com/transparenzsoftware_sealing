package com.metabit.custom.safe.transparency.verification.format.sml.SignatureOnly;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.metabit.custom.safe.transparency.Utils;
import com.metabit.custom.safe.transparency.verification.ContainedPublicKeyParser;
import com.metabit.custom.safe.transparency.verification.DecodingException;
import com.metabit.custom.safe.transparency.verification.EncodingType;
import com.metabit.custom.safe.transparency.verification.ValidationException;
import com.metabit.custom.safe.transparency.verification.VerificationType;
import com.metabit.custom.safe.transparency.verification.format.sml.SMLSignature;
import com.metabit.custom.safe.transparency.verification.format.sml.SMLValidationException;
import com.metabit.custom.safe.transparency.verification.format.sml.SMLVerificationParserBase;
import com.metabit.custom.safe.transparency.verification.format.sml.SignatureOnly.embedded.SignedMeterValue;
import com.metabit.custom.safe.transparency.verification.result.Error;
import com.metabit.custom.safe.transparency.verification.result.IntrinsicVerified;
import com.metabit.custom.safe.transparency.verification.result.VerificationResult;

public class SignatureOnlyVerificationParser extends SMLVerificationParserBase implements ContainedPublicKeyParser {

	private static final Logger LOGGER = LogManager.getLogger(SignatureOnlyVerificationParser.class);
	private final SMLReader smlReader;

	public SignatureOnlyVerificationParser() {
		super();
		smlReader = new SMLReader();
	}

	@Override
	public VerificationType getVerificationType() {
		return VerificationType.EDL_40_SIG;
	}

	@Override
	public VerificationResult parseAndVerify(String data, byte[] publicKey, IntrinsicVerified intrinsicVerified) {

		try {
			final SignedMeterValue signedMeterValue = smlReader.readFromString(Utils.unescapeXML(data));
			signedMeterValue.validate();
			final SMLSignature smlSignature = smlReader.parseSMLSigXml(signedMeterValue);
			if (smlSignature.getUnit() != 30) {
				throw new SMLValidationException("Invalid unit present in sml data", "error.sml.invalid.unit");
			}
			final SMLVerifiedData verifiedData = new SMLVerifiedData(smlSignature, VerificationType.EDL_40_SIG,
					EncodingType.PLAIN, Utils.toFormattedHex(publicKey));

			if (intrinsicVerified.ok() || verifier.verify(publicKey, smlSignature)) {
				return new VerificationResult(verifiedData, intrinsicVerified);
			} else {
				return new VerificationResult(verifiedData, Error.withVerificationFailed());
			}
		} catch (final ValidationException e) {
			return new VerificationResult(Error.withValidationException(e));
		}

	}

	@Override
	public boolean canParseData(String data) {

		try {
			final SignedMeterValue signedMeterValue = smlReader.readFromString(Utils.unescapeXML(data));
			signedMeterValue.validate();
		} catch (final ValidationException e) {
			LOGGER.info("Data not matching for " + VerificationType.EDL_40_SIG);
			return false;
		}
		LOGGER.info("Match for " + VerificationType.EDL_40_SIG + " detected");
		return true;
	}

	@Override
	public String parsePublicKey(String data) {
		try {
			final SignedMeterValue signedMeterValue = smlReader.readFromString(Utils.unescapeXML(data));
			return signedMeterValue.getPublicKey() != null ? signedMeterValue.getPublicKey().getCleanedValue() : null;
		} catch (final ValidationException e) {
			return null;
		}
	}

	@Override
	public String createFormattedKey(String data) {
		final String parsedKey = parsePublicKey(data);
		String formattedKey = parsedKey;
		final List<EncodingType> encodingTypes = EncodingType.guessType(parsedKey, false);
		if (encodingTypes.size() > 0) {
			try {
				formattedKey = Utils.toFormattedHex(encodingTypes.get(0).decode(parsedKey), 4);
			} catch (final DecodingException e) {
				LOGGER.warn("Could not decode parsed key", e);
			}
		}
		return formattedKey;
	}
}
