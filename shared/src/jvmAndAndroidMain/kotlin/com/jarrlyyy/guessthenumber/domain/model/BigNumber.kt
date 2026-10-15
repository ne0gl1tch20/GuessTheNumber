package com.jarrlyyy.guessthenumber.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale

@Serializable(with = BigNumberSerializer::class)
data class BigNumber(val value: BigDecimal) : Comparable<BigNumber> {

    constructor(str: String) : this(parseString(str))
    constructor(d: Double) : this(BigDecimal.valueOf(d))
    constructor(l: Long) : this(BigDecimal.valueOf(l))
    constructor(i: Int) : this(BigDecimal.valueOf(i.toLong()))

    operator fun plus(other: BigNumber): BigNumber = BigNumber(this.value.add(other.value, MathContext.DECIMAL128))
    operator fun minus(other: BigNumber): BigNumber {
        val res = this.value.subtract(other.value, MathContext.DECIMAL128)
        return BigNumber(if (res.signum() < 0) BigDecimal.ZERO else res)
    }
    operator fun times(other: BigNumber): BigNumber = BigNumber(this.value.multiply(other.value, MathContext.DECIMAL128))
    operator fun div(other: BigNumber): BigNumber {
        if (other.value.compareTo(BigDecimal.ZERO) == 0) return ZERO
        return BigNumber(this.value.divide(other.value, 16, RoundingMode.HALF_UP))
    }

    fun pow(n: Int): BigNumber = BigNumber(this.value.pow(n, MathContext.DECIMAL128))
    fun pow(n: Double): BigNumber {
        val ln = kotlin.math.ln(this.value.toDouble()) * n
        val res = kotlin.math.exp(ln)
        return BigNumber(res)
    }

    override fun compareTo(other: BigNumber): Int = this.value.compareTo(other.value)

    fun max(other: BigNumber): BigNumber = if (this >= other) this else other
    fun min(other: BigNumber): BigNumber = if (this <= other) this else other
    fun floor(): BigNumber = BigNumber(this.value.setScale(0, RoundingMode.FLOOR))

    fun format(notation: String = "Standard"): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
        if (value.compareTo(BigDecimal.ONE) < 0) {
            return value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        }
        val exponent = value.precision() - value.scale() - 1
        
        when (notation) {
            "Scientific" -> {
                return String.format(Locale.US, "%.2e", value)
            }
            "Engineering" -> {
                val engExp = (exponent / 3) * 3
                val divisor = BigDecimal.TEN.pow(engExp)
                val scaled = value.divide(divisor, 2, RoundingMode.HALF_UP)
                return "${scaled.stripTrailingZeros().toPlainString()}e$engExp"
            }
            else -> {
                if (exponent < 3) {
                    return value.setScale(0, RoundingMode.HALF_UP).toPlainString()
                }
                val suffixIndex = exponent / 3
                val remainder = exponent % 3
                val divisor = BigDecimal.TEN.pow(exponent - remainder)
                val scaled = value.divide(divisor, 2, RoundingMode.HALF_UP)

                val suffix = getSuffix(suffixIndex)
                return if (suffix != null) {
                    "${scaled.stripTrailingZeros().toPlainString()}$suffix"
                } else {
                    String.format(Locale.US, "%.2e", value)
                }
            }
        }
    }

    private fun getSuffix(index: Int): String? {
        val suffixes = listOf(
            "", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No",
            "Dc", "Ud", "Dd", "Td", "Qad", "Qid", "Sxd", "Spd", "Ocd", "Nod",
            "Vg", "UVg", "DVg", "TVg", "QaVg", "QiVg", "SxVg", "SpVg", "OcVg", "NoVg", "Tg"
        )
        return if (index in suffixes.indices) suffixes[index] else null
    }

    override fun toString(): String = format()

    companion object {
        val ZERO = BigNumber(BigDecimal.ZERO)
        val ONE = BigNumber(BigDecimal.ONE)
        val TEN = BigNumber(BigDecimal.TEN)
        val HUNDRED = BigNumber(BigDecimal(100))

        private fun parseString(s: String): BigDecimal {
            val clean = s.trim().uppercase()
            if (clean.isEmpty() || clean == "0") return BigDecimal.ZERO
            try {
                return BigDecimal(clean)
            } catch (_: Exception) {
                val map = mapOf(
                    "K" to 3, "M" to 6, "B" to 9, "T" to 12, "QA" to 15, "QI" to 18,
                    "SX" to 21, "SP" to 24, "OC" to 27, "NO" to 30, "DC" to 33,
                    "UD" to 36, "DD" to 39, "TD" to 42, "QAD" to 45, "QID" to 48,
                    "SXD" to 51, "SPD" to 54, "OCD" to 57, "NOD" to 60, "VG" to 63,
                    "UVG" to 66, "DVG" to 69, "TVG" to 72, "QAVG" to 75, "QIVG" to 78,
                    "SXVG" to 81, "SPVG" to 84, "OCVG" to 87, "NOVG" to 90, "TG" to 93,
                    "VGN" to 63
                )
                for ((suffix, exp) in map) {
                    if (clean.endsWith(suffix)) {
                        val numPart = clean.substring(0, clean.length - suffix.length).trim()
                        val num = BigDecimal(numPart)
                        return num.multiply(BigDecimal.TEN.pow(exp))
                    }
                }
                return BigDecimal.ZERO
            }
        }
    }
}

class BigNumberSerializer : kotlinx.serialization.KSerializer<BigNumber> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigNumber", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: BigNumber) {
        encoder.encodeString(value.value.toPlainString())
    }
    override fun deserialize(decoder: Decoder): BigNumber {
        return BigNumber(decoder.decodeString())
    }
}
