package camp.cultr.darakserver.service

import dev.turingcomplete.kotlinonetimepassword.GoogleAuthenticator
import org.apache.commons.codec.binary.Base32
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class OtpService(@Value("\${darak.instance-name}") private val instanceName: String) {

    fun validate(secret: ByteArray, otp: String) = GoogleAuthenticator(Base32().encode(secret)).isValid(otp)

    fun generateOtp(secret: ByteArray) = GoogleAuthenticator(Base32().encode(secret)).generate()

    fun generateOtpUri(secret: ByteArray, userName: String) =
        GoogleAuthenticator(Base32().encode(secret)).otpAuthUriBuilder().issuer(instanceName).label(userName, instanceName).buildToString()
}
