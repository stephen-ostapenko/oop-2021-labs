@file:Suppress("NonAsciiCharacters")

package csc.markobot.dsl

import csc.markobot.api.*

@MakroBotDsl
class PlastikBuilder {
    var толщина: Int = 0
    fun build(x: Int): Plastik {
        толщина = x
        return Plastik(толщина)
    }
}

val пластик = PlastikBuilder()

@MakroBotDsl
class MetalBuilder {
    var толщина: Int = 0
    fun build(x: Int): Metal {
        толщина = x
        return Metal(толщина)
    }
}

val металл = MetalBuilder()

val `очень легкая` = LoadClass.VeryLight
val легкая = LoadClass.Light
val средняя = LoadClass.Medium
val тяжёлая = LoadClass.Heavy
val `очень тяжёлая` = LoadClass.VeryHeavy
val огромная = LoadClass.Enormous

@MakroBotDsl
class MakroBotBuilder {
    lateinit var head: Head
    lateinit var body: Body
    lateinit var hands: Hands
    lateinit var chassis: Chassis

    fun build(name: String) = MakroBot(name, head, body, hands, chassis)

    @MakroBotDsl
    class HeadBuilder {
        lateinit var material: Material
        lateinit var eyes: List<Eye>
        lateinit var mouth: Mouth

        fun build(): Head = Head(material, eyes, mouth)

        infix fun PlastikBuilder.толщиной(x: Int) {
            this@HeadBuilder.material = PlastikBuilder().build(x)
        }

        infix fun MetalBuilder.толщиной(x: Int) {
            this@HeadBuilder.material = MetalBuilder().build(x)
        }

        @MakroBotDsl
        class EyeBuilder {

            lateinit var LampEyes: List<LampEye>
            lateinit var LedEyes: List<LedEye>

            fun build(): List<Eye> = LampEyes + LedEyes

            @MakroBotDsl
            class LampEyeBuilder {
                var количество: Int = 0
                var яркость: Int = 0

                fun build(): List<LampEye> {
                    val lamps = mutableListOf<LampEye>()
                    for (i in 1..количество) {
                        lamps.add(LampEye(яркость))
                    }
                    return lamps
                }
            }

            fun лампы(settings: LampEyeBuilder.() -> Unit) {
                this@EyeBuilder.LampEyes = LampEyeBuilder().apply(settings).build()
            }

            @MakroBotDsl
            class LedEyeBuilder {
                var количество: Int = 0
                var яркость: Int = 0

                fun build(): List<LedEye> {
                    val leds = mutableListOf<LedEye>()
                    for (i in 1..количество) {
                        leds.add(LedEye(яркость))
                    }
                    return leds
                }
            }

            fun диоды(settings: LedEyeBuilder.() -> Unit) {
                this@EyeBuilder.LedEyes = LedEyeBuilder().apply(settings).build()
            }
        }

        fun глаза(settings: EyeBuilder.() -> Unit) {
            this@HeadBuilder.eyes = EyeBuilder().apply(settings).build()
        }

        @MakroBotDsl
        class MouthBuilder {
            var speaker: Speaker? = null

            fun build() = Mouth(speaker)

            @MakroBotDsl
            class SpeakerBuilder {
                var мощность: Int = 0

                fun build() = Speaker(мощность)
            }

            fun динамик(settings: SpeakerBuilder.() -> Unit) {
                this@MouthBuilder.speaker = SpeakerBuilder().apply(settings).build()
            }
        }

        fun рот(settings: MouthBuilder.() -> Unit) {
            this@HeadBuilder.mouth = MouthBuilder().apply(settings).build()
        }
    }

    fun голова(settings: HeadBuilder.() -> Unit) {
        this@MakroBotBuilder.head = HeadBuilder().apply(settings).build()
    }

    @MakroBotDsl
    class BodyBuilder {
        lateinit var material: Material
        lateinit var strings: List<String>

        fun build(): Body = Body(material, strings)

        infix fun PlastikBuilder.толщиной(x: Int) {
            this@BodyBuilder.material = PlastikBuilder().build(x)
        }

        infix fun MetalBuilder.толщиной(x: Int) {
            this@BodyBuilder.material = MetalBuilder().build(x)
        }

        @MakroBotDsl
        class StringsBuilder {
            private val strings = arrayListOf<String>()

            fun build() = strings

            operator fun String.unaryPlus() {
                strings.add(this)
            }
        }

        fun надпись(settings: StringsBuilder.() -> Unit) {
            this@BodyBuilder.strings = StringsBuilder().apply(settings).build()
        }
    }

    fun туловище(settings: BodyBuilder.() -> Unit) {
        this@MakroBotBuilder.body = BodyBuilder().apply(settings).build()
    }

    @MakroBotDsl
    class HandsBuilder {
        lateinit var material: Material
        lateinit var нагрузка: Pair<LoadClass, LoadClass>

        fun build(): Hands = Hands(material, нагрузка.first, нагрузка.second)

        infix fun PlastikBuilder.толщиной(x: Int) {
            this@HandsBuilder.material = PlastikBuilder().build(x)
        }

        infix fun MetalBuilder.толщиной(x: Int) {
            this@HandsBuilder.material = MetalBuilder().build(x)
        }

        operator fun LoadClass.minus(b: LoadClass): Pair<LoadClass, LoadClass> {
            return (this to b)
        }
    }

    fun руки(settings: HandsBuilder.() -> Unit) {
        this@MakroBotBuilder.hands = HandsBuilder().apply(settings).build()
    }

    @MakroBotDsl
    class ChassisBuilder {
        enum class ChassisType {
            Caterpillar, Legs, Wheel
        }

        lateinit var chassisType: ChassisType

        @MakroBotDsl
        class CaterpillarBuilder() {
            var ширина: Int = 0

            infix fun шириной(x: Int) {
                ширина = x
            }

            fun build(): Chassis.Caterpillar = Chassis.Caterpillar(ширина)
        }

        fun гусеницы(settings: CaterpillarBuilder.() -> Unit) {
            this@ChassisBuilder.chassisType = ChassisType.Caterpillar
        }

        @MakroBotDsl
        class LegsBuilder() {
            fun build(): Chassis.Legs = Chassis.Legs
        }

        fun ноги(settings: LegsBuilder.() -> Unit) {
            this@ChassisBuilder.chassisType = ChassisType.Legs
        }

        @MakroBotDsl
        class WheelBuilder() {
            var диаметр: Int = 0
            var количество: Int = 0

            fun build(): Chassis.Wheel = Chassis.Wheel(количество, диаметр)
        }

        fun колеса(settings: LegsBuilder.() -> Unit) {
            this@ChassisBuilder.chassisType = ChassisType.Wheel
        }

        fun build(): Chassis {
            return when (chassisType) {
                ChassisType.Caterpillar -> Chassis.Caterpillar(CaterpillarBuilder().ширина)
                ChassisType.Legs -> Chassis.Legs
                ChassisType.Wheel -> Chassis.Wheel(WheelBuilder().количество, WheelBuilder().диаметр)
            }
        }
    }

    fun шасси(settings: ChassisBuilder.() -> Unit) {
        this@MakroBotBuilder.chassis = ChassisBuilder().apply(settings).build()
    }
}

fun робот(name: String, settings: MakroBotBuilder.() -> Unit) = MakroBotBuilder().apply(settings).build(name)