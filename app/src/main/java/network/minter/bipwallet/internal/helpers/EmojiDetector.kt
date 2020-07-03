/*
 * Copyright (C) by MinterTeam. 2020
 * @link <a href="https://github.com/MinterTeam">Org Github</a>
 * @link <a href="https://github.com/edwardstock">Maintainer Github</a>
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package network.minter.bipwallet.internal.helpers

/**
 * minter-android-wallet. 2020
 * @author Eduard Maximovich (edward.vstock@gmail.com)
 */

object EmojiDetector {
//    private val r = Pattern.compile()

    fun emojiLength(string: String): Int {
        if (string.isEmpty()) {
            return -1
        }
        //val pattern = Pattern.compile("""(\u00a9|\u00ae|[\u2000-\u3300]|\ud83c[\ud000-\udfff]|\ud83d[\ud000-\udfff]|\ud83e[\ud000-\udfff])""", Pattern.UNICODE_CHARACTER_CLASS)
        val data = string.toCharArray()
        if (data.size >= 2) {
            val a = data[0].toInt()
            val b = data[1].toInt()

            @Suppress("RedundantIf")
            return if (a == 0x00a9) {
                1
            } else if (a == 0x00ae) {
                1
            } else if (a in 0x2000..0x3300) {
                1
            } else if (a == 0xd83c && b in 0xd000..0xdfff) {
                2
            } else if (a == 0xd83d && b in 0xd000..0xdfff) {
                2
            } else if (a == 0xd83e && b in 0xd000..0xdfff) {
                2
            } else {
                -1
            }
        }

        return -1
    }
//
//    fun test(string: String, isEmoji: Boolean) {
//        if (isEmojiOnly(string) !== isEmoji) {
//            println(string + " should" + (if (isEmoji) "" else " not") + " be emoji only")
//        }
//    }

    /*
    @JvmStatic
    fun main(args: Array<String>) {
        test("Hello 😲", false)
        test("-", false)
        test("+", false)
        test("$", false)
        test("👴🏻", true) // Requires Unicode 8.0 support for skin tones
        test(" 👨‍👨‍👧‍👧 ", true)
        test(" 0️⃣ ❤️", true)
        test("👨‍👨‍👧‍👧 ❤️", true)
        test(" 0️⃣ ❤️👨‍👨‍👧‍👧 ", true)
        test("0️⃣\n❤️", true)
        test("©®🔝🔛🔙💱➕➖➗®©™🔜🔴🔵⚫️⚪️🔹🔘🔸▪️▫️◻️◾️🔉🔇♦️♥️♣️♠️🀄️🎴👁‍🗨💭🗯🕔🕓🕖🕖🕜🕥💬🕎☪☪🔝☑️#️⃣#️⃣⤵️*️⃣ℹ️↩️↪️↙️↘️➡️🔂◀️🔼⏸⏯4️⃣🆓🆖📶🆒🛄🏧🌀💠✅〽️💯⁉️❕🚳🚯🚷🚫❌⭕️💢⛔️🅱㊙️✴️🆚💘💕💚🔎📝🔓🚩🎐🎀⛱🛏🗿🔑🏷🔬🏺🛡⚔🔫🛠🛠💎💴🕯📡⏲📻📞📸💾🖱📲🕍🏯🎇🏙🏜🏞🎢🚥🚦💺🚤🚇🚅🚟🚍🚜🚑🚙🎷🎺🚴🏾🎱🏉🏀🍶🍵🍿🎂🍡🍛🌭🍠🍍🍇⛄️☃🌨☀️☄⭐️🌝🌚🌚🌝🌑🌒🌕🌺🐚🕸🍃🌴🐑🐄🐄🐌🐦🐗🐽🐭💍🕶👓👠👞👨‍👩‍👧‍👦👩‍❤️‍👩🙎🏼💂🏽👥👇🏻👏🏻😿🤕😲😪🙁😟🙄😘😁🐑🌴👓👏🏻😲😘", true)
        test("㏎", false)
        test("㏷", false)
        test("䷑", false)
        test("䷫", false)
        test("䷽", false)
        test("꒞", false)
    }
     */
}