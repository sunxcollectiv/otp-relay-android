package com.collectivworks.otprelay.relay

/**
 * Decides whether an SMS carries a one-time code and, if so, pulls it out.
 *
 * A message has to pass two gates. It must mention something code-like
 * ("verification", "OTP", "passcode", plain "code"...) and must not look like
 * marketing ("promo code", "coupon"), and it must contain a standalone run of
 * 4–8 digits that is not part of an amount, date, time or phone number.
 * Anything else is ignored and never leaves the phone.
 *
 * When several numbers qualify, the one closest to a keyword wins, which is
 * what separates "Your code is 482913" from the order number in the same text.
 *
 * Alphanumeric codes ("AB12CD") are deliberately out of scope: without a
 * digit-only shape the false-positive rate on ordinary words is too high.
 */
object OtpExtractor {

    private val KEYWORD = Regex(
        """\b(?:otp|one[\s-]?time|pass\s?code|verif\w*|authenticat\w*|2fa|mfa|pin|code)\b""",
        RegexOption.IGNORE_CASE
    )

    private val MARKETING = Regex(
        """\b(?:promo\w*|coupon|discount|voucher|referral|redeem|gift\s?card|% off)\b""",
        RegexOption.IGNORE_CASE
    )

    /**
     * A 4–8 digit run, or two groups of three ("123-456", "123 456", as
     * WhatsApp and Instagram send them). Google's "G-123456" is accepted with
     * the prefix dropped, since sites only want the digits.
     *
     * The lookarounds reject numbers glued to anything that makes them
     * something else: currency ($1234), decimals and thousands (12.50, 1,234),
     * percentages, dates and times (2026-09-22, 10:30), and the tail of a
     * phone number whose groups are space- or dash-separated.
     */
    private val CANDIDATE = Regex(
        """(?<![\w$€£¥#.,/:+-])(?<!\d )(?:G-)?(\d{3}[ -]\d{3}|\d{4,8})(?![\w%]|[.,:/ -]\d)"""
    )

    /** Keywords further than this from any candidate don't count as describing it. */
    private const val MAX_KEYWORD_DISTANCE = 100

    /** The code in [body], digits only, or null when [body] is not an OTP message. */
    fun extract(body: String): String? {
        if (MARKETING.containsMatchIn(body)) return null
        val keywords = KEYWORD.findAll(body).map { it.range }.toList()
        if (keywords.isEmpty()) return null

        return CANDIDATE.findAll(body)
            .map { match -> match to keywords.minOf { distance(match.range, it) } }
            .filter { (_, distance) -> distance <= MAX_KEYWORD_DISTANCE }
            .minByOrNull { (_, distance) -> distance }
            ?.first
            ?.groupValues?.get(1)
            ?.filter(Char::isDigit)
    }

    /** Characters between two non-overlapping ranges, whichever comes first. */
    private fun distance(a: IntRange, b: IntRange): Int =
        if (a.first > b.last) a.first - b.last else b.first - a.last
}
