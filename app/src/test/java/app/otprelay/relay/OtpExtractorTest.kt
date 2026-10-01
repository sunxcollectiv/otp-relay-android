package app.otprelay.relay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtpExtractorTest {

    private fun assertCode(expected: String, body: String) =
        assertEquals(body, expected, OtpExtractor.extract(body))

    private fun assertIgnored(body: String) =
        assertNull(body, OtpExtractor.extract(body))

    @Test
    fun `picks up common OTP phrasings`() {
        assertCode("482913", "Your Acme verification code is 482913. It expires in 10 minutes.")
        assertCode("482913", "482913 is your Instagram code. Don't share it.")
        assertCode("4829", "Your OTP is 4829")
        assertCode("73920145", "Use 73920145 as your one-time passcode")
        assertCode("5521", "Your login PIN: 5521")
        assertCode("991203", "Your 2FA code: 991203")
        assertCode("118822", "Authentication code 118822 for your bank")
    }

    @Test
    fun `drops the Google prefix`() {
        assertCode("123456", "G-123456 is your Google verification code.")
    }

    @Test
    fun `joins split codes`() {
        assertCode("123456", "Your WhatsApp code: 123-456")
        assertCode("123456", "Instagram code 123 456. Don't share it.")
    }

    @Test
    fun `prefers the number nearest the keyword`() {
        assertCode(
            "663201",
            "Order 88412 update: to confirm delivery, your verification code is 663201"
        )
    }

    @Test
    fun `ignores messages that are not OTPs`() {
        assertIgnored("Hey, are we still on for 7? I'll be at 1234 Main St")
        assertIgnored("Your order 55123 has shipped and arrives Tuesday")
        assertIgnored("Running late, see you soon")
    }

    @Test
    fun `ignores marketing codes`() {
        assertIgnored("Use promo code 2024 for 20% off your next order")
        assertIgnored("Your coupon code 8812 is ready to redeem")
    }

    @Test
    fun `ignores numbers that are amounts, dates, times or phone numbers`() {
        assertIgnored("Your code for the event: payment of $1250 received")
        assertIgnored("Code review meeting moved to 2026-09-22 at 10:30")
        assertIgnored("Call 1-800-555-1234 for your verification")
        assertIgnored("Verification needed: call +1 555 123 4567")
        assertIgnored("Your code balance is 12.5000 credits")
    }

    @Test
    fun `ignores keywords with no number near them`() {
        assertIgnored("Your verification is complete. " + "x".repeat(150) + " Ref 998877")
    }
}
