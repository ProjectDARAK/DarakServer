package camp.cultr.darakserver.service

import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import org.apache.commons.codec.binary.Base32
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * Service class for handling operations related to One-Time Passwords (OTP).
 * This class utilizes the `GoogleAuthenticator` library for OTP generation and validation.
 * It also supports generation of OTP URIs for user registration and login flows.
 *
 * @property instanceName The name of the application or instance, used as the OTP issuer.
 *                        Loaded from the application properties.
 */
@Service
class OtpService(@Value("\${darak.instance-name}") private val instanceName: String) {

    /**
     * Validates a given OTP (One-Time Password) against a secret key.
     * This method encodes the given secret key to Base32 and uses the Google Authenticator to check the validity of the OTP.
     *
     * @param secret The secret key as a ByteArray, used to validate the OTP.
     * @param otp The OTP as a String, to be validated against the encoded secret.
     * @return A Boolean indicating whether the OTP is valid (true) or not (false).
     */
    fun validate(secret: ByteArray, otp: String) = GoogleAuthenticator(Base32().encode(secret)).isValid(otp)

    /**
     * Generates a One-Time Password (OTP) for the given secret key.
     * The method encodes the secret using Base32 encoding and utilizes the Google Authenticator library
     * to generate an OTP.
     *
     * @param secret The secret key as a ByteArray, used as the basis for OTP generation.
     * @return A generated OTP as an Integer.
     */
    fun generateOtp(secret: ByteArray) = GoogleAuthenticator(Base32().encode(secret)).generate()

    /**
     * Generates a URI for configuring a One-Time Password (OTP) using the Google Authenticator app.
     * The URI conforms to the OTP Auth URI format, providing details about the issuer, account label,
     * and the secret key used for generating OTPs.
     *
     * @param secret The secret key as a ByteArray, encoded in Base32 format. This key is used as the basis for OTP generation.
     * @param userName The username or account label associated with the OTP configuration.
     * @return A String representing the OTP Auth URI that can be used for adding the account to an authenticator app.
     */
    fun generateOtpUri(secret: ByteArray, userName: String) =
        GoogleAuthenticator(Base32().encode(secret)).otpAuthUriBuilder().issuer(instanceName).label(userName, instanceName).buildToString()
}
